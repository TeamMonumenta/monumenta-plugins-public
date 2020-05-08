package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.PotionUtils;

public class IceAspect implements BaseEnchantment {
	private static final int ICE_ASPECT_DURATION = 20 * 5;
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Ice Aspect";
	private static final String LEVEL_METAKEY = "IceAspectLevelMetakey";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> validSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public void onAttack(Plugin plugin, Player player, int level, LivingEntity target, EntityDamageByEntityEvent event) {
		PotionUtils.applyPotion(player, target, new PotionEffect(PotionEffectType.SLOW, (int)(ICE_ASPECT_DURATION * player.getCooledAttackStrength(0)), level - 1, false, true));
		player.getWorld().spawnParticle(Particle.SNOWBALL, target.getLocation().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0.001);

		if (target instanceof Blaze) {
			event.setDamage(event.getDamage() + 1.0);
		}
	}

	// Thrown trident damage handling
	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, int level, Projectile proj, ProjectileLaunchEvent event) {
		if (proj instanceof Trident) {
			proj.setMetadata(LEVEL_METAKEY, new FixedMetadataValue(plugin, level));
		}
	}

	//Trident hit effect
	public static void onShootAttack(Plugin plugin, Projectile proj, LivingEntity target, EntityDamageByEntityEvent event) {
		if (proj.hasMetadata(LEVEL_METAKEY) && proj instanceof Trident && proj.getShooter() instanceof Player) {
			int level = proj.getMetadata(LEVEL_METAKEY).get(0).asInt();
			Player player = (Player)proj.getShooter();
			PotionUtils.applyPotion(player, target, new PotionEffect(PotionEffectType.SLOW, (ICE_ASPECT_DURATION), level - 1, false, true));
			player.getWorld().spawnParticle(Particle.SNOWBALL, target.getLocation().add(0, 1, 0), 8, 0.5, 0.5, 0.5, 0.001);
		}
	}
}
