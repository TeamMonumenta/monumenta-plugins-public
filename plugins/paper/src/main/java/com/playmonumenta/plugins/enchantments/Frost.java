package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.PotionUtils;

public class Frost implements BaseEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Frost";
	private static final int FROST_DURATION = 20 * 4;
	private static final String LEVEL_METAKEY = "FrostLevelMetakey";
	private static final EnumSet<EntityType> ALLOWED_PROJECTILES = EnumSet.of(EntityType.ARROW, EntityType.SPECTRAL_ARROW);

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public boolean useEnchantLevels() {
		return false;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND, ItemSlot.OFFHAND);
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, int level, Projectile proj, ProjectileLaunchEvent event) {
		if (ALLOWED_PROJECTILES.contains(proj.getType())) {
			int mainHandLevel = this.getLevelFromItem(player.getInventory().getItemInMainHand());
			int offHandLevel = this.getLevelFromItem(player.getInventory().getItemInOffHand());

			if (mainHandLevel > 0 && offHandLevel > 0
				&& player.getInventory().getItemInMainHand().getType().equals(Material.BOW)
				&& player.getInventory().getItemInOffHand().getType().equals(Material.BOW)) {
				/* If we're trying to cheat by dual-wielding this enchant, subtract the lower of the two levels */
				level -= mainHandLevel < offHandLevel ? mainHandLevel : offHandLevel;
			}

			/*
			 * TODO: Check that player doesn't have two bows with this enchant in main and offhand
			 * If they do, subtract from level the level of the lower of the two bows
			 */
			proj.setMetadata(LEVEL_METAKEY, new FixedMetadataValue(plugin, level));
		}
	}

	/*
	 * TODO: This needs some kind of better registration than expecting it to be called directly
	 *
	 * IF YOU COPY THIS YOU MUST PUT A CORRESPONDING CALL IN EntityDamageByEntityEvent !
	 *
	 * This works this way because you might have the enchantment when you fire the arrow, but switch to a different item before it hits
	 */
	public static void onShootAttack(Plugin plugin, Projectile proj, LivingEntity target, EntityDamageByEntityEvent event) {
		if (proj.hasMetadata(LEVEL_METAKEY)) {
			// Level isn't actually used currently

			PotionUtils.applyPotion(event.getDamager(), target, new PotionEffect(PotionEffectType.SLOW, FROST_DURATION, 1, true, true));
			target.getWorld().spawnParticle(Particle.SNOWBALL, target.getLocation().add(0, 1.15, 0), 10, 0.2, 0.35, 0.2, 0.05);

			if (target instanceof Blaze) {
				event.setDamage(event.getDamage() + 1.0);
			}
		}
	}
}
