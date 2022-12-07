package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Collection;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
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
	/**
	 * mPlugin
	 * mBoss
	 * lobs
	 * explodeOnTouch
	 * explodeDelay -> if not explodeOnTouch ?  ticks to wait before checking for entity overlap to this block (explode) : ticks to wait before the lob explode when hitting the gound
	 * cooldown
	 *
	 * damage & effects
	 *
	 * getGranadeTarget -> return a list of livingEntity where we should launch a lobs
	 * getExplosionTarget -> return a list of livingEntity where that are hit by the explosion
	 * initSpellAesthetics -> void for Aesthetics on Boss
	 * grenadeAesthetics -> void for Aesthetics on Grenade
	 * explosionAesthetics -> void for Aesthetics explosion
	 *
	 *
	 */

	/**
	 * @param plugin              The monumenta plugin for runnables
	 * @param boss                The mob that is casting this ability
	 * @param grenadeMaterial     Material for the grenade
	 * @param explodeOnTouch      if the grenade should explode before touching the ground when hit another entity
	 * @param explodeDelay        if not explodeOnTouch ?  ticks to wait before checking for entity overlap to this block (explode) : ticks to wait before the lob explode when hitting the gound
	 * @param lobs                number of lobs casted per target
	 * @param lobsDelay           ticks between each lobs
	 * @param duration            the max duration of the grenade
	 * @param cooldown            cooldown of this spell
	 * @param lingeringDuration   the duration of the lingering
	 * @param lingeringRadius     the range of the lingering
	 * @param grenadeTargets      return the target of the grenade
	 * @param explosionTargets    return the target of the explosion and also used for the lingering
	 * @param aestheticsBoss      aesthetics launch at the start of the spell
	 * @param grenadeAesthetics   aesthetics launch at grenade location each tick
	 * @param explosionAesthetics aesthetics launch when the grenade explode
	 * @param hitAction           action called for each livingentity that explosionTargets return
	 * @param ringAesthetics      aesthetics for the ring lingering
	 * @param cencterAesthetics   aesthetics for the cencter of the ring lingering
	 * @param lingeringHitAction  action called for each livingentity that explosionTargets return if inside of the range lingeringRadius
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
		LingeringCenterAesthetics cencterAesthetics,
		HitAction lingeringHitAction
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
		mLingeringCenterAesthetics = cencterAesthetics;
		mLingeringRingAesthetics = ringAesthetics;
		mLingeringHit = lingeringHitAction;


	}


	@Override
	public void run() {
		final Location bossLocation = mBoss.getLocation();
		List<? extends LivingEntity> targets = mGrenadeTargets.getTargets();

		if (!targets.isEmpty()) {
			mAestheticsBoss.launch(mBoss, bossLocation);
			for (LivingEntity target : targets) {
				new BukkitRunnable() {
					int mLobsLaunched = 0;

					@Override
					public void run() {
						launchGrenade(bossLocation, target);
						mLobsLaunched++;
						if (mLobsLaunched >= mLobs) {
							cancel();
						}
					}
				}.runTaskTimer(mPlugin, 0, mLobsDelay);
			}
		}
	}

	private void launchGrenade(Location bossLocation, LivingEntity target) {
		try {
			FallingBlock fallingBlock = bossLocation.getWorld().spawnFallingBlock(mBoss.getEyeLocation().add(0, 1, 0), mGrenadeMaterial.createBlockData());
			fallingBlock.setDropItem(false);
			Location pLoc = target.getLocation();
			Location tLoc = fallingBlock.getLocation();
			Vector vect = new Vector(pLoc.getX() - tLoc.getX(), 0, pLoc.getZ() - tLoc.getZ());
			vect.normalize().multiply(pLoc.distance(tLoc) / 20).setY(0.7f);
			fallingBlock.setVelocity(vect);

			BukkitRunnable runn = new BukkitRunnable() {
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
							if (blockLocation.getBlock().getType() == mFallingBlock.getBlockData().getMaterial()) {
								blockLocation.getBlock().setType(Material.AIR);
							}

							mExplosionAesthetics.launch(mBoss, blockLocation);
							List<? extends LivingEntity> targets = mExplosionTargets.getTargets(blockLocation);
							launchLingering(blockLocation);
							for (LivingEntity target : targets) {
								mHitAction.launch(mBoss, target, blockLocation);
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
						this.cancel();
						return;
					}

					mTicks += 1;
				}

				@Override
				public synchronized void cancel() throws IllegalStateException {
					super.cancel();
					mFallingBlock.remove();
					if (mFallingBlock.getLocation().getBlock().getType() == mFallingBlock.getBlockData().getMaterial()) {
						mFallingBlock.getLocation().getBlock().setType(Material.AIR);
					}
					mActiveRunnables.remove(this);
				}


				void launchLingering(Location startingLoc) {
					if (mLingeringDuration > 0) {
						new BukkitRunnable() {
							double mRadius = mLingeringRadius;
							Location mCenter = startingLoc;
							int mTimer = 0;
							int mRevolutionDegrees = 360;
							double mCurrentDegrees = FastUtils.randomDoubleInRange(0, mRevolutionDegrees);
							double mDegreeSpeed = mRevolutionDegrees / (mLingeringDuration / (mRadius / 2));

							@Override
							public void run() {
								mCurrentDegrees += mDegreeSpeed;
								double offsetX = FastUtils.sinDeg(mCurrentDegrees) * mRadius;
								double offsetZ = FastUtils.cosDeg(mCurrentDegrees) * mRadius;
								Location spawningLoc = mCenter.clone();
								spawningLoc.add(offsetX, 0, offsetZ);
								mLingeringRingAesthetics.launch(spawningLoc);
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
			runn.runTaskTimer(mPlugin, 0, 1);
			mActiveRunnables.add(runn);

		} catch (Exception e) {
			mPlugin.getLogger().warning("Failed to summon grenade for SpellBaseGrenadeLauncher, mob:'" + mBoss.getName() + "' Reason: " + e.getMessage());
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
}
