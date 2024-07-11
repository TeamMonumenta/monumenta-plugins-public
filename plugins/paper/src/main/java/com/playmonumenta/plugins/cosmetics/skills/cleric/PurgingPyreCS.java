package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLightning;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.AbstractMap;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class PurgingPyreCS extends CleansingRainCS {

	public static final String NAME = "Purging Pyre";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"The fires of the Scourge are not of pure evil.",
			"If one were to control them, they could cleanse",
			"even the most cursed souls."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.SOUL_CAMPFIRE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	private static final Particle.DustOptions CYAN = new Particle.DustOptions(Color.fromRGB(0, 200, 200), 1.0f);
	private int mDegree = 0;

	@Override
	public void rainCast(Player player, double mRadius) {
		mDegree = 0;
		World world = player.getWorld();
		Location loc = player.getLocation().subtract(0, LocationUtils.distanceToGround(player.getLocation(), 0, PlayerUtils.getJumpHeight(player)), 0);
		world.playSound(loc, Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 1.2f, 1.9f);
		world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 1.3f, 1.9f);
		world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.1f, 1.7f);
		world.playSound(loc, Sound.AMBIENT_SOUL_SAND_VALLEY_MOOD, SoundCategory.PLAYERS, 0.9f, 2.0f);
		new PPLightning(Particle.REDSTONE, loc).count(30).init(16, 1.5, 1).duration(15).data(CYAN).spawnAsPlayerActive(player);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			world.playSound(loc, Sound.ITEM_BONE_MEAL_USE, SoundCategory.PLAYERS, 1.8f, 0.2f);
			world.playSound(loc, Sound.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.6f, 0.8f);
			world.playSound(loc, Sound.ENTITY_BLAZE_BURN, SoundCategory.PLAYERS, 1.5f, 0.8f);
			world.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1.8f, 0.2f);
			world.playSound(loc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 0.3f, 1.2f);
			new PartialParticle(Particle.FLASH, loc, 1).spawnAsPlayerActive(player);
			ParticleUtils.explodingRingEffect(Plugin.getInstance(), loc, mRadius, 0, 8,
				List.of(
					new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(1.0, (Location location) -> new PartialParticle(Particle.BLOCK_CRACK, location, 1, 0, 12, 0, 0.1)
						.data(Material.CYAN_CONCRETE_POWDER.createBlockData()).directionalMode(true).spawnAsPlayerActive(player))
				)
			);
			loc.setPitch(0);
			ParticleUtils.drawParticleCircleExplosion(player, loc, 0, 0.5, 0, 0,
				25, 0.33f, false, 0, 0.15, Particle.SCULK_SOUL);
		}, 15);
		// Hieroglyph for "Pure"
		Vector front = player.getEyeLocation().getDirection().setY(0).normalize().multiply(mRadius * 0.6);
		Vector left90 = VectorUtils.rotateTargetDirection(front, -90, 0);
		Vector right90 = VectorUtils.rotateTargetDirection(front, 90, 0);
		Location loc1 = loc.clone().add(left90);
		Location loc2 = loc.clone().add(right90);
		for (int i = 0; i < 2; i++) {
			double delta = 0.2*i;
			final Particle.DustOptions ORANGE = new Particle.DustOptions(Color.fromRGB(255 - 60 * i, 135 - 35 * i, 0), 1.2f - i * 0.2f);
			new PPLine(Particle.REDSTONE, loc1.clone().subtract(front), loc1.clone().add(front)).data(ORANGE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE, loc.clone().subtract(front), loc2.clone().subtract(front)).data(ORANGE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE, loc2.clone().subtract(front), loc2).data(ORANGE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE, loc, loc.clone().add(front)).data(ORANGE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE, loc, loc.clone().add(front.clone().multiply(0.5)).add(right90.clone().multiply(0.5))).data(ORANGE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE, loc.clone().add(front), loc.clone().add(front.clone().multiply(0.5)).add(right90.clone().multiply(0.5))).data(ORANGE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
			new PPCircle(Particle.REDSTONE, loc2.clone().add(front.clone().multiply(0.5)), mRadius*0.3).data(ORANGE).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
		}
		new PPCircle(Particle.ENCHANTMENT_TABLE, loc, mRadius).countPerMeter(12).extraRange(0.1, 0.2).innerRadiusFactor(1)
			.directionalMode(true).delta(1, 0.2, 4).rotateDelta(true).spawnAsPlayerActive(player);
	}

	@Override
	public void rainCloud(Player player, double ratio, double mRadius) {
		Location loc = player.getLocation().subtract(0, LocationUtils.distanceToGround(player.getLocation(), 0, PlayerUtils.getJumpHeight(player)), 0);
		if (mDegree >= 135) {
			for (int spiral = 0; spiral < 3; spiral++) {
				double degree = mDegree + spiral * 360.0 / 3;
				Location loc1 = loc.clone().add(mRadius * FastUtils.cosDeg(degree), 0.2, mRadius * FastUtils.sinDeg(degree));
				Vector v = new Vector(FastUtils.cosDeg(degree), 1.0, FastUtils.sinDeg(degree));
				new PartialParticle(Particle.SOUL_FIRE_FLAME, loc1, 1, v.getX(), v.getY(), v.getZ(), 0.06, null, true)
					.spawnAsPlayerActive(player);
			}
		}
		mDegree += 9;
	}

	@Override
	public void rainEnhancement(Player player, double smallRatio, double mRadius) {
		Location loc = player.getLocation().subtract(0, LocationUtils.distanceToGround(player.getLocation(), 0, PlayerUtils.getJumpHeight(player)), 0);
		if (mDegree >= 135) {
			for (int spiral = 0; spiral < 3; spiral++) {
				double degree = mDegree + spiral * 360.0 / 3;
				Location loc1 = loc.clone().add(mRadius * FastUtils.cosDeg(degree), 0.2, mRadius * FastUtils.sinDeg(degree));
				Vector v = new Vector(FastUtils.cosDeg(degree), 1.0, FastUtils.sinDeg(degree));
				new PartialParticle(Particle.SOUL_FIRE_FLAME, loc1, 1, v.getX(), v.getY(), v.getZ(), 0.06, null, true)
					.spawnAsPlayerActive(player);
			}
		}
	}
}
