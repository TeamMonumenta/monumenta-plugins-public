package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;

public class Spark implements BaseEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Spark";
	private static final String LEVEL_METAKEY = "SparkLevelMetakey";
	private static final EnumSet<EntityType> ALLOWED_PROJECTILES = EnumSet.of(EntityType.ARROW, EntityType.SPECTRAL_ARROW);
	private static final Particle.DustOptions YELLOW_1_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 20), 1.0f);
	private static final Particle.DustOptions YELLOW_2_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 120), 1.0f);
	// Bow velocity comes out at around 2.95 to 3.05
	private static final double ARROW_VELOCITY_SCALE = 3;

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

			double rand = FastUtils.RANDOM.nextDouble();
			double randChance = Math.min(1, proj.getVelocity().length() / ARROW_VELOCITY_SCALE / AttributeProjectileSpeed.getProjectileSpeedModifier(proj));

			if (rand < randChance) {
				proj.setMetadata(LEVEL_METAKEY, new FixedMetadataValue(plugin, level));
			}
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

			if (target instanceof Guardian || target instanceof IronGolem) {
				event.setDamage(event.getDamage() + 1.0);
			}

			//0.5 second stun
			if (!EntityUtils.isBoss(target) && !EntityUtils.isElite(target)) {
				EntityUtils.applyStun(plugin, 10, target);
			} else {
				return;
			}

			World world = proj.getWorld();

			world.spawnParticle(Particle.REDSTONE, target.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, YELLOW_1_COLOR);
			world.spawnParticle(Particle.REDSTONE, target.getLocation().add(0, 1, 0), 12, 0.5, 0.5, 0.5, YELLOW_2_COLOR);
			world.spawnParticle(Particle.FIREWORKS_SPARK, target.getLocation().add(0, 1, 0), 15, 0, 0, 0, 0.15);
			world.playSound(target.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.65f, 1.5f);
		}
	}
}
