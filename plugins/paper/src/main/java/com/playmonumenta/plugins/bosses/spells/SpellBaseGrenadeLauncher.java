package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.Collection;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellBaseGrenadeLauncher extends Spell {

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Material mGrenadeMaterial;
	private final Boolean mExplodeOnTouch;
	private final int mExplodeDelay;
	private final int mLobs;
	private final int mLobsDelay;
	private final int mDuration;
	private final int mCooldown;
	private final GetSpellTargets<LivingEntity> mGrenadeTargets;
	private final GetGrenadeTarget<LivingEntity> mExplosionTargets;
	private final InitAesthetics mAestheticsBoss;
	private final GrenadeAesthetics mGrenadeAesthetics;
	private final GrenadeAesthetics mExplosionAesthetics;
	private final HitAction mHitAction;


	//lingering stuff
	private final int mLingeringDuration;
	private final double mLingeringRadius;
	private final HitAction mLingeringHit;
	private final LingeringRingAesthetics mLingeringRingAesthetics;
	private final LingeringCenterAesthetics mLingeringCenterAesthetics;

	private final LoSPool mSummonPool;
	private final float mGrenadeYVelocity;
	private final double mThrowVariance;

	public SpellBaseGrenadeLauncher(
		Plugin plugin,
		LivingEntity boss,
		Material grenadeMaterial,
		Boolean explodeOnTouch,
		int explodeDelay,
		int lobs,
		int lobsDelay,
		int duration,
		int cooldown,

		//lingering stuff
		int lingeringDuration,
		double lingeringRadius,

		GetSpellTargets<LivingEntity> grenadeTargets,
		GetGrenadeTarget<LivingEntity> explosionTargets,
		InitAesthetics aestheticsBoss,
		GrenadeAesthetics grenadeAesthetics,
		GrenadeAesthetics explosionAesthetics,
		HitAction hitAction,

		//lingering stuff
		LingeringRingAesthetics ringAesthetics,
		LingeringCenterAesthetics centerAesthetics,
		HitAction lingeringHitAction
	) {
		this(plugin, boss, grenadeMaterial, explodeOnTouch, explodeDelay, lobs, lobsDelay, duration, cooldown,
			lingeringDuration, lingeringRadius, grenadeTargets, explosionTargets, aestheticsBoss, grenadeAesthetics,
			explosionAesthetics, hitAction, ringAesthetics, centerAesthetics, lingeringHitAction, LoSPool.EMPTY, 0.7f, 0.0);
	}

	/*
	 * mPlugin
	 * mBoss
	 * lobs
	 * explodeOnTouch
	 * explodeDelay -> if not explodeOnTouch ?  ticks to wait before checking for entity overlap to this block (explode) : ticks to wait before the lob explode when hitting the ground
	 * cooldown
	 *
	 * damage & effects
	 *
	 * getGrenadeTarget -> return a list of livingEntity where we should launch a lobs
	 * getExplosionTarget -> return a list of livingEntity where that are hit by the explosion
	 * initSpellAesthetics -> void for Aesthetics on Boss
	 * grenadeAesthetics -> void for Aesthetics on Grenade
	 * explosionAesthetics -> void for Aesthetics explosion
	 *
	 *
	 */

	/**
	 * @param plugin              The Monumenta plugin for runnables
	 * @param boss                The mob that is casting this ability
	 * @param grenadeMaterial     Material for the grenade
	 * @param explodeOnTouch      if the grenade should explode before touching the ground when hit another entity
	 * @param explodeDelay        if not explodeOnTouch ?  ticks to wait before checking for entity overlap to this block (explode) : ticks to wait before the lob explode when hitting the ground
	 * @param lobs                number of lobs cast per target
	 * @param lobsDelay           ticks between each lobs
	 * @param duration            the max duration of the grenade
	 * @param cooldown            Cooldown of this spell
	 * @param lingeringDuration   the duration of the lingering
	 * @param lingeringRadius     the range of the lingering
	 * @param grenadeTargets      return the target of the grenade
	 * @param explosionTargets    return the target of the explosion and also used for the lingering
	 * @param aestheticsBoss      aesthetics launch at the start of the spell
	 * @param grenadeAesthetics   aesthetics launch at grenade location each tick
	 * @param explosionAesthetics aesthetics launch when the grenade explode
	 * @param hitAction           action called for each LivingEntity that explosionTargets return
	 * @param ringAesthetics      aesthetics for the ring lingering
	 * @param centerAesthetics    aesthetics for the center of the ring lingering
	 * @param lingeringHitAction  action called for each LivingEntity that explosionTargets returns if inside lingeringRadius
	 * @param mobPool             the mob pool to be spawned when the grenade explodes
	 * @param throwVariance       variance of where the grenade will be thrown
	 */
	public SpellBaseGrenadeLauncher(
		Plugin plugin,
		LivingEntity boss,
		Material grenadeMaterial,
		Boolean explodeOnTouch,
		int explodeDelay,
		int lobs,
		int lobsDelay,
		int duration,
		int cooldown,

		//lingering stuff
		int lingeringDuration,
		double lingeringRadius,

		GetSpellTargets<LivingEntity> grenadeTargets,
		GetGrenadeTarget<LivingEntity> explosionTargets,
		InitAesthetics aestheticsBoss,
		GrenadeAesthetics grenadeAesthetics,
		GrenadeAesthetics explosionAesthetics,
		HitAction hitAction,

		//lingering stuff
		LingeringRingAesthetics ringAesthetics,
		LingeringCenterAesthetics centerAesthetics,
		HitAction lingeringHitAction,

		LoSPool mobPool,
		float yVelocity,
		double throwVariance
	) {
		mPlugin = plugin;
		mBoss = boss;
		mGrenadeMaterial = grenadeMaterial;
		mExplodeOnTouch = explodeOnTouch;
		mExplodeDelay = explodeDelay;
		mLobs = lobs;
		mLobsDelay = lobsDelay;
		mDuration = duration;
		mCooldown = cooldown;
		mGrenadeTargets = grenadeTargets;
		mExplosionTargets = explosionTargets;
		mAestheticsBoss = aestheticsBoss;
		mGrenadeAesthetics = grenadeAesthetics;
		mExplosionAesthetics = explosionAesthetics;
		mHitAction = hitAction;

		//lingering stuff
		mLingeringDuration = lingeringDuration;
		mLingeringRadius = lingeringRadius;
		mLingeringCenterAesthetics = centerAesthetics;
		mLingeringRingAesthetics = ringAesthetics;
		mLingeringHit = lingeringHitAction;

		mSummonPool = mobPool;
		mGrenadeYVelocity = yVelocity;
		mThrowVariance = throwVariance;
	}


	@Override
	public void run() {
		final Location bossLocation = mBoss.getLocation();
		List<? extends LivingEntity> targets = mGrenadeTargets.getTargets();

		if (!targets.isEmpty()) {
			mAestheticsBoss.launch(mBoss, bossLocation);
			for (LivingEntity target : targets) {
				new BukkitRunnable() {
					int mT = 0;

					@Override
					public void run() {
						if (EntityUtils.shouldCancelSpells(mBoss)) {
							this.cancel();
							return;
						}

						if (mT % mLobsDelay == 0) {
							launchGrenade(bossLocation, target);
							if (mT >= (mLobs - 1) * mLobsDelay) {
								this.cancel();
							}
						}

						mT++;
					}
				}.runTaskTimer(mPlugin, 0, 1);
			}
		}
	}

	private void launchGrenade(Location bossLocation, LivingEntity target) {
		try {
			FallingBlock fallingBlock = bossLocation.getWorld().spawnFallingBlock(mBoss.getEyeLocation().add(0, 1, 0), mGrenadeMaterial.createBlockData());
			fallingBlock.setDropItem(false);
			EntityUtils.disableBlockPlacement(fallingBlock);
			Location pLoc = target.getLocation();
			Location tLoc = fallingBlock.getLocation();

			// apply throw variance
			if (mThrowVariance != 0) {
				double r = FastUtils.randomDoubleInRange(0, mThrowVariance);
				double theta = FastUtils.randomDoubleInRange(0, 2 * Math.PI);
				double x = r * Math.cos(theta);
				double z = r * Math.sin(theta);
				pLoc.add(x, 0, z);
			}

			// approximate formula for the max height of a falling block's trajectory given its initial y-velocity
			double maxHeight = -0.453758* mGrenadeYVelocity + 12.6052*Math.pow(mGrenadeYVelocity, 2) +
				-3.75027*Math.pow(mGrenadeYVelocity, 3) + 0.906156*Math.pow(mGrenadeYVelocity, 4) +
				-0.114669*Math.pow(mGrenadeYVelocity, 5);

			// h = 0.5 * g * t^2
			// t^2 = 0.5 * g / h
			// t = sqrt(0.5 * g / h)
			double timeOfFlight = Math.sqrt(0.5 * 16 / maxHeight);

			Location endPoint = pLoc.clone();
			endPoint.setY(tLoc.getY());
			double distance = endPoint.distance(tLoc);
			double velocity = distance * timeOfFlight;

			// lessen velocity if the y-velocity is very low as to avoid overshooting, and vice versa
			velocity *= 1 + (mGrenadeYVelocity - 0.7);

			// Divide the actual velocity by 32 (speed at which things fall in minecraft; don't ask me why, but it works)
			Vector vel = new Vector(pLoc.getX() - tLoc.getX(), 0, pLoc.getZ() - tLoc.getZ()).normalize().multiply(velocity / 32);
			vel.setY(mGrenadeYVelocity);

			if (!Double.isFinite(vel.getX())) {
				vel = new Vector(0, 1, 0);
			}
			fallingBlock.setVelocity(vel);
			fallingBlock.addScoreboardTag("DisableBlockPlacement");

			BukkitRunnable run = new BukkitRunnable() {
				final FallingBlock mFallingBlock = fallingBlock;
				int mDelay = mExplodeDelay;
				int mTicks = 0;

				@Override
				public void run() {
					if (isCancelled()) {
						return;
					}

					Location blockLocation = mFallingBlock.getLocation();
					mGrenadeAesthetics.launch(mBoss, blockLocation);

					if (mExplodeOnTouch) {
						mDelay -= 1;
						BoundingBox box = mFallingBlock.getBoundingBox();
						//explosion on collision with entity
						Collection<LivingEntity> collide = bossLocation.getWorld().getNearbyEntitiesByType(LivingEntity.class, blockLocation, 5);
						for (LivingEntity entity : collide) {
							if (!entity.equals(mBoss) && entity.getBoundingBox().overlaps(box) && mDelay <= 0) {
								mFallingBlock.remove();
								mExplosionAesthetics.launch(mBoss, blockLocation);
								List<? extends LivingEntity> targets = mExplosionTargets.getTargets(blockLocation);
								launchLingering(blockLocation);
								for (LivingEntity target : targets) {
									mHitAction.launch(mBoss, target, blockLocation);
								}
								Entity spawn = mSummonPool.spawn(blockLocation);
								if (spawn != null) {
									summonPlugins(spawn);
								}

								this.cancel();
								return;

							}
						}
					}

					//explosion on ground
					if (mFallingBlock.isOnGround() || !mFallingBlock.isValid()) {
						mDelay -= 1;
						if (mDelay <= 0) {
							mFallingBlock.remove();
							mExplosionAesthetics.launch(mBoss, blockLocation);
							List<? extends LivingEntity> targets = mExplosionTargets.getTargets(blockLocation);
							launchLingering(blockLocation);
							for (LivingEntity target : targets) {
								mHitAction.launch(mBoss, target, blockLocation);
							}
							Entity spawn = mSummonPool.spawn(blockLocation);
							if (spawn != null) {
								summonPlugins(spawn);
							}

							this.cancel();
							return;
						}
					}

					if (mTicks >= mDuration) {
						mFallingBlock.remove();
						mExplosionAesthetics.launch(mBoss, blockLocation);
						List<? extends LivingEntity> targets = mExplosionTargets.getTargets(blockLocation);
						launchLingering(blockLocation);
						for (LivingEntity target : targets) {
							mHitAction.launch(mBoss, target, blockLocation);
						}
						Entity entity = mSummonPool.spawn(blockLocation);
						if (entity != null) {
							summonPlugins(entity);
						}

						this.cancel();
						return;
					}

					mTicks += 1;
				}

				@Override
				public synchronized void cancel() throws IllegalStateException {
					super.cancel();
					mFallingBlock.remove();
					mActiveRunnables.remove(this);
				}


				void launchLingering(Location startingLoc) {
					if (mLingeringDuration > 0) {
						new BukkitRunnable() {
							final double mRadius = mLingeringRadius;
							final Location mCenter = startingLoc;
							int mTimer = 0;
							final int mRevolutionDegrees = 360;
							double mCurrentDegrees = FastUtils.randomDoubleInRange(0, mRevolutionDegrees);
							final double mDegreeSpeed = mRevolutionDegrees / (mLingeringDuration / (mRadius / 2));

							@Override
							public void run() {
								mCurrentDegrees += mDegreeSpeed;
								Vector offset1 = new Vector(FastUtils.sinDeg(mCurrentDegrees) * mRadius, 0, FastUtils.cosDeg(mCurrentDegrees) * mRadius);
								Vector offset2 = new Vector(FastUtils.sinDeg(mCurrentDegrees + 90) * mRadius, 0, FastUtils.cosDeg(mCurrentDegrees + 90) * mRadius);
								Vector offset3 = new Vector(FastUtils.sinDeg(mCurrentDegrees + 180) * mRadius, 0, FastUtils.cosDeg(mCurrentDegrees + 180) * mRadius);
								Vector offset4 = new Vector(FastUtils.sinDeg(mCurrentDegrees + 270) * mRadius, 0, FastUtils.cosDeg(mCurrentDegrees + 270) * mRadius);

								mLingeringRingAesthetics.launch(mCenter.clone().add(offset1));
								mLingeringRingAesthetics.launch(mCenter.clone().add(offset2));
								mLingeringRingAesthetics.launch(mCenter.clone().add(offset3));
								mLingeringRingAesthetics.launch(mCenter.clone().add(offset4));
								mLingeringCenterAesthetics.launch(mCenter, mTimer);

								//each 10 ticks run damage on players

								if (mTimer % 10 == 0) {
									List<? extends LivingEntity> targets = mExplosionTargets.getTargets(mCenter);
									for (LivingEntity entity : targets) {
										if (entity.getLocation().distance(mCenter) <= mRadius) {
											mLingeringHit.launch(mBoss, entity, mCenter);
										}
									}
								}

								if (mTimer >= mLingeringDuration) {
									this.cancel();
									return;
								}

								mTimer += 2;
							}


							@Override
							public synchronized void cancel() throws IllegalStateException {
								super.cancel();
								mActiveRunnables.remove(this);
							}

						}.runTaskTimer(mPlugin, 0, 2);
					}
				}

			};
			run.runTaskTimer(mPlugin, 0, 1);
			mActiveRunnables.add(run);

		} catch (Exception e) {
			MMLog.warning("Failed to summon grenade for SpellBaseGrenadeLauncher, mob:'" + mBoss.getName() + "'", e);
		}

	}


	@Override
	public int cooldownTicks() {
		return mCooldown;
	}


	@FunctionalInterface
	public interface GetGrenadeTarget<V extends LivingEntity> {
		List<? extends V> getTargets(Location loc);
	}

	@FunctionalInterface
	public interface InitAesthetics {
		void launch(LivingEntity boss, Location loc);
	}

	@FunctionalInterface
	public interface GrenadeAesthetics {
		void launch(LivingEntity boss, Location loc);
	}

	@FunctionalInterface
	public interface HitAction {
		void launch(LivingEntity boss, LivingEntity target, Location location);
	}

	//Lingering stuff
	@FunctionalInterface
	public interface LingeringRingAesthetics {
		void launch(Location loc);
	}

	@FunctionalInterface
	public interface LingeringCenterAesthetics {
		void launch(Location loc, int ticks);
	}

	public void summonPlugins(Entity summon) {

	}
}
