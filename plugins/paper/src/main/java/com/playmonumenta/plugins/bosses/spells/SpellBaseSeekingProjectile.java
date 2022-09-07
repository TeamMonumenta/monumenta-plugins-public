package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellBaseSeekingProjectile extends Spell {

	@FunctionalInterface
	public interface AestheticAction {
		/**
		 * Run with the location to do aesthetics, also provides a ticks ongoing parameter if operations should be performed less often
		 */
		void run(World world, Location loc, int ticks);
	}

	@FunctionalInterface
	public interface HitAction {
		/**
		 * Called when the projectile intersects a player (or possibly a block)
		 *
		 * @param target Player being targeted (null if hit a block)
		 * @param loc    Location where the projectile hit (either at player or block)
		 */
		void run(World world, @Nullable LivingEntity target, Location loc);
	}

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final int mRange;
	private final boolean mSingleTarget;
	private final boolean mLaunchTracking;
	private final int mCharge;
	private final int mChargeInterval;
	private final double mOffsetLeft;
	private final double mOffsetUp;
	private final double mOffsetFront;
	private final int mSplit;
	private final double mSplitAngle;
	private final int mMirror;
	private final double mFixYaw;
	private final double mFixPitch;
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

	private final boolean mFixed;
	private int mChargeRemain;

	public SpellBaseSeekingProjectile(Plugin plugin, LivingEntity boss, int range, boolean singleTarget, boolean launchTracking, int cooldown, int delay,
			double speed, double turnRadius, int lifetimeTicks, double hitboxLength, boolean collidesWithBlocks, boolean lingers,
			AestheticAction initiateAesthetic, AestheticAction launchAesthetic, AestheticAction projectileAesthetic, HitAction hitAction) {
		this(plugin, boss, range, singleTarget, launchTracking, cooldown, delay,
				speed, turnRadius, lifetimeTicks, hitboxLength, collidesWithBlocks, lingers, 0, false,
				initiateAesthetic, launchAesthetic, projectileAesthetic, hitAction);
	}

	/**
	 * @param range               Range within which players may be targeted
	 * @param singleTarget        Target random player (true) or all players (false)
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
		mCharge = 1;
		mChargeInterval = 100;
		mCooldown = cooldown;
		mDelay = delay;
		mOffsetLeft = 0;
		mOffsetUp = 0;
		mOffsetFront = 0;
		mSplit = 1;
		mSplitAngle = 30;
		mMirror = 0;
		mFixed = false;
		mFixYaw = 0.0;
		mFixPitch = 0.0;
		mSpeed = speed;
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

		//initialize
		mChargeRemain = 0;
	}
	//Constructors above are redirected to this one.

	public SpellBaseSeekingProjectile(Plugin plugin, LivingEntity boss, boolean launchTracking, int cooldown, int delay,
									  double speed, double turnRadius, int lifetimeTicks, double hitboxLength, boolean collidesWithBlocks, boolean lingers, int collisionCheckDelay, boolean collidesWithOthers,
									  GetSpellTargets<LivingEntity> targets, AestheticAction initiateAesthetic, AestheticAction launchAesthetic, AestheticAction projectileAesthetic, HitAction hitAction) {
		this(plugin, boss, launchTracking, 1, 40, cooldown, delay,
			0, 0, 0, 0, 200.0, 100.0, 1, 30, speed, turnRadius,
			lifetimeTicks, hitboxLength, lingers, collidesWithBlocks, collidesWithOthers, collisionCheckDelay,
			targets, initiateAesthetic, launchAesthetic, projectileAesthetic, hitAction);
	}

	/**
	 * @param plugin              Plugin
	 * @param boss                Boss
	 * @param launchTracking      Launch projectile at where the player is at time of launch (true) or was at time of spell nitiation (false)
	 * @param charge              Repeatly launch projectile for specific times
	 * @param chargeInterval      Interval between casting charges
	 * @param cooldown            How often this spell can be cast
	 * @param delay               How long between spell initiation and projectile launch
	 * @param offsetX             X-offset from mob's eye to projectile's start (Left)
	 * @param offsetY             Y-offset from mob's eye to projectile's start (Up)
	 * @param offsetZ             Z-offset from mob's eye to projectile's start (Front)
	 * @param mirror              Generate duplicated projectiles. 1 = L-R, 2 = F-B, 4 = U-D
	 * @param fixYaw              Force projectile shoot in a yaw relative to shooter
	 * @param fixPitch            Force projectile shoot in a pitch relative to shooter
	 * @param split               How many projectiles to be launched in a sector plane
	 * @param splitAngle          Angles between splited projectiles in degree
	 * @param speed               How many blocks per tick the projectile travels
	 * @param turnRadius          How many radians per tick the projectile can turn (higher values = tighter tracking, 0 = no tracking)
	 * @param lifetimeTicks       How many ticks before the projectile should dissipate
	 * @param hitboxLength        Dimensions of the projectile hitbox
	 * @param lingers             Whether the projectile should dissipate upon boss death
	 * @param collidesWithBlocks  Whether the projectile should dissipate upon contact with a block
	 * @param initiateAesthetic   Called when the attack initiates
	 * @param launchAesthetic     Called when the projectile is launched
	 * @param projectileAesthetic Called each tick at projectile locations
	 * @param hitAction           Called when the projectile intersects a player (or possibly a block)
	 */
	public SpellBaseSeekingProjectile(Plugin plugin, LivingEntity boss, boolean launchTracking, int charge, int chargeInterval, int cooldown, int delay,
									  double offsetX, double offsetY, double offsetZ, int mirror, double fixYaw, double fixPitch, int split, double splitAngle, double speed, double turnRadius,
									  int lifetimeTicks, double hitboxLength, boolean lingers, boolean collidesWithBlocks, boolean collidesWithOthers, int collisionCheckDelay,
									  GetSpellTargets<LivingEntity> targets, AestheticAction initiateAesthetic, AestheticAction launchAesthetic, AestheticAction projectileAesthetic, HitAction hitAction) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mLaunchTracking = launchTracking;
		mDelay = delay;
		mCooldown = cooldown;
		mCharge = charge;
		mChargeInterval = chargeInterval;
		mOffsetLeft = offsetX;
		mOffsetUp = offsetY;
		mOffsetFront = offsetZ;
		mSplit = Math.max(split, 1);
		mSplitAngle = Math.max(splitAngle, 0);
		mMirror = mirror < 0 || mirror >= 8 ? 0 : mirror;
		mFixed = !((fixYaw > 180.0) || (fixYaw < -180.0) || (fixPitch > 90.0) || (fixPitch < -90.0));
		mFixYaw = fixYaw;
		mFixPitch = fixPitch;
		mSpeed = speed;
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

		//it should be not used since mGetSpellTargets will handle also the singletarget
		mSingleTarget = false;
		//used to calc if the projectile should stop before since the player is to far.
		mRange = 50;

		//initialize
		mChargeRemain = 0;
	}
	//Constructors above are redirected to this one.

	@Override
	public void run() {
		mInitiateAesthetic.run(mWorld, mBoss.getEyeLocation(), 0);

		if (mGetSpellTargets != null) {
			List<? extends LivingEntity> entities = mGetSpellTargets.getTargets();
			Map<LivingEntity, Location> locations = new HashMap<LivingEntity, Location>();
			if (!mLaunchTracking) {
				for (LivingEntity target : entities) {
					locations.put(target, target.getEyeLocation());
				}
			}
			BukkitRunnable initiateSpell = new BukkitRunnable() {
				final List<? extends LivingEntity> mTargets = entities;
				final Map<LivingEntity, Location> mLocations = locations;

				@Override
				public void run() {
					if (!mLaunchTracking) {
						for (Map.Entry<LivingEntity, Location> entry : mLocations.entrySet()) {
							LivingEntity target = entry.getKey();
							launchDX(target, entry.getValue(), mOffsetLeft, mOffsetUp, mOffsetFront, mSplit, mSplitAngle, mMirror, mFixYaw, mFixPitch);
						}
					} else {
						for (LivingEntity target : mTargets) {
							launchDX(target, target.getEyeLocation(), mOffsetLeft, mOffsetUp, mOffsetFront, mSplit, mSplitAngle, mMirror, mFixYaw, mFixPitch);
						}
					}

				}

			};

			initiateSpell.runTaskLater(mPlugin, mDelay);
			mActiveRunnables.add(initiateSpell);
			consumeCharge();
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
			final List<Player> mPlayers = players;
			final Map<Player, Location> mLocations = locations;

			@Override
			public void run() {
				if (!mPlayers.isEmpty()) {
					if (mSingleTarget) {
						// Single target chooses a random player within range
						Collections.shuffle(mPlayers);
						for (Player player : mPlayers) {
							if (LocationUtils.hasLineOfSight(mBoss, player)) {
								if (!mLaunchTracking) {
									launchDX(player, mLocations.get(player), mOffsetLeft, mOffsetUp, mOffsetFront, mSplit, mSplitAngle, mMirror, mFixYaw, mFixPitch);
								} else {
									launchDX(player, player.getEyeLocation(), mOffsetLeft, mOffsetUp, mOffsetFront, mSplit, mSplitAngle, mMirror, mFixYaw, mFixPitch);
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
									launchDX(player, entry.getValue(), mOffsetLeft, mOffsetUp, mOffsetFront, mSplit, mSplitAngle, mMirror, mFixYaw, mFixPitch);
								}
							}
						} else {
							for (Player player : mPlayers) {
								if (LocationUtils.hasLineOfSight(mBoss, player)) {
									launchDX(player, player.getEyeLocation(), mOffsetLeft, mOffsetUp, mOffsetFront, mSplit, mSplitAngle, mMirror, mFixYaw, mFixPitch);
								}
							}
						}
					}
				}
			}
		};

		initiateSpell.runTaskLater(mPlugin, mDelay);
		mActiveRunnables.add(initiateSpell);
		consumeCharge();
	}

	@Override
	public boolean canRun() {
		if (EntityUtils.isStunned(mBoss) || EntityUtils.isSilenced(mBoss)) {
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
		return mChargeRemain > 0 ? mChargeInterval : mCooldown;
	}

	public <V extends LivingEntity> void launchDX(V target, Location targetLoc, double offsetX, double offsetY, double offsetZ,
												  int split, double splitAngle, int mirror, double fixYaw, double fixPitch) {
		// yaw degrees of splits
		double[] yaws = new double[split];
		for (int i = 0; i < split; i++) {
			yaws[i] = splitAngle * (i - (split - 1) / 2.0);
			launch(target, targetLoc, mFixed, fixYaw, fixPitch, offsetX, offsetY, offsetZ, yaws[i], 0);
			if (mirror % 2 == 1) {
				launch(target, targetLoc, mFixed, -fixYaw, fixPitch, -offsetX, offsetY, offsetZ, yaws[i], 0);
			}
			if (mirror >= 2) {
				launch(target, targetLoc, mFixed, fixYaw >= 0 ? (180.0 - fixYaw) : (-180.0 - fixYaw),
					fixPitch, offsetX, offsetY, -offsetZ, yaws[i], 0);
			}
			if (mirror == 3) {
				launch(target, targetLoc, mFixed, fixYaw >= 0 ? (fixYaw - 180.0) : (fixYaw + 180.0),
					fixPitch, -offsetX, offsetY, -offsetZ, yaws[i], 0);
			}
		}

	}

	// normal launch
	public <V extends LivingEntity> void launch(V target, Location targetLoc) {
		launch(target, targetLoc, false, 0, 0.0, 0, 0, 0, 0.0, 0.0);
	}

	public <V extends LivingEntity> void launch(V target, Location targetLoc, boolean fixed, double fYaw, double fPitch,
												double offsetX, double offsetY, double offsetZ, double offsetYaw, double offsetPitch) {
		mLaunchAesthetic.run(mWorld, mBoss.getEyeLocation(), 0);

		BukkitRunnable runnable = new BukkitRunnable() {
			//Start point of projectiles
			Location mLocation = mBoss.getEyeLocation().add(VectorUtils.rotateYAxis(
				VectorUtils.rotateXAxis(new Vector(offsetX, offsetY, offsetZ), mBoss.getLocation().getPitch()),
				mBoss.getLocation().getYaw()));
			BoundingBox mHitbox = BoundingBox.of(mLocation, mHitboxLength / 2, mHitboxLength / 2, mHitboxLength / 2);
			V mTarget = target;
			//Base direction of projectiles
			Vector mBaseDir = !fixed ? targetLoc.clone().subtract(mLocation).toVector().normalize() :
				VectorUtils.rotationToVector(fYaw + mBoss.getLocation().getYaw(), fPitch);
			//Hint: Clone is important for multiple launching
			//Vector mDirection = targetLoc.clone().subtract(mLocation).toVector().normalize();
			Vector mDirection = VectorUtils.rotateTargetDirection(
				mBaseDir, offsetYaw, offsetPitch);

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
					Vector newDirection = mTarget.getEyeLocation().subtract(mLocation).toVector();
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
						Collection<LivingEntity> entities = mLocation.getWorld().getNearbyEntitiesByType(LivingEntity.class, mLocation, mHitboxLength + 2);
						for (LivingEntity entity : entities) {
							if (mHitbox.overlaps(entity.getBoundingBox()) && !mBoss.equals(entity) && (mTarget.equals(entity) || mCollidesWithOthers)) {
								mHitAction.run(mWorld, entity, mLocation);
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

	//Reusable for other charge implementation
	private void consumeCharge() {
		if (mChargeRemain > 0) {
			mChargeRemain--;
		} else {
			mChargeRemain = mCharge - 1;
		}
	}

}
