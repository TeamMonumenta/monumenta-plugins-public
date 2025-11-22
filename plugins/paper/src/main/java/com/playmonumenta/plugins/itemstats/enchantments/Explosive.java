package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.listeners.DamageListener;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.util.BoundingBox;

public class Explosive implements Enchantment {
	public static final int RADIUS = 2;
	public static final double DAMAGE_PERCENTAGE_PER_LEVEL = 0.1;
	private static final Particle.DustOptions YELLOW_1_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 20), 1.0f);
	private static final Particle.DustOptions YELLOW_2_COLOR = new Particle.DustOptions(Color.fromRGB(255, 255, 120), 1.0f);
	private static final Particle.DustOptions BLEED_COLOR = new Particle.DustOptions(Color.fromRGB(210, 44, 44), 1.0f);
	private static final String EXPLODED_THIS_TICK_METADATA = "ExplosiveThisTick";

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
		return EnumSet.of(Slot.MAINHAND, Slot.PROJECTILE);
	}

	@Override
	public double getPriorityAmount() {
		return 19;
	}
	// After Hex Eater, should be the last "flat" damage event

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageEvent.DamageType.PROJECTILE
			&& event.getDamager() instanceof Projectile projectile
			&& EntityUtils.isAbilityTriggeringProjectile(projectile, true)
			&& EntityUtils.isHostileMob(enemy)
			&& MetadataUtils.checkOnceThisTick(plugin, enemy, EXPLODED_THIS_TICK_METADATA)
			&& !(projectile.hasMetadata(ElementalArrows.FIRE_ARROW_METAKEY)
			|| projectile.hasMetadata(ElementalArrows.ICE_ARROW_METAKEY)
			|| projectile.hasMetadata(ElementalArrows.THUNDER_ARROW_METAKEY))) {
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
			int earth = (int) itemStatsMap.get(EnchantmentType.EARTH_ASPECT);
			int wind = (int) itemStatsMap.get(EnchantmentType.WIND_ASPECT);
			int punch = (int) itemStatsMap.get(EnchantmentType.PUNCH);

			Location location = LocationUtils.getEntityCenter(enemy);

			// Using bounding box because otherwise it is a bit inconsistent.
			List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(location, RADIUS * 2);
			BoundingBox box = BoundingBox.of(location, RADIUS, RADIUS, RADIUS);
			nearbyMobs.remove(enemy);

			double damage = event.getFlatDamage() * value * DAMAGE_PERCENTAGE_PER_LEVEL;

			for (LivingEntity mob : nearbyMobs) {
				BoundingBox mobBox = mob.getBoundingBox();
				if (box.overlaps(mobBox)) {
					// Deal damage.
					DamageUtils.damage(player, mob, DamageEvent.DamageType.PROJECTILE_ENCH, damage, ClassAbility.EXPLOSIVE, false);
					Punch.applyPunch(plugin, punch, mob, projectile.getVelocity());
				}
			}

			//Visual feedback
			float multiplier = 1f;
			if (AbilityUtils.isVolley(player, projectile)) {
				multiplier = 0.15f;
			} else if (itemStatsMap.get(EnchantmentType.MULTISHOT) == 1) {
				multiplier = 0.4f;
			}
			particles(location, player, multiplier);
			if (ice > 0) {
				player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 0.6f * multiplier, 1.3f);
				new PartialParticle(Particle.SNOW_SHOVEL, location, 25, 1.5, 1.5, 1.5).spawnAsPlayerActive(player);
			}
			if (thunder > 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 0.6f * multiplier, 0.8f);
				new PartialParticle(Particle.REDSTONE, location, 12, 1.5, 1.5, 1.5, YELLOW_1_COLOR).spawnAsPlayerActive(player);
				new PartialParticle(Particle.REDSTONE, location, 12, 1.5, 1.5, 1.5, YELLOW_2_COLOR).spawnAsPlayerActive(player);
			}
			if (decay > 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.3f * multiplier, 0.7f);
				new PartialParticle(Particle.SQUID_INK, location, 25, 1.5, 1.5, 1.5).spawnAsPlayerActive(player);
			}
			if (bleed > 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_SLIME_SQUISH, SoundCategory.PLAYERS, 0.7f * multiplier, 0.7f);
				new PartialParticle(Particle.REDSTONE, location, 25, 1.5, 1.5, 1.5, BLEED_COLOR).spawnAsPlayerActive(player);
			}
			if (wind > 0) {
				player.playSound(player.getLocation(), Sound.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, multiplier, 0.30f);
				new PartialParticle(Particle.CLOUD, location, 25, 1.5, 1.5, 1.5).spawnAsPlayerActive(player);
			}
			if (earth > 0) {
				player.playSound(player.getLocation(), Sound.BLOCK_GRAVEL_BREAK, SoundCategory.PLAYERS, multiplier, 1.0f);
				new PartialParticle(Particle.FALLING_DUST, location, 12, 1.5, 1.5, 1.5, Material.COARSE_DIRT.createBlockData()).spawnAsPlayerActive(player);
				new PartialParticle(Particle.REDSTONE, location, 12, 1.5, 1.5, 1.5, new Particle.DustOptions(Color.fromRGB(120, 148, 82), 0.75f)).spawnAsPlayerActive(player);
			}
			if (fire > 0 || fire + ice + thunder + decay + bleed + wind + earth == 0) {
				player.playSound(player.getLocation(), Sound.BLOCK_LAVA_POP, SoundCategory.PLAYERS, 0.6f * multiplier, 0.9f);
				new PartialParticle(Particle.LAVA, location, 25, 1.5, 1.5, 1.5).spawnAsPlayerActive(player);
			}
		}
	}

	public static void particles(Location loc, Player player, float multiplier) {
		int count = 10;
		double offset = 0.5;
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, count, offset, offset, offset, 0.25).spawnAsPlayerActive(player);
		new PartialParticle(Particle.SOUL_FIRE_FLAME, loc, count, offset, offset, offset, 0.25).spawnAsPlayerActive(player);
		new PartialParticle(Particle.FLAME, loc, count, offset, offset, offset, 0.25).spawnAsPlayerActive(player);
		player.playSound(player.getLocation(), Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, SoundCategory.PLAYERS, 0.8f * multiplier, 1.5f);
	}
}
