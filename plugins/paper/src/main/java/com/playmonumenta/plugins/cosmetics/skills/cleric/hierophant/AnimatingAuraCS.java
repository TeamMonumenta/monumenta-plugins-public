package com.playmonumenta.plugins.cosmetics.skills.cleric.hierophant;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
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

public class AnimatingAuraCS extends ThuribleProcessionCS {

	public static final String NAME = "Animating Aura";

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Small, dancing spirits encircle you,",
			"channeling life into your surroundings."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.CANDLE;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	private int mDegree = 0;

	@Override
	public void endBuildupEffect(Player player) {
		Location loc = player.getLocation().subtract(0, LocationUtils.distanceToGround(player.getLocation(), 0, PlayerUtils.getJumpHeight(player)), 0);
		World world = player.getWorld();
		world.playSound(loc, Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 1.35f, 1.1f);
		world.playSound(loc, Sound.ENTITY_ZOMBIE_CONVERTED_TO_DROWNED, SoundCategory.PLAYERS, 1.5f, 0.8f);
		world.playSound(loc, Sound.ENTITY_IRON_GOLEM_DAMAGE, SoundCategory.PLAYERS, 1.6f, 0.8f);
		world.playSound(loc, Sound.BLOCK_CONDUIT_DEACTIVATE, SoundCategory.PLAYERS, 0.85f, 0.9f);
		new PartialParticle(Particle.SPELL_INSTANT, loc, 75, 1, 1, 1, 0.5).spawnAsPlayerActive(player);
		// Hieroglyph for "Blessing"
		Vector front = loc.getDirection().clone().setY(0).normalize().multiply(4);
		Vector left = VectorUtils.rotateTargetDirection(front, -90, 0);
		Vector right = VectorUtils.rotateTargetDirection(front, 90, 0);
		Location loc1 = loc.clone().add(left);
		Location loc2 = loc.clone().add(right);
		for (int i = 0; i < 2; i++) {
			double delta = 0.2 * i;
			final Particle.DustOptions LIME = new Particle.DustOptions(Color.fromRGB(50 - 10 * i, 200 - 40 * i, 50 - 10 * i), 1.2f - i * 0.2f);
			new PPLine(Particle.REDSTONE, loc, loc.clone().subtract(front)).data(LIME).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE, loc1, loc.clone().subtract(front.clone().multiply(0.5))).data(LIME).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE, loc2, loc.clone().subtract(front.clone().multiply(0.5))).data(LIME).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE, loc.clone().subtract(front), loc1.clone().subtract(front.clone().multiply(0.5))).data(LIME).countPerMeter(10).scaleLength(0.8).delta(delta, 0, delta).spawnAsPlayerActive(player);
			new PPLine(Particle.REDSTONE, loc.clone().subtract(front), loc2.clone().subtract(front.clone().multiply(0.5))).data(LIME).countPerMeter(10).scaleLength(0.8).delta(delta, 0, delta).spawnAsPlayerActive(player);
			new PPCircle(Particle.REDSTONE, loc.clone().add(front.clone().multiply(0.5)), 2).data(LIME).countPerMeter(10).delta(delta, 0, delta).spawnAsPlayerActive(player);
		}
		new PPCircle(Particle.ENCHANTMENT_TABLE, loc, 4).countPerMeter(12).extraRange(0.1, 0.2).innerRadiusFactor(1)
			.directionalMode(true).delta(2, 1, -8).rotateDelta(true).spawnAsPlayerActive(player);
	}

	@Override
	public void firstBuffs(Player player) {
		new PartialParticle(Particle.SPELL_INSTANT, player.getLocation(), 2, 1, 1.0, 1).spawnAsPlayerPassive(player);
	}

	@Override
	public void secondBuffs(Player player) {
		new PartialParticle(Particle.SPELL_INSTANT, player.getLocation(), 2, 1, 1.0, 1).spawnAsPlayerPassive(player);
		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				Location loc = player.getLocation().add(2*FastUtils.sinDeg(4 * mTicks + mDegree), 0.6 - LocationUtils.distanceToGround(player.getLocation(), 0, PlayerUtils.getJumpHeight(player)) + 0.5 * FastUtils.sinDeg(6 * mTicks + 1.5 * mDegree), 2*FastUtils.cosDeg(4 * mTicks + mDegree));
				new PartialParticle(Particle.GLOW, loc, 1, 0, 1, 0, 0.25).directionalMode(true).spawnAsPlayerPassive(player);
				mTicks++;
				if (mTicks == 4) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
		mDegree += 16 % 360;
	}

	@Override
	public void thirdBuffs(Player player) {
		new PartialParticle(Particle.SPELL_INSTANT, player.getLocation(), 2, 1, 1.0, 1).spawnAsPlayerPassive(player);
		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				for (int i = 0; i < 2; i++) {
					Location loc = player.getLocation().add(2 * FastUtils.sinDeg(4 * mTicks + mDegree + 120 * i), 0.6 - LocationUtils.distanceToGround(player.getLocation(), 0, PlayerUtils.getJumpHeight(player)) + 0.5 * FastUtils.sinDeg(6 * mTicks + 1.5 * mDegree + 120 * i), 2 * FastUtils.cosDeg(4 * mTicks + mDegree + 120 * i));
					new PartialParticle(Particle.GLOW, loc, 1, 0, 1, 0, 0.25).directionalMode(true).spawnAsPlayerPassive(player);
				}
				mTicks++;
				if (mTicks == 4) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
		mDegree += 16 % 360;
	}

	@Override
	public void fourthBuffs(Player player) {
		new PartialParticle(Particle.SPELL_INSTANT, player.getLocation(), 2, 1, 1.0, 1).spawnAsPlayerPassive(player);
		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				for (int i = 0; i < 3; i++) {
					Location loc = player.getLocation().add(2 * FastUtils.sinDeg(4 * mTicks + mDegree + 120 * i), 0.6 - LocationUtils.distanceToGround(player.getLocation(), 0, PlayerUtils.getJumpHeight(player)) + 0.5 * FastUtils.sinDeg(6 * mTicks + 1.5 * mDegree + 120 * i), 2 * FastUtils.cosDeg(4 * mTicks + mDegree + 120 * i));
					new PartialParticle(Particle.GLOW, loc, 1, 0, 1, 0, 0.25).directionalMode(true).spawnAsPlayerPassive(player);
				}
				mTicks++;
				if (mTicks == 4) {
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
		mDegree += 16 % 360;
	}
}
