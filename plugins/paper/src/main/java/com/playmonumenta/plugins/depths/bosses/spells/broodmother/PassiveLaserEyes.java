package com.playmonumenta.plugins.depths.bosses.spells.broodmother;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.bosses.Broodmother;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.function.Function;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class PassiveLaserEyes extends Spell {

	public static final String SPELL_NAME = "Laser Eyes";
	public static final double MAGIC_DAMAGE = 8;
	public static final double FIRE_DAMAGE = 4;
	public static final int MOVEMENT_COOLDOWN = 140;
	public static final int TRIGGER_COOLDOWN = 4;
	public static final int TRIGGERS_PER_MOVEMENT = MOVEMENT_COOLDOWN / TRIGGER_COOLDOWN;
	public static final double MIN_YAW = 30;
	public static final double MAX_YAW = 150;
	public static final double MIN_PITCH = 20;
	public static final double MAX_PITCH = 60;
	public static final int INTERPOLATION_DURATION = 100;
	public static final Function<Integer, Double> INTERPOLATION_FUNCTION = (ticks) -> {
		double progress = (double) ticks / (double) INTERPOLATION_DURATION;
		return -2.0d * Math.pow(progress, 3.0d) + 3.0d * Math.pow(progress, 2.0d);
	};

	private final LivingEntity mBoss;
	private final Broodmother mBroodmother;
	private final ArrayList<Location> mEyes = new ArrayList<>();
	private final Location mAccumulationPoint;
	private final Particle.DustOptions mLaserOptions = new Particle.DustOptions(Color.RED, 1);

	private int mSpellTicks = 0;
	private int mTriggers = 0;

	public PassiveLaserEyes(LivingEntity boss, Broodmother broodmother, @Nullable DepthsParty party) {
		mBoss = boss;
		mBroodmother = broodmother;
		Vector baseDirection = VectorUtils.rotationToVector(90, 25);
		// Eye locations from boss
		mEyes.add(mBoss.getLocation().add(-0.5, 8.5, 1).setDirection(baseDirection));
		mEyes.add(mBoss.getLocation().add(-0.5, 8.5, -1).setDirection(baseDirection));
		mEyes.add(mBoss.getLocation().add(-1.5, 6.5, 3).setDirection(baseDirection));
		mEyes.add(mBoss.getLocation().add(-1.5, 6.5, -3).setDirection(baseDirection));
		mEyes.add(mBoss.getLocation().add(-1.5, 5.5, 1).setDirection(baseDirection));
		mEyes.add(mBoss.getLocation().add(-1.5, 5.5, -1).setDirection(baseDirection));
		if (party != null && party.getAscension() >= 15) {
			mEyes.add(mBoss.getLocation().add(-1.5, 5.5, 2).setDirection(baseDirection));
			mEyes.add(mBoss.getLocation().add(-1.5, 5.5, -2).setDirection(baseDirection));
		}
		mAccumulationPoint = mBoss.getLocation().add(-6, 8, 0);
	}

	@Override
	public void run() {
		// Do not laser during dash
		if (mBroodmother.isCastingDisruptiveSpell()) {
			return;
		}

		if (mSpellTicks < TRIGGER_COOLDOWN) {
			mSpellTicks++;
			return;
		}
		mSpellTicks = 0;
		drawLasersAndHitPlayers();

		mTriggers++;
		if (mTriggers >= TRIGGERS_PER_MOVEMENT) {
			moveLasers();
			mTriggers = 0;
		}
	}

	private void drawLasersAndHitPlayers() {
		for (Location eye : mEyes) {
			RayTraceResult rayTraceResult = eye.getWorld().rayTrace(mAccumulationPoint, eye.getDirection(), 200, FluidCollisionMode.NEVER, true, 0.35,
				e -> e instanceof Player player && !player.getGameMode().equals(GameMode.SPECTATOR)
			);
			if (rayTraceResult == null) {
				continue;
			}

			Entity hitEntity = rayTraceResult.getHitEntity();
			if (hitEntity instanceof Player hitPlayer) {
				laserHitPlayer(hitPlayer);
				drawLaserToLocation(mAccumulationPoint, rayTraceResult.getHitPosition().toLocation(eye.getWorld()));
			} else {
				// No player entity hit. Hit the block instead.
				Block hitBlock = rayTraceResult.getHitBlock();
				BlockFace hitBlockFace = rayTraceResult.getHitBlockFace();
				if (hitBlock != null && hitBlockFace != null) {
					drawLaserToLocation(mAccumulationPoint, hitBlock.getLocation().add(hitBlockFace.getDirection()));
				}
			}
			// Line from eye to accumulation point
			new PPLine(Particle.REDSTONE, eye, mAccumulationPoint).countPerMeter(1.5).data(mLaserOptions)
				.offset(FastUtils.randomDoubleInRange(0, 0.5)).spawnAsBoss();
		}
		// Accumulation point ball
		new PartialParticle(Particle.REDSTONE, mAccumulationPoint, 5).delta(0.15).data(mLaserOptions).spawnAsBoss();
	}

	private void laserHitPlayer(Player hitPlayer) {
		DamageUtils.damage(mBoss, hitPlayer, DamageEvent.DamageType.FIRE, FIRE_DAMAGE, null, true, true, SPELL_NAME);
		DamageUtils.damage(mBoss, hitPlayer, DamageEvent.DamageType.MAGIC, MAGIC_DAMAGE, null, true, true, SPELL_NAME);
		hitPlayer.playSound(hitPlayer, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.HOSTILE, 1, 1);
		hitPlayer.setFireTicks(hitPlayer.getFireTicks() + 60);
	}

	private void drawLaserToLocation(Location from, Location to) {
		new PPLine(Particle.REDSTONE, from, to).countPerMeter(1.5).data(mLaserOptions).offset(FastUtils.randomDoubleInRange(0, 0.5)).spawnAsBoss();
		new PartialParticle(Particle.REDSTONE, from.clone().add(1, 0, 0), 3).data(mLaserOptions).delta(0.25).spawnAsBoss();
		new PartialParticle(Particle.SMOKE_LARGE, to, 1).spawnAsBoss();
	}

	private void moveLasers() {
		for (Location eye : mEyes) {
			Vector newDir = VectorUtils.rotationToVector(FastUtils.randomDoubleInRange(MIN_YAW, MAX_YAW), FastUtils.randomDoubleInRange(MIN_PITCH, MAX_PITCH));
			// Telegraph new location
			LocationUtils.rayTraceToBlock(mAccumulationPoint, newDir, 300, (hitLoc) -> {
				new PPLine(Particle.WAX_ON, mAccumulationPoint, hitLoc).countPerMeter(2.5).spawnAsBoss();
				LocationUtils.rayTraceToBlock(mAccumulationPoint, eye.getDirection(), 300, (hitLocOld) -> {
					new PPLine(Particle.WAX_ON, hitLoc, hitLocOld).countPerMeter(2.5).spawnAsBoss();
				});
			});

			// Interpolation
			new BukkitRunnable() {
				final Location mStartLoc = eye;
				final Vector mStartDir = eye.getDirection();
				final Vector mTargetDir = newDir.clone();
				final Vector mDifferences = mTargetDir.clone().subtract(mStartDir.clone());

				int mTicks = 1;

				@Override
				public void run() {
					double progress = INTERPOLATION_FUNCTION.apply(mTicks);
					Vector differencesWithProgress = mDifferences.clone().multiply(progress);
					mStartLoc.setDirection(mStartDir.clone().add(differencesWithProgress));

					if (mTicks >= INTERPOLATION_DURATION) {
						cancel();
						return;
					}
					mTicks++;
				}
			}.runTaskTimer(Plugin.getInstance(), 0, 1);
		}
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}

}
