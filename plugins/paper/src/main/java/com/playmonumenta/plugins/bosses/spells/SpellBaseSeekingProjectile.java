package com.playmonumenta.plugins.bosses.spells;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellBaseSeekingProjectile extends Spell {

	@FunctionalInterface
	public interface AestheticAction {
		/**
		 * @param loc Location to do aesthetics, also provides a ticks ongoing parameter if operations should be performed less often
		 */
		void run(World world, Location loc, int ticks);
	}

	@FunctionalInterface
	public interface HitAction {
		/**
		 * Called when the projectile intersects a player (or possibly a block)
		 * @param target Player being targeted (null if hit a block)
		 * @param loc    Location where the projectile hit (either at player or block)
		 */
		void run(World world, LivingEntity target, Location loc);
	}

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final int mRange;
	private final boolean mSingleTarget;
	private final boolean mLaunchTracking;
	private final int mCooldown;
	private final int mDelay;
	private final double mSpeed;
	private final double mTurnRadius;
	private final int mLifetimeTicks;
	private final double mHitboxLength;
	private final boolean mCollidesWithBlocks;
	private final boolean mLingers;
	private final AestheticAction mInitiateAesthetic;
	private final AestheticAction mLaunchAesthetic;
	private final AestheticAction mProjectileAesthetic;
	private final HitAction mHitAction;
	private final int mCollisionCheckDelay;
	private final boolean mCollidesWithOthers;
	private final GetSpellTargets<LivingEntity> mGetSpellTargets;

	public SpellBaseSeekingProjectile(Plugin plugin, LivingEntity boss, int range, boolean singleTarget, boolean launchTracking, int cooldown, int delay,
			double speed, double turnRadius, int lifetimeTicks, double hitboxLength, boolean collidesWithBlocks, boolean lingers,
			AestheticAction initiateAesthetic, AestheticAction launchAesthetic, AestheticAction projectileAesthetic, HitAction hitAction) {
		this(plugin, boss, range, singleTarget, launchTracking, cooldown, delay,
				speed, turnRadius, lifetimeTicks, hitboxLength, collidesWithBlocks, lingers, 0, false,
				initiateAesthetic, launchAesthetic, projectileAesthetic, hitAction);
	}

	/**
	 * @param plugin              Plugin
	 * @param boss                Boss
	 * @param range               Range within which players may be targeted
	 * @param singleTarget        Target random player (true) or all players (false)
	 * @param launchTracking      Launch projectile at where the player is at time of launch (true) or was at time of spell nitiation (false)
	 * @param cooldown            How often this spell can be cast
	 * @param delay               How long between spell initiation and projectile launch
	 * @param speed               How many blocks per tick the projectile travels
	 * @param turnRadius          How many radians per tick the projectile can turn (higher values = tighter tracking, 0 = no tracking)
	 * @param lifetimeTicks       How many ticks before the projectile should dissipate
	 * @param hitboxLength        Dimensions of the projectile hitbox
	 * @param collidesWithBlocks  Whether the projectile should dissipate upon contact with a block
	 * @param lingers             Whether the projectile should dissipate upon boss death
	 * @param initiateAesthetic   Called when the attack initiates
	 * @param launchAesthetic     Called when the projectile is launched
	 * @param projectileAesthetic Called each tick at projectile locations
	 * @param hitAction           Called when the projectile intersects a player (or possibly a block)
	 */
	public SpellBaseSeekingProjectile(Plugin plugin, LivingEntity boss, int range, boolean singleTarget, boolean launchTracking, int cooldown, int delay,
			double speed, double turnRadius, int lifetimeTicks, double hitboxLength, boolean collidesWithBlocks, boolean lingers, int collisionCheckDelay, boolean collidesWithOthers,
			AestheticAction initiateAesthetic, AestheticAction launchAesthetic, AestheticAction projectileAesthetic, HitAction hitAction) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mRange = range;
		mSingleTarget = singleTarget;
		mLaunchTracking = launchTracking;
		mCooldown = cooldown;
		mSpeed = speed;
		mDelay = delay;
		mTurnRadius = Math.max(0, Math.min(Math.PI, turnRadius));
		mLifetimeTicks = lifetimeTicks;
		mHitboxLength = hitboxLength;
		mCollidesWithBlocks = collidesWithBlocks;
		mLingers = lingers;
		mInitiateAesthetic = initiateAesthetic;
		mLaunchAesthetic = launchAesthetic;
		mProjectileAesthetic = projectileAesthetic;
		mHitAction = hitAction;
		mCollisionCheckDelay = collisionCheckDelay;
		mCollidesWithOthers = collidesWithOthers;

		//not used
		mGetSpellTargets = null;
	}

	public SpellBaseSeekingProjectile(Plugin plugin, LivingEntity boss, boolean launchTracking, int cooldown, int delay,
			double speed, double turnRadius, int lifetimeTicks, double hitboxLength, boolean collidesWithBlocks, boolean lingers, int collisionCheckDelay, boolean collidesWithOthers,
			GetSpellTargets<LivingEntity> targets, AestheticAction initiateAesthetic, AestheticAction launchAesthetic, AestheticAction projectileAesthetic, HitAction hitAction) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mLaunchTracking = launchTracking;
		mCooldown = cooldown;
		mSpeed = speed;
		mDelay = delay;
		mTurnRadius = Math.max(0, Math.min(Math.PI, turnRadius));
		mLifetimeTicks = lifetimeTicks;
		mHitboxLength = hitboxLength;
		mCollidesWithBlocks = collidesWithBlocks;
		mLingers = lingers;
		mInitiateAesthetic = initiateAesthetic;
		mLaunchAesthetic = launchAesthetic;
		mProjectileAesthetic = projectileAesthetic;
		mHitAction = hitAction;
		mCollisionCheckDelay = collisionCheckDelay;
		mCollidesWithOthers = collidesWithOthers;
		mGetSpellTargets = targets;

		mSingleTarget = false;
		//it should be not used since mGetSpellTargets will handle also the singletarget

		mRange = 50;
		//used to calc if the projectile should stop before since the player is to far.
	}


	@Override
	public void run() {
		mInitiateAesthetic.run(mWorld, mBoss.getEyeLocation(), 0);

		if (mGetSpellTargets != null) {
			List<LivingEntity> entities = mGetSpellTargets.getTargets();
			Map<LivingEntity, Location> locations = new HashMap<LivingEntity, Location>();
			if (!mLaunchTracking) {
				for (LivingEntity target : entities) {
					locations.put(target, target.getEyeLocation());
				}
			}
			BukkitRunnable initiateSpell = new BukkitRunnable() {
				List<LivingEntity> mTargets = entities;
				Map<LivingEntity, Location> mLocations = locations;

				@Override
				public void run() {
					if (!mLaunchTracking) {
						for (Map.Entry<LivingEntity, Location> entry : mLocations.entrySet()) {
							LivingEntity target = entry.getKey();
							launch(target, entry.getValue());
						}
					} else {
						for (LivingEntity target : mTargets) {
							launch(target, target.getEyeLocation());
						}
					}

				}

			};

			initiateSpell.runTaskLater(mPlugin, mDelay);
			mActiveRunnables.add(initiateSpell);
			return;

		}

		final List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mRange, false);

		Map<Player, Location> locations = new HashMap<Player, Location>();
		if (!mLaunchTracking) {
			for (Player player : players) {
				locations.put(player, player.getEyeLocation());
			}
		}

		BukkitRunnable initiateSpell = new BukkitRunnable() {
			List<Player> mPlayers = players;
			Map<Player, Location> mLocations = locations;

			@Override
			public void run() {
				if (!mPlayers.isEmpty()) {
					if (mSingleTarget) {
						// Single target chooses a random player within range
						Collections.shuffle(mPlayers);
						for (Player player : mPlayers) {
							if (LocationUtils.hasLineOfSight(mBoss, player)) {
								if (!mLaunchTracking) {
									launch(player, mLocations.get(player));
								} else {
									launch(player, player.getEyeLocation());
								}
								return;
							}
						}
					} else {
						// Otherwise target all players within range
						if (!mLaunchTracking) {
							for (Map.Entry<Player, Location> entry : mLocations.entrySet()) {
								Player player = entry.getKey();
								if (LocationUtils.hasLineOfSight(mBoss, player)) {
									launch(player, entry.getValue());
								}
							}
						} else {
							for (Player player : mPlayers) {
								if (LocationUtils.hasLineOfSight(mBoss, player)) {
									launch(player, player.getEyeLocation());
								}
							}
						}
					}
				}
			}
		};

		initiateSpell.runTaskLater(mPlugin, mDelay);
		mActiveRunnables.add(initiateSpell);
	}

	@Override
	public boolean canRun() {
		if (EntityUtils.isStunned(mBoss) || EntityUtils.isSilenced(mBoss) || EntityUtils.isConfused(mBoss)) {
			return false;
		}
		if (mGetSpellTargets != null) {
			return !mGetSpellTargets.getTargets().isEmpty();
		}
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mRange, false);

		if (!players.isEmpty()) {
			for (Player player : players) {
				if (LocationUtils.hasLineOfSight(mBoss, player)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}

	public <V extends LivingEntity> void launch(V target, Location targetLoc) {
		mLaunchAesthetic.run(mWorld, mBoss.getEyeLocation(), 0);

		BukkitRunnable runnable = new BukkitRunnable() {
			Location mLocation = mBoss.getEyeLocation();
			BoundingBox mHitbox = BoundingBox.of(mLocation, mHitboxLength / 2, mHitboxLength / 2, mHitboxLength / 2);
			V mTarget = target;
			Vector mDirection = targetLoc.subtract(mLocation).toVector().normalize();
			int mTicks = 0;
			int mCollisionDelayTicks = mCollisionCheckDelay;

			@Override
			public void run() {
				mTicks++;
				mCollisionDelayTicks--;

				if (mTarget != null && (!mTarget.isValid() || mTarget.isDead())) {
					this.cancel();
					if (!mLingers) {
						mActiveRunnables.remove(this);
					}
					onEndAction(mLocation, mHitbox);
					return;
				}

				if (mTarget != null) {
					Vector newDirection = mTarget.getLocation().add(0, 1.0, 0).subtract(mLocation).toVector();
					if (newDirection.length() > 2 * mRange) {
						this.cancel();
						if (!mLingers) {
							mActiveRunnables.remove(this);
						}
						onEndAction(mLocation, mHitbox);
						return;
					}
					newDirection.normalize();
					// Because of double rounding errors, the dot product could be something stupid like 1.0000000000000002 (true story), so pre-process it before taking acos()
					double newAngle = Math.acos(Math.max(-1, Math.min(1, mDirection.dot(newDirection))));

					// This is some weird trigonometry stuff but I'm pretty sure it works
					if (newAngle < mTurnRadius) {
						mDirection = newDirection;
					} else {
						double halfEndpointDistance = FastUtils.sin(newAngle / 2);

						// Only do calculations if there's actually a direction change
						if (halfEndpointDistance != 0) {
							double scalar = (halfEndpointDistance + FastUtils.sin(mTurnRadius - newAngle / 2)) / (2 * halfEndpointDistance);
							mDirection.add(newDirection.subtract(mDirection).multiply(scalar)).normalize();
						}
					}
				}

				Vector shift = mDirection.clone().multiply(mSpeed);

				Block block = mLocation.getBlock();
				if (mCollidesWithBlocks && mCollisionDelayTicks <= 0) {
					if (!block.isLiquid() && mHitbox.overlaps(block.getBoundingBox())) {
						mHitAction.run(mWorld, null, mLocation.subtract(mDirection.multiply(0.5)));
						this.cancel();
						if (!mLingers) {
							mActiveRunnables.remove(this);
						}
						return;
					}
				} else {
					if (mHitbox.overlaps(block.getBoundingBox())) {
						if (block.isLiquid()) {
							shift.multiply(0.5);
						} else {
							// If going through blocks, increase the effects
							mProjectileAesthetic.run(mWorld, mLocation, mTicks);
							shift.multiply(0.125);
						}
					}
				}
				mLocation.add(shift);
				mHitbox.shift(shift);
				mProjectileAesthetic.run(mWorld, mLocation, mTicks);

				if (mCollisionDelayTicks <= 0) {
					// Grab all players that could have overlapping bounding boxes
					if (mGetSpellTargets != null) {
						Collection<Entity> entities = mLocation.getWorld().getNearbyEntities(mLocation, mHitboxLength + 2, mHitboxLength + 2, mHitboxLength + 2);
						for (Entity entity : entities) {
							if (entity instanceof LivingEntity && mHitbox.overlaps(entity.getBoundingBox()) && (mTarget.equals(entity) || !mCollidesWithOthers)) {
								mHitAction.run(mWorld, mTarget, mLocation);
								this.cancel();
								if (!mLingers) {
									mActiveRunnables.remove(this);
								}
								return;
							}
						}
					}


					for (Player player : PlayerUtils.playersInRange(mLocation, mHitboxLength + 2, true)) {
						if (mHitbox.overlaps(player.getBoundingBox()) && (player.equals(mTarget) || !mCollidesWithOthers)) {
							mHitAction.run(mWorld, player, mLocation);
							this.cancel();
							if (!mLingers) {
								mActiveRunnables.remove(this);
							}
							return;
						}
					}
				}

				if (mTicks > mLifetimeTicks) {
					this.cancel();
					if (!mLingers) {
						mActiveRunnables.remove(this);
					}
					onEndAction(mLocation, mHitbox);
				}
			}
		};

		runnable.runTaskTimer(mPlugin, 1, 1);
		if (!mLingers) {
			mActiveRunnables.add(runnable);
		}
	}

	//Does not run if the projectile hits/collides
	protected void onEndAction(Location projLoc, BoundingBox projHitbox) {
		//Do nothing, must be overriden in subclasses to be used
	}
}
