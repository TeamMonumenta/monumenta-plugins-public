package com.playmonumenta.plugins.cosmetics.skills.scout.ranger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ButterflyEffectCS extends TacticalManeuverCS {
	public static final String NAME = "Butterfly Effect";
	private static final Particle.DustOptions LIGHT_BLUE_COLOR = new Particle.DustOptions(Color.fromRGB(173, 216, 230), 2.1f);
	private static final Particle.DustOptions SMALL_BLUE_COLOR = new Particle.DustOptions(Color.fromRGB(173, 216, 230), 1.0f);
	private static final Particle.DustOptions BLACK_COLOR = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.0f);
	private static final Map<UUID, Boolean> activeManeuvers = new HashMap<>();

	@Override
	public @Nullable List<String> getDescription() {
		return List.of(
			"Each flap ripples through time."
		);
	}

	@Override
	public Material getDisplayItem() {
		return Material.FEATHER;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public void maneuverStartEffect(World world, Player mPlayer, Vector dir) {
		doEffect(world, mPlayer);
	}

	@Override
	public void maneuverBackEffect(World world, Player mPlayer) {
		doEffect(world, mPlayer);
	}

	public void doEffect(World world, Player mPlayer) {
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.2f, 1f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_WOOL_PLACE, SoundCategory.PLAYERS, 2f, 0.8f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_WOOL_PLACE, SoundCategory.PLAYERS, 2f, 0.8f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_WOOL_PLACE, SoundCategory.PLAYERS, 2f, 0.8f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_BEEHIVE_EXIT, SoundCategory.PLAYERS, 1.5f, 1.9f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 2f, 1f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.4f, 1f);
		if (activeManeuvers.getOrDefault(mPlayer.getUniqueId(), false)) {
			return;
		}
		activeManeuvers.put(mPlayer.getUniqueId(), true);
		new BukkitRunnable() {
			int mAngleOne = 0;
			int mAngleTwo = 180;
			int mTicks = 0;

			@Override
			public void run() {
				if ((mTicks > 5 && PlayerUtils.isOnGroundOrMountIsOnGround(mPlayer)) || mPlayer.isDead() || !mPlayer.isOnline() || !mPlayer.getLocation().isChunkLoaded() || mTicks > 30 * 20) {
					activeManeuvers.put(mPlayer.getUniqueId(), false);
					this.cancel();
				}
				Block block = mPlayer.getLocation().getBlock();
				if (BlockUtils.isWaterlogged(block) || block.getType() == Material.LAVA || BlockUtils.isClimbable(block)) {
					activeManeuvers.put(mPlayer.getUniqueId(), false);
					this.cancel();
				}
				Vector mFront = mPlayer.getEyeLocation().getDirection().setY(0).normalize();
				Location helix = mPlayer.getLocation().clone().add(new Vector(mFront.getX() * -4, 0, mFront.getZ() * -4)).add(0, 1, 0);
				Location flap = mPlayer.getLocation().clone().add(new Vector(mFront.getX() * -2, 0, mFront.getZ() * -2)).add(0, 1, 0);
				Vector right = mFront.clone().crossProduct(new Vector(0, 1, 0)).normalize();
				new PPCircle(Particle.REDSTONE, helix, 1).data(LIGHT_BLUE_COLOR).countPerMeter(2).directionalMode(false).rotateDelta(true).axes(new Vector(0, 1, 0), right.clone()).ringMode(true).arcDegree(mAngleOne, mAngleOne).spawnAsPlayerActive(mPlayer);
				new PPCircle(Particle.REDSTONE, helix, 1).data(LIGHT_BLUE_COLOR).countPerMeter(2).directionalMode(false).rotateDelta(true).axes(new Vector(0, 1, 0), right.clone()).ringMode(true).arcDegree(mAngleTwo, mAngleTwo).spawnAsPlayerActive(mPlayer);
				mAngleOne += 10;
				mAngleTwo += 10;
				if (mAngleOne == 360) {
					mAngleOne = 0;
				}
				if (mAngleTwo == 360) {
					mAngleTwo = 0;
				}
				int mTickCounter = (mTicks % 25);
				switch (mTickCounter) {
					case 0 -> {
						drawWing(mPlayer, mFront, 180);
					}
					case 4, 20 -> {
						drawWing(mPlayer, mFront, 195);
					}
					case 8, 16 -> {
						drawWing(mPlayer, mFront, 225);
					}
					case 12 -> {
						drawWing(mPlayer, mFront, 260);
						world.playSound(mPlayer.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_STEP, SoundCategory.PLAYERS, 1f, 0.75f);
						world.playSound(mPlayer.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 0.5f, 1.5f);
						world.playSound(mPlayer.getLocation(), Sound.BLOCK_WOOL_FALL, SoundCategory.PLAYERS, 1f, 1f);
						for (int x = 0; x <= 19; x++) {
							Location random = flap.clone().add(right.clone().multiply(FastUtils.randomFloatInRange(-1.5f, 1.5f))).add(mFront.clone().multiply(-1).multiply(FastUtils.randomFloatInRange(0.5f, 1.5f)));
							new PartialParticle(Particle.END_ROD, flap, 1, random.getX() - flap.getX(), FastUtils.randomFloatInRange(-1.5f, 1.5f), random.getZ() - flap.getZ(), FastUtils.randomFloatInRange(0.15f, 0.25f))
								.directionalMode(true).spawnAsPlayerActive(mPlayer);
							Location random2 = flap.clone().add(right.clone().multiply(FastUtils.randomFloatInRange(-1.5f, 1.5f))).add(mFront.clone().multiply(-1).multiply(FastUtils.randomFloatInRange(0.5f, 1.5f)));
							new PartialParticle(Particle.WAX_OFF, flap, 1, random2.getX() - flap.getX(), FastUtils.randomFloatInRange(-1.5f, 1.5f), random2.getZ() - flap.getZ(), FastUtils.randomFloatInRange(15f, 25f))
								.directionalMode(true).spawnAsPlayerActive(mPlayer);
						}
					}
					default -> {
					}
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	private void drawWing(Player mPlayer, Vector dir, int angle) {
		double[][] bezierControlPoints = {
			{0.000, 0.940, 1.622, 1.530, 1.548, 2.973, 2.523, 2.413},
			{1.331, 1.404, 1.169, 1.537, 1.507, 2.064, 1.792, 2.408},
			{0.829, 0.665, 0.290, 0.015, 0.382, 0.384, 0.518, 1.233},
			{0.829, 1.146, 1.327, 1.242, 0.382, 0.566, 1.047, 1.243},
			{1.335, 0.746, 0.815, 1.242, 1.51, 1.381, 1.59, 1.243}
		};

		double mScaleFactor = 1.5;
		for (double[] points : bezierControlPoints) {
			for (int i = 0; i < points.length; i++) {
				points[i] *= mScaleFactor;
			}
		}

		Location mCenter = mPlayer.getLocation();
		Vector yawDirection = dir.clone().setY(0).normalize();
		Vector backOffset = yawDirection.clone().multiply(-0.25);
		Location centerLocation = mCenter.add(backOffset).add(0, -1, 0);
		Location leftWingLocation = centerLocation.clone();
		Location rightWingLocation = centerLocation.clone();

		// flip for other wing
		double[][] mFlippedControlPoints = new double[bezierControlPoints.length][];
		for (int i = 0; i < bezierControlPoints.length; i++) {
			mFlippedControlPoints[i] = bezierControlPoints[i].clone();
			mFlippedControlPoints[i][0] = -bezierControlPoints[i][0];
			mFlippedControlPoints[i][1] = -bezierControlPoints[i][1];
			mFlippedControlPoints[i][2] = -bezierControlPoints[i][2];
			mFlippedControlPoints[i][3] = -bezierControlPoints[i][3];
		}

		Vector mRightYawDirection = VectorUtils.rotateYAxis(yawDirection, angle);
		draw2DBezier(rightWingLocation, mRightYawDirection, mFlippedControlPoints, mPlayer);
		Vector mLeftYawDirection = VectorUtils.rotateYAxis(yawDirection, -angle);
		draw2DBezier(leftWingLocation, mLeftYawDirection, bezierControlPoints, mPlayer);
	}

	private void draw2DBezier(Location centerLocation, Vector yawDirection, double[][] bezierControlPoints, Player mPlayer) {
		for (double[] points : bezierControlPoints) {
			ParticleUtils.drawCurve(
				centerLocation,
				0, 100, yawDirection,
				t -> 0.0,
				t -> {
					double tScaled = t / 100.0;
					return Math.pow(1 - tScaled, 3) * points[4]
						+ 3 * Math.pow(1 - tScaled, 2) * tScaled * points[5]
						+ 3 * (1 - tScaled) * Math.pow(tScaled, 2) * points[6]
						+ Math.pow(tScaled, 3) * points[7];
				},
				t -> {
					double tScaled = t / 100.0;
					return Math.pow(1 - tScaled, 3) * points[0]
						+ 3 * Math.pow(1 - tScaled, 2) * tScaled * points[1]
						+ 3 * (1 - tScaled) * Math.pow(tScaled, 2) * points[2]
						+ Math.pow(tScaled, 3) * points[3];
				},
				(l, t) -> {
					if (FastUtils.randomIntInRange(1, 10) <= 7) {
						new PartialParticle(Particle.REDSTONE, l, 1, 0.05, 0, 0.05, 0, BLACK_COLOR).spawnAsPlayerActive(mPlayer);
					}
				}
			);
		}
	}

	@Override
	public void maneuverTickEffect(Player mPlayer) {
	}

	@Override
	public void maneuverHitEffect(World world, Player mPlayer, LivingEntity le) {
		Location mLoc = le.getLocation().add(0, le.getHeight() / 2, 0);
		new PartialParticle(Particle.SPELL_INSTANT, mLoc, 50, 0.65, 0.35, 0.65, 0.3).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.REDSTONE, mLoc, 100, 2.5, 1.5, 2.5, 0, SMALL_BLUE_COLOR).spawnAsPlayerActive(mPlayer);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 0.7f, 1f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_CALCITE_STEP, SoundCategory.PLAYERS, 0.7f, 1f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_CANDLE_EXTINGUISH, SoundCategory.PLAYERS, 0.7f, 0.65f);
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_AZALEA_HIT, SoundCategory.PLAYERS, 0.7f, 1f);
		world.playSound(mPlayer.getLocation(), Sound.ENTITY_TURTLE_EGG_CRACK, SoundCategory.PLAYERS, 0.5f, 0.8f);
		new BukkitRunnable() {
			double mRadius = (le.getWidth() / 2) + 0.8;
			int mTicks = 0;

			@Override
			public void run() {
				mRadius -= 0.2;
				if (mTicks < 6) {
					new PPCircle(Particle.SPELL_INSTANT, mLoc, mRadius).countPerMeter(2).spawnAsPlayerActive(mPlayer);
					new PPCircle(Particle.SPELL_INSTANT, mLoc.clone().add(0, 0.7, 0), mRadius).countPerMeter(2).spawnAsPlayerActive(mPlayer);
					new PPCircle(Particle.SPELL_INSTANT, mLoc.clone().add(0, -0.7, 0), mRadius).countPerMeter(2).spawnAsPlayerActive(mPlayer);
				}
				if (mTicks >= 6) {
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 1, 5);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent event) {
		activeManeuvers.remove(event.getPlayer().getUniqueId());
	}
}
