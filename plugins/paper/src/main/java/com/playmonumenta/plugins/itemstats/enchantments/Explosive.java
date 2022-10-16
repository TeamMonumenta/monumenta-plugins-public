package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.BoundingBox;

public class Explosive implements Enchantment {
	public static final int RADIUS = 4;
	public static final double DAMAGE_PERCENTAGE_PER_LEVEL = 0.1;
	private static final Particle.DustOptions YELLOW_1_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 20), 1.0f);
	private static final Particle.DustOptions YELLOW_2_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 120), 1.0f);
	private static final Particle.DustOptions BLEED_COLOR = new Particle.DustOptions(Color.fromRGB(210, 44, 44), 1.0f);

	public static final String CHARM_DAMAGE = "Explosive Damage";
	public static final String CHARM_RADIUS = "Explosive Radius";

	@Override
	public String getName() {
		return "Explosive";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.EXPLODING;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public double getPriorityAmount() {
		return 5002;
	}

	@Override
	public void onProjectileHit(Plugin plugin, Player player, double level, ProjectileHitEvent event, Projectile projectile) {
		if (projectile instanceof AbstractArrow arrow && !(arrow instanceof Trident) && !arrow.isCritical()) {
			// Ensures only fully charged arrow shots are registered.
			return;
		}

		ItemStatManager.PlayerItemStats playerItemStats = DamageListener.getProjectileItemStats(projectile);
		if (playerItemStats == null) {
			return;
		}

		ItemStatManager.PlayerItemStats.ItemStatsMap itemStatsMap = playerItemStats.getItemStats();

		//Get enchant levels on weapon
		int fire = (int) itemStatsMap.get(EnchantmentType.FIRE_ASPECT);
		int ice = (int) itemStatsMap.get(EnchantmentType.ICE_ASPECT);
		int thunder = (int) itemStatsMap.get(EnchantmentType.THUNDER_ASPECT);
		int decay = (int) itemStatsMap.get(EnchantmentType.DECAY);
		int bleed = (int) itemStatsMap.get(EnchantmentType.BLEEDING);
		//int earth = (int) itemStatsMap.get(EnchantmentType.EARTH_ASPECT);
		//int wind = (int) itemStatsMap.get(EnchantmentType.WIND_ASPECT);

		Location location = EntityUtils.getProjectileHitLocation(event);

		double radius = CharmManager.getRadius(player, CHARM_RADIUS, RADIUS);
		particles(location, player, radius);

		// Using bounding box because otherwise it is a bit inconsistent.
		List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(location, radius);
		BoundingBox box = BoundingBox.of(location, radius / 2, radius / 2, radius / 2);

		Entity hitEntity = event.getHitEntity();
		if (hitEntity instanceof LivingEntity le) {
			nearbyMobs.remove(le);
		}

		double rawDamage = ItemStatUtils.getAttributeAmount(player.getInventory().getItemInMainHand(), ItemStatUtils.AttributeType.PROJECTILE_DAMAGE_ADD, ItemStatUtils.Operation.ADD, ItemStatUtils.Slot.MAINHAND);
		double damage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, level * DAMAGE_PERCENTAGE_PER_LEVEL * rawDamage);
		for (LivingEntity mob : nearbyMobs) {
			if (mob.getBoundingBox().overlaps(box)) {
				DamageUtils.damage(player, mob, new DamageEvent.Metadata(DamageEvent.DamageType.OTHER, ClassAbility.EXPLOSIVE, playerItemStats), damage, true, true, false);
			}
		}

		//Visual feedback
		if (ice > 0) {
			player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.6f, 1.3f);
			new PartialParticle(Particle.SNOW_SHOVEL, location, 25, 1.5, 1.5, 1.5).spawnAsPlayerActive(player);
		}
		if (thunder > 0) {
			player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.6f, 0.8f);
			new PartialParticle(Particle.REDSTONE, location, 12, 1.5, 1.5, 1.5, YELLOW_1_COLOR).spawnAsPlayerActive(player);
			new PartialParticle(Particle.REDSTONE, location, 12, 1.5, 1.5, 1.5, YELLOW_2_COLOR).spawnAsPlayerActive(player);
		}
		if (decay > 0) {
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.4f, 0.7f);
			new PartialParticle(Particle.SQUID_INK, location, 25, 1.5, 1.5, 1.5).spawnAsPlayerActive(player);
		}
		if (bleed > 0) {
			player.playSound(player.getLocation(), Sound.ENTITY_SLIME_SQUISH, 0.7f, 0.7f);
			new PartialParticle(Particle.REDSTONE, location, 25, 1.5, 1.5, 1.5, BLEED_COLOR).spawnAsPlayerActive(player);
		}
		if (fire > 0 || fire + ice + thunder + decay + bleed == 0) {
			player.playSound(player.getLocation(), Sound.BLOCK_LAVA_POP, 0.6f, 0.9f);
			player.getWorld().spawnParticle(Particle.LAVA, location, 25, 1.5, 1.5, 1.5);
		}

		// Remove projectile (mainly to avoid tridents doing a double explosion)
		projectile.remove();
	}

	public static void particles(Location loc, Player player, double radius) {
		World world = loc.getWorld();
		double ratio = radius / RADIUS;
		int count = (int) (10 * ratio * ratio);
		double offset = 0.5 * ratio;
		world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, count, offset, offset, offset, 0.25);
		world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, count, offset, offset, offset, 0.25);
		world.spawnParticle(Particle.FLAME, loc, count, offset, offset, offset, 0.25);
		player.playSound(player.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0f, 1.5f);
	}
}
