package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;

public class Sniper implements BaseEnchantment {
	private static final String PROPERTY_NAME = ChatColor.GRAY + "Sniper";
	public static final int DISTANCE = 16;
	public static final int DAMAGE_PER_LEVEL = 2;
	public static final String LEVEL_METAKEY = "SniperLevelMetakey";
	private static final String LOCATION_METAKEY = "SniperLocationMetakey";
	private static final EnumSet<EntityType> ALLOWED_PROJECTILES = EnumSet.of(EntityType.ARROW, EntityType.SPECTRAL_ARROW);

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, int level, Projectile proj, ProjectileLaunchEvent event) {
		if (ALLOWED_PROJECTILES.contains(proj.getType())) {
			if ((proj instanceof AbstractArrow) && !((AbstractArrow)proj).isCritical()) {
				// If this is an arrow, it must be critical. Since this is not, abort early
				return;
			}

			proj.setMetadata(LEVEL_METAKEY, new FixedMetadataValue(plugin, level));
			proj.setMetadata(LOCATION_METAKEY, new FixedMetadataValue(plugin, player.getLocation()));
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
		if (proj.hasMetadata(LEVEL_METAKEY) && proj.hasMetadata(LOCATION_METAKEY)) {
			int level = proj.getMetadata(LEVEL_METAKEY).get(0).asInt();
			Location loc = (Location)proj.getMetadata(LOCATION_METAKEY).get(0).value();

			if (loc.distance(target.getLocation()) > DISTANCE) {
				event.setDamage(event.getDamage() + level * DAMAGE_PER_LEVEL);
				ProjectileSource shooter = proj.getShooter();
				if (shooter instanceof Player) {
					particles(target.getEyeLocation(), (Player) shooter);
				}
			}
		}
	}

	public static void particles(Location loc, Player player) {
		World world = loc.getWorld();
		world.spawnParticle(Particle.CRIT, loc, 30, 0, 0, 0, 0.65);
		world.spawnParticle(Particle.CRIT_MAGIC, loc, 30, 0, 0, 0, 0.65);
		player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.6f, 0.5f);
	}
}
