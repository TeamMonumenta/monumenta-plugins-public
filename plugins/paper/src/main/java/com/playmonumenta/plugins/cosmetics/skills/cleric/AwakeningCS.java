package com.playmonumenta.plugins.cosmetics.skills.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class AwakeningCS extends CelestialBlessingCS {

	public static final String NAME = "Awakening";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"As the flesh binds the mind, lucidity lies beyond reach.",
			"Transcend the barrier, and awaken your mind."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.END_CRYSTAL;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	private static final Particle.DustOptions LIGHT_CYAN = new Particle.DustOptions(Color.fromRGB(0, 255, 255), 1.2f);
	private static final Particle.DustOptions CYAN = new Particle.DustOptions(Color.fromRGB(0, 180, 180), 1.2f);
	private static final Particle.DustOptions PURPLE = new Particle.DustOptions(Color.fromRGB(100, 20, 200), 1.2f);
	private static final Particle.DustOptions DARK_PURPLE = new Particle.DustOptions(Color.fromRGB(50, 10, 100), 1.2f);

	@Override
	public void tickEffect(Player player, Player target, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		Location loc = target.getLocation().add(0, 1, 0);
		new PartialParticle(Particle.REDSTONE, loc, 2, 0.8, 0.8, 0.8, 0).data(LIGHT_CYAN).spawnAsPlayerBuff(target);
		new PartialParticle(Particle.REDSTONE, loc, 2, 0.8, 0.8, 0.8, 0).data(CYAN).spawnAsPlayerBuff(target);
		new PartialParticle(Particle.ENCHANTMENT_TABLE, loc, 1, 0.8, 0.8, 0.8, 0).spawnAsPlayerBuff(target);
	}

	@Override
	public void loseEffect(Player player, Player target) {
		Location loc = target.getLocation();
		target.playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_DEATH, SoundCategory.PLAYERS, 0.6f, 0.6f);
		target.playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.8f, 0.6f);
		new PartialParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1).spawnAsPlayerBuff(target);
	}

	@Override
	public void startEffectTargets(Player player) {
		Location locPlayer = player.getLocation().subtract(0, LocationUtils.distanceToGround(player.getLocation(), 0, PlayerUtils.getJumpHeight(player)), 0);
		player.playSound(locPlayer, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.3f, 0.7f);
		player.playSound(locPlayer, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.PLAYERS, 1.3f, 0.6f);
		player.playSound(locPlayer, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.PLAYERS, 1.3f, 1.1f);
		// Hieroglyph for "Mind"
		Vector front = locPlayer.getDirection().setY(0).normalize().multiply(3);
		Vector left = VectorUtils.rotateTargetDirection(front, -120, 0);
		Vector right = VectorUtils.rotateTargetDirection(front, 120, 0);
		Location loc1 = locPlayer.clone().add(front);
		Location loc2 = locPlayer.clone().add(left);
		Location loc3 = locPlayer.clone().add(right);
		for (int i = 0; i < 2; i++) {
			double delta = 0.2 * i;
			final Particle.DustOptions CYAN = new Particle.DustOptions(Color.fromRGB(0, 180 - 60 * i, 180 - 60 * i), 1.2f - i * 0.2f);
			new PPLine(Particle.REDSTONE, loc1, loc2).data(CYAN).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE, loc1, loc3).data(CYAN).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE, loc2, loc3).data(CYAN).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE, loc1, locPlayer.clone().subtract(front)).data(CYAN).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
		}
		new PPCircle(Particle.ENCHANTMENT_TABLE, locPlayer, 3).countPerMeter(12).extraRange(0.1, 0.2).innerRadiusFactor(1)
			.directionalMode(true).delta(1, 1, -4).rotateDelta(true).spawnAsPlayerActive(player);
	}

	@Override
	public void startEffectCaster(Player caster, double radius) {
		Location locCaster = caster.getLocation().subtract(0, LocationUtils.distanceToGround(caster.getLocation(), 0, PlayerUtils.getJumpHeight(caster)), 0);
		World world = caster.getWorld();
		world.playSound(locCaster, Sound.ENTITY_WARDEN_ROAR, SoundCategory.PLAYERS, 1.0f, 0.8f);
		world.playSound(locCaster, Sound.ENTITY_WITHER_AMBIENT, SoundCategory.PLAYERS, 0.25f, 0.9f);
		Location arcLoc1 = locCaster.clone().add(0, 1, 0);
		arcLoc1.setPitch(0);
		Location arcLoc2 = locCaster.clone().add(0, 2.5, 0);
		arcLoc2.setPitch(0);
		ParticleUtils.drawHalfArc(arcLoc1, 4 + Math.random(), -10 + 20 * Math.random(), 0, 359, 5, 0.2,
			(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.DUST_COLOR_TRANSITION, l, 2, 0.05, 0.05, 0.05, 0,
				new Particle.DustTransition(
					ParticleUtils.getTransition(LIGHT_CYAN, CYAN, ring / 4D).getColor(),
					ParticleUtils.getTransition(PURPLE, DARK_PURPLE, ring / 4D).getColor(),
					1.2f
				))
				.directionalMode(false).spawnAsPlayerActive(caster));
		ParticleUtils.drawHalfArc(arcLoc2, 4 + Math.random(), 180 + 10 - 20 * Math.random(), 0, 359, 5, 0.2,
			(Location l, int ring, double angleProgress) -> new PartialParticle(Particle.DUST_COLOR_TRANSITION, l, 2, 0.05, 0.05, 0.05, 0,
				new Particle.DustTransition(
					ParticleUtils.getTransition(LIGHT_CYAN, CYAN, ring / 4D).getColor(),
					ParticleUtils.getTransition(PURPLE, DARK_PURPLE, ring / 4D).getColor(),
					1.2f
				))
				.directionalMode(false).spawnAsPlayerActive(caster));
		new BukkitRunnable() {
			int mTicks = 0;
			int mIter = 0;
			double mDegree = 0;

			@Override
			public void run() {
				for (int i = 0; i < 10; i++) {
					for (int spiral = 0; spiral < 3; spiral++) {
						double degree = mDegree + spiral * 360.0 / 3;
						Location l1 = locCaster.clone().add((3 * FastUtils.cosDeg(degree)) / Math.pow(2, mTicks), Math.pow(mIter, 2) / 275, (3 * FastUtils.sinDeg(degree)) / Math.pow(2, mTicks));
						Vector v = new Vector(-FastUtils.sinDeg(degree), 0.5, FastUtils.cosDeg(degree));
						new PartialParticle(Particle.END_ROD, l1, 1, v.getX(), 1, v.getZ(), 0.05, null, true, 0.02)
							.spawnAsPlayerActive(caster);
					}
					mDegree += 10;
					mIter++;
				}
				mTicks++;
				if (mTicks > 5) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
