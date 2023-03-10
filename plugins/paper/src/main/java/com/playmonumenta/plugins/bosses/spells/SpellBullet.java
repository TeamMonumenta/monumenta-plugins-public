package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class SpellBullet extends Spell {

	public enum Pattern {
		BORDER("border"),
		BORDER_2("border_2"),
		BORDER_1("border_1"),
		HARD_BORDER("hard_border"),
		JUNKO("junko"),
		HARD_JUNKO("hard_junko"),
		SANAE("sanae"),
		HARD_SANAE("hard_sanae"),
		POLYGRAPH("polygraph"),
		HARD_POLYGRAPH("hard_polygraph"),
		FREEZE("freeze"),
		HARD_FREEZE("hard_freeze"),
		NUE("nue"),
		HARD_NUE("hard_nue"),
		SLASH("slash"),
		HARD_SLASH("hard_slash"),
		MALLET("mallet"),
		HARD_MALLET("hard_mallet"),
		TRAPPER("trapper"),
		HARD_TRAPPER("hard_trapper"),
		INVALID("invalid");

		public final String mLabel;

		Pattern(String label) {
			this.mLabel = label;
		}

		public static Pattern valueOfLabel(String label) {
			for (Pattern e : values()) {
				if (e.mLabel.equals(label)) {
					return e;
				}
			}
			return INVALID;
		}
	}

	@FunctionalInterface
	public interface CastAction {
		void run(Entity entity);
	}

	@FunctionalInterface
	public interface TickAction {
		void run(Entity entity, int tick);
	}

	@FunctionalInterface
	public interface IntersectAction {
		void run(@Nullable Player player, Location loc, boolean blocked, @Nullable Location prevLoc);
	}

	private final Plugin mPlugin;
	private final LivingEntity mCaster;
	private final int mDuration;
	private final double mVelocity;
	private final double mDetectRange;
	private final double mHitboxRadius;
	private final CastAction mCastAction;
	private final IntersectAction mIntersectAction;
	private final Material mBulletMaterial;
	private final int mEmissionSpeed;
	private final int mDelay;
	private final TickAction mTickAction;
	private final int mCooldown;
	private final int mBulletDuration;
	private final Pattern mPattern;
	private final Vector mOffset;
	// Junko Specific
	private int mAccelStart;
	private int mAccelEnd;
	private double mAccel;
	private boolean mPassThrough;
	private double mRotationSpeed;
	private final double ARMOR_STAND_HEAD_OFFSET = 1.6875;
	private final double PLAYER_HITBOX_HEIGHT = 1.8;

	public SpellBullet(Plugin plugin, LivingEntity caster, Vector offset, int duration, int delay, int emissionSpeed, double velocity, double detectRange, double hitboxRadius, int cooldown, int bulletDuration, String pattern,
					   double accel, int accelStart, int accelEnd, boolean passThrough, double rotationSpeed, TickAction tickAction, CastAction castAction, Material bulletMaterial, IntersectAction intersectAction) {
		mPlugin = plugin;
		mEmissionSpeed = emissionSpeed;
		mCaster = caster;
		mDelay = delay;
		mDuration = duration;
		mVelocity = velocity;
		mDetectRange = detectRange;
		mHitboxRadius = hitboxRadius;
		mCastAction = castAction;
		mIntersectAction = intersectAction;
		mBulletMaterial = bulletMaterial;
		mTickAction = tickAction;
		mCooldown = cooldown;
		mBulletDuration = bulletDuration;
		mPattern = Pattern.valueOfLabel(pattern.toLowerCase());
		mAccel = accel;
		mAccelStart = accelStart;
		mAccelEnd = accelEnd;
		mPassThrough = passThrough;
		mRotationSpeed = rotationSpeed;
		mOffset = offset;
	}

	@Override
	public void run() {
		new BukkitRunnable() {
			int mTicks = 0;
			int mCasts = 0;
			double mRotation = 0;
			int mJunkoOffset = (int) (Math.random() * 2);
			double mRandomAngle = Math.random() * Math.PI;
			Location mSanaeLoc = mCaster.getLocation().clone().add(new Vector(7.75, 0, 0).add(mOffset).rotateAroundY(mRandomAngle));
			Location mHardSanaeLoc = mCaster.getLocation().clone().add(new Vector(3.375, 0, 0).add(mOffset).rotateAroundY(mRandomAngle));

			double mSanaeAngle = 162 / 180.0 * Math.PI + mRandomAngle;

			@Override
			public void run() {
				int correctedTicks = mTicks - mDelay;
				if (mCaster == null || mCaster.isDead() || EntityUtils.isStunned(mCaster) || EntityUtils.isSilenced(mCaster)) {
					this.cancel();
					return;
				}
				if (mTicks < mDelay) {
					mTickAction.run(mCaster, mTicks);
				} else if (mTicks < mDuration + mDelay && mTicks % mEmissionSpeed == 0) {
					if (PlayerUtils.playersInRange(mCaster.getLocation(), mDetectRange, false).size() > 0) {
						List<Player> hittablePlayers = PlayerUtils.playersInRange(mCaster.getLocation(), 75, false);
						if (mPattern == Pattern.BORDER || mPattern == Pattern.BORDER_2 || mPattern == Pattern.BORDER_1) {
							launchAcceleratingBullet(hittablePlayers, new Vector(1, 0, 0).rotateAroundY(mRotation), 0, 0, 0);
							if (mPattern == Pattern.BORDER || mPattern == Pattern.BORDER_2) {
								launchAcceleratingBullet(hittablePlayers, new Vector(-1, 0, 0).rotateAroundY(mRotation), 0, 0, 0);
								if (mPattern == Pattern.BORDER) {
									launchAcceleratingBullet(hittablePlayers, new Vector(0, 0, -1).rotateAroundY(mRotation), 0, 0, 0);
									launchAcceleratingBullet(hittablePlayers, new Vector(0, 0, 1).rotateAroundY(mRotation), 0, 0, 0);
								}
							}
							mCasts++;
							mRotation += Math.sin(mCasts / 30.0 * Math.PI);
						} else if (mPattern == Pattern.HARD_BORDER) {
							for (int i = 0; i < 8; i++) {
								launchAcceleratingBullet(hittablePlayers, new Vector(1, 0, 0).rotateAroundY(mRotation + i * Math.PI / 4), 0, 0, 0, true);
							}
							mCasts++;
							mRotation += Math.sin(mCasts / 120.0 * Math.PI);
						} else if (mPattern == Pattern.JUNKO || mPattern == Pattern.HARD_JUNKO) {
							double multiplier = mPattern == Pattern.JUNKO ? 1 : 1.5;
							for (int i = 0; i < 40 * multiplier; i++) {
								launchAcceleratingBullet(hittablePlayers, new Vector(1, 0, 0).rotateAroundY((2 * i + mJunkoOffset) * Math.PI / (40.0 * multiplier)), mAccel, mAccelStart, mAccelEnd, mPattern == Pattern.HARD_JUNKO);
							}
						} else if (mPattern == Pattern.SANAE) {
							int j = mTicks % 10;
							Vector direction = new Vector(1, 0, 0).rotateAroundY(mSanaeAngle + (90 + (j - 3) * 15) / 180.0 * Math.PI);
							mSanaeLoc.add(new Vector(10 / 7.0, 0, 0).rotateAroundY(mSanaeAngle));
							launchAcceleratingBullet(hittablePlayers, mSanaeLoc, mBulletDuration + (50 - mTicks), direction, 0.15, 55 - (mTicks - mDelay), 57 - (mTicks - mDelay));
							if (j == 9) {
								mSanaeAngle -= 216 / 180.0 * Math.PI;
							}
						} else if (mPattern == Pattern.HARD_SANAE) {
							int j = correctedTicks % 3;
							Vector direction = new Vector(1, 0, 0).rotateAroundY(mSanaeAngle + (90 + (2 * j - 3) * 15) / 180.0 * Math.PI);
							mHardSanaeLoc.add(new Vector(8 / 7.0, 0, 0).rotateAroundY(mSanaeAngle));
							for (int i = 0; i < 5; i++) {
								launchParametrizedBullet(hardSanaeRunnable(hittablePlayers, mHardSanaeLoc, new Vector(1, 0, 0).rotateAroundY(mSanaeAngle - i * 216 / 180.0 * Math.PI), direction, correctedTicks, true));
							}
							direction = new Vector(1, 0, 0).rotateAroundY(mSanaeAngle + (90 + (2 * j - 2) * 15) / 180.0 * Math.PI);
							mHardSanaeLoc.add(new Vector(8 / 7.0, 0, 0).rotateAroundY(mSanaeAngle));
							for (int i = 0; i < 5; i++) {
								launchParametrizedBullet(hardSanaeRunnable(hittablePlayers, mHardSanaeLoc, new Vector(1, 0, 0).rotateAroundY(mSanaeAngle - i * 216 / 180.0 * Math.PI), direction, correctedTicks, true));
							}
							if (j == 2) {
								mSanaeAngle -= 216 / 180.0 * Math.PI;
							}
						} else if (mPattern == Pattern.POLYGRAPH) {
							double rotation = mTicks / mRotationSpeed * Math.PI;
							if (mTicks % 10 == 0) {
								for (int i = 0; i < 4; i++) {
									summonMarker(new Vector(1, 0, 0).rotateAroundY(rotation + i * Math.PI / 2.0), mDetectRange);
								}
								List<Player> players = EntityUtils.getNearestPlayers(mCaster.getLocation(), mDetectRange);
								for (Player player : players) {
									double distance = player.getLocation().toVector().setY(0).distance(mCaster.getLocation().toVector().setY(0));
									for (int i = 0; i < 5; i++) {
										launchAcceleratingBullet(hittablePlayers, mCaster.getLocation().add(new Vector(1, 0, 0).add(mOffset).rotateAroundY(rotation + i * Math.PI / 2.5).multiply(distance)), mBulletDuration, new Vector(), 0, 0, 0);
									}
								}
							}
						} else if (mPattern == Pattern.HARD_POLYGRAPH) {
							double rotation = mTicks / mRotationSpeed * Math.PI;
							if (mTicks % 10 == 0) {
								for (int i = 0; i < 6; i++) {
									summonMarker(new Vector(1, 0, 0).rotateAroundY(rotation + i * Math.PI / 3.0), mDetectRange, 0.2);
								}
								List<Player> players = EntityUtils.getNearestPlayers(mCaster.getLocation(), mDetectRange);
								for (Player player : players) {
									double distance = player.getLocation().toVector().setY(0).distance(mCaster.getLocation().toVector().setY(0));
									for (int i = 0; i < 6; i++) {
										launchAcceleratingBullet(hittablePlayers, mCaster.getLocation().add(new Vector(1, 0, 0).rotateAroundY(rotation + i * Math.PI / 3.0).multiply(distance)).add(mOffset), mBulletDuration, new Vector(), 0, 0, 0, true);
									}
								}
							}
						} else if (mPattern == Pattern.NUE && (correctedTicks / 60) % 2 == 0) {
							int even = ((correctedTicks / mEmissionSpeed) % 2 == 1) ? 1 : -1;
							double angleOffset = (correctedTicks / 60) / 8.0 * Math.PI;
							for (int i = 0; i < 4; i++) {
								launchParametrizedBullet(nueRunnable(hittablePlayers, new Vector(1, 0, 0).rotateAroundY(mRandomAngle + angleOffset + i * Math.PI / 2.0), even, correctedTicks, false));
							}
						} else if (mPattern == Pattern.HARD_NUE && (correctedTicks / 60) % 2 == 0) {
							int even = ((correctedTicks / mEmissionSpeed) % 2 == 1) ? 1 : -1;
							double angleOffset = (correctedTicks / 60) / 16.0 * Math.PI;
							for (int i = 0; i < 8; i++) {
								launchParametrizedBullet(nueRunnable(hittablePlayers, new Vector(1, 0, 0).rotateAroundY(mRandomAngle + angleOffset + i * Math.PI / 4.0), even, correctedTicks, true));
							}
						} else if (mPattern == Pattern.FREEZE) {
							for (int i = 0; i < 3; i++) {
								launchParametrizedBullet(freezeRunnable(hittablePlayers, correctedTicks, false));
							}
						} else if (mPattern == Pattern.HARD_FREEZE) {
							if (correctedTicks < 20) {
								for (int i = 0; i < 3; i++) {
									launchParametrizedBullet(freezeRunnable(hittablePlayers, correctedTicks, false));
								}
							} else if (correctedTicks > 40 && correctedTicks <= 50) {
								for (int i = 0; i < 5; i++) {
									launchAcceleratingBullet(hittablePlayers, mCaster.getLocation().add(mOffset), mBulletDuration - correctedTicks, new Vector(0, 0, 1).rotateAroundY(Math.random() * 2 * Math.PI), 0, 0, 0, true);
								}
							}
						} else if (mPattern == Pattern.SLASH) {
							double length = 10;
							double distance = FastUtils.RANDOM.nextDouble(3);
							slash(hittablePlayers, length, distance, 10.0, false);
						} else if (mPattern == Pattern.HARD_SLASH) {
							double length = 10;
							double distance = FastUtils.RANDOM.nextDouble(5);
							slash(hittablePlayers, length, distance, 10.0, true);
						} else if (mPattern == Pattern.MALLET) {
							double granularity = 10;
							double offset = FastUtils.RANDOM.nextDouble(Math.PI);
							if (correctedTicks % 30 == 0) {
								for (int i = 0; i < granularity; i++) {
									launchAcceleratingBullet(hittablePlayers, new Vector(1, 0, 0).rotateAroundY(2 * Math.PI / granularity * i + offset), false, 3);
								}
							}
							double secondGranularity = 2;
							for (int i = 0; i < secondGranularity; i++) {
								launchAcceleratingBullet(hittablePlayers, mCaster.getLocation().clone().add(2, 0, 0), mBulletDuration, new Vector(1, 0, 0).rotateAroundY(2 * Math.PI / secondGranularity * i + correctedTicks * 3 / 30.0), 0, 0, 0, false, 3);
								launchAcceleratingBullet(hittablePlayers, mCaster.getLocation().clone().add(-2, 0, 0), mBulletDuration, new Vector(1, 0, 0).rotateAroundY(2 * Math.PI / secondGranularity * i - correctedTicks * 3 / 30.0), 0, 0, 0, false, 3);
							}
						} else if (mPattern == Pattern.HARD_MALLET) {
							double granularity = 15;
							double offset = FastUtils.RANDOM.nextDouble(Math.PI);
							if (correctedTicks % 30 == 0) {
								for (int i = 0; i < granularity; i++) {
									launchAcceleratingBullet(hittablePlayers, new Vector(1, 0, 0).rotateAroundY(2 * Math.PI / granularity * i + offset), true, 6);
								}
							}
							double secondGranularity = 3;
							for (int i = 0; i < secondGranularity; i++) {
								launchAcceleratingBullet(hittablePlayers, mCaster.getLocation().clone().add(2, 0, 0), mBulletDuration, new Vector(1, 0, 0).rotateAroundY(2 * Math.PI / secondGranularity * i + correctedTicks * 3 / 30.0), 0, 0, 0, false, 6);
								launchAcceleratingBullet(hittablePlayers, mCaster.getLocation().clone().add(-2, 0, 0), mBulletDuration, new Vector(1, 0, 0).rotateAroundY(2 * Math.PI / secondGranularity * i - correctedTicks * 3 / 30.0), 0, 0, 0, false, 6);
							}
						} else if (mPattern == Pattern.TRAPPER) {
							double granularity = 5;
							List<Player> players = EntityUtils.getNearestPlayers(mCaster.getLocation(), mDetectRange);
							for (Player player : players) {
								for (int i = 0; i < granularity; i++) {
									Location playerLoc = player.getLocation();
									playerLoc.setY(mCaster.getLocation().getY());
									launchParametrizedBullet(trapperRunnable(hittablePlayers, player, playerLoc.add(new Vector(3, 0, 0).rotateAroundY(2 * Math.PI / granularity * i)), 30, 0, false));
								}
							}
						} else if (mPattern == Pattern.HARD_TRAPPER) {
							double granularity = 10;
							List<Player> players = EntityUtils.getNearestPlayers(mCaster.getLocation(), mDetectRange);
							for (Player player : players) {
								for (int i = 0; i < granularity; i++) {
									Location playerLoc = player.getLocation();
									playerLoc.setY(mCaster.getLocation().getY());
									launchParametrizedBullet(trapperRunnable(hittablePlayers, player, playerLoc.add(new Vector(3, 0, 0).rotateAroundY(2 * Math.PI / granularity * i)), 20 + (i % 2) * 20, 0, true));
								}
							}
						} else if (mPattern == Pattern.INVALID) {
							boolean small = true;
							launchAcceleratingBullet(hittablePlayers, mCaster.getLocation().clone().add(0, 3, 0), 5000, new Vector(), 0, 0, 0, small);
							/*
							// HITBOX TESTING
							//launchAcceleratingBullet(mCaster.getLocation().clone().add(0, 3, 0), 5000, new Vector(), 0, 0, 0);
							double radius = mHitboxRadius / (small ? 2 : 1);
							Location particleCenter = mCaster.getLocation().clone().add(0, 3, 0);
							mCaster.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleCenter.clone().add(radius, radius, radius), 1, 0, 0, 0, 0);
							mCaster.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleCenter.clone().add(-radius, radius, radius), 1, 0, 0, 0, 0);
							mCaster.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleCenter.clone().add(-radius, -radius, radius), 1, 0, 0, 0, 0);
							mCaster.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleCenter.clone().add(-radius, -radius, -radius), 1, 0, 0, 0, 0);
							mCaster.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleCenter.clone().add(-radius, radius, -radius), 1, 0, 0, 0, 0);
							mCaster.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleCenter.clone().add(radius, -radius, radius), 1, 0, 0, 0, 0);
							mCaster.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleCenter.clone().add(radius, -radius, -radius), 1, 0, 0, 0, 0);
							mCaster.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleCenter.clone().add(radius, radius, -radius), 1, 0, 0, 0, 0);
							*/
						}
					}
				} else if (mTicks >= mDuration + mDelay) {
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void summonMarker(Vector dir, double length) {
		summonMarker(dir, length, 0);
	}

	private void summonMarker(Vector dir, double length, double yoffset) {
		new PPLine(Particle.DRAGON_BREATH, mCaster.getLocation().add(0, yoffset, 0), dir, length).countPerMeter(10).spawnAsEnemy();
	}

	private void slash(List<Player> players, double length, double distance, double granularity, boolean small) {
		double rotationAngle = FastUtils.RANDOM.nextDouble(2 * Math.PI);
		Vector slashDirection = new Vector(1, 0, 0).rotateAroundY(rotationAngle);
		Location spawnLoc = mCaster.getLocation().add(new Vector(-length / 2.0, 0, distance).rotateAroundY(rotationAngle));
		new PPLine(Particle.CRIT, spawnLoc.clone().add(0, 0.2, 0), slashDirection, length).countPerMeter(10).spawnAsEnemy();
		for (int i = 0; i < granularity; i++) {
			Location detLoc = spawnLoc.clone().add(slashDirection.clone().multiply(length / granularity * i));
			int mI = i;
			new BukkitRunnable() {
				@Override
				public void run() {
					launchAcceleratingBullet(players, detLoc, mBulletDuration, new Vector(0, 0, 1).rotateAroundY(rotationAngle + Math.PI / (4.0 * (granularity - mI + 1))), 0, 0, 0, small);
					launchAcceleratingBullet(players, detLoc, mBulletDuration, new Vector(0, 0, -1).rotateAroundY(rotationAngle - Math.PI / (4.0 * (granularity - mI + 1))), 0, 0, 0, small);
				}
			}
			.runTaskLater(mPlugin, 10 + i);
		}
	}

	private BukkitRunnable trapperRunnable(List<Player> players, Player target, Location detLoc, int waitTime, int offsetTicks, boolean small) {

		ArmorStand bullet = spawnBullet(detLoc, small);
		double hitboxRadius = mHitboxRadius / (small ? 2 : 1);
		double mInnerVelocity = mVelocity;
		return new BukkitRunnable() {
			BoundingBox mBox = BoundingBox.of(detLoc, hitboxRadius, hitboxRadius, hitboxRadius);
			int mTicks = 0;

			Vector mDir = new Vector();

			@Override
			public void run() {
				// Iterate two times and half the velocity so that way we can have more accurate travel for intersection.
				if (mTicks + offsetTicks == waitTime) {
					mDir = target.getLocation().toVector().subtract(bullet.getLocation().toVector());
					mDir.setY(0);
					mDir = mDir.normalize();
				}
				checkForCollisions(mBox, mInnerVelocity, mDir, bullet, players, this);
				Location loc = mBox.getCenter().toLocation(mCaster.getWorld());
				mTicks++;
				bullet.teleport(loc.clone().add(0, -ARMOR_STAND_HEAD_OFFSET / (small ? 2 : 1), 0));
				checkIfBulletExpired(mTicks + offsetTicks, mBulletDuration, bullet, this);
			}
		};
	}

	private BukkitRunnable hardSanaeRunnable(List<Player> players, Location detLoc, Vector initialDir, Vector collapseDir, int offsetTicks, boolean small) {

		ArmorStand bullet = spawnBullet(detLoc, small);
		double hitboxRadius = mHitboxRadius / (small ? 2 : 1);
		return new BukkitRunnable() {
			BoundingBox mBox = BoundingBox.of(detLoc, hitboxRadius, hitboxRadius, hitboxRadius);
			int mTicks = 0;
			double mInnerVelocity = mVelocity;

			Vector mDir = new Vector();

			@Override
			public void run() {
				// Iterate two times and half the velocity so that way we can have more accurate travel for intersection.
				if (mTicks + offsetTicks == 25) {
					mDir = initialDir;
					mInnerVelocity = mVelocity * 1.25;
				}
				if (mTicks + offsetTicks == 40) {
					mDir = new Vector();
					mInnerVelocity = mVelocity;
				}
				if (mTicks + offsetTicks == 50) {
					mDir = collapseDir;
				}
				checkForCollisions(mBox, mInnerVelocity, mDir, bullet, players, this);
				Location loc = mBox.getCenter().toLocation(mCaster.getWorld());
				mTicks++;
				bullet.teleport(loc.clone().add(0, -ARMOR_STAND_HEAD_OFFSET / (small ? 2 : 1), 0));
				checkIfBulletExpired(mTicks + offsetTicks, mBulletDuration, bullet, this);
			}
		};
	}


	private BukkitRunnable nueRunnable(List<Player> players, Vector initialDir, int even, int offsetTicks, boolean small) {
		Location detLoc = mCaster.getLocation().clone().add(mOffset);

		ArmorStand bullet = spawnBullet(detLoc, small);
		double hitboxRadius = mHitboxRadius / (small ? 2 : 1);
		return new BukkitRunnable() {
			BoundingBox mBox = BoundingBox.of(detLoc, hitboxRadius, hitboxRadius, hitboxRadius);
			int mTicks = 0;
			double mInnerVelocity = mVelocity;

			Vector mDir = initialDir;

			@Override
			public void run() {
				// Iterate two times and half the velocity so that way we can have more accurate travel for intersection.
				if (((mTicks + offsetTicks) / 60) % 2 == 1) {
					mDir = new Vector(bullet.getLocation().getZ() - mCaster.getLocation().getZ(), 0, mCaster.getLocation().getX() - bullet.getLocation().getX());
					mDir = mDir.multiply(even);
					mInnerVelocity = Math.PI / 60.0;
				} else {
					int shift = (((mTicks + offsetTicks) / 60) % 4 == 0) ? 1 : -1;
					mDir = initialDir.clone().multiply(shift);
					mInnerVelocity = mVelocity;
				}
				checkForCollisions(mBox, mInnerVelocity, mDir, bullet, players, this);
				Location loc = mBox.getCenter().toLocation(mCaster.getWorld());
				mTicks++;
				bullet.teleport(loc.clone().add(0, -ARMOR_STAND_HEAD_OFFSET / (small ? 2 : 1), 0));
				checkIfBulletExpired(mTicks, mBulletDuration, bullet, this);
			}
		};
	}

	private BukkitRunnable freezeRunnable(List<Player> players, int offsetTicks, boolean small) {
		Location detLoc = mCaster.getLocation().clone().add(mOffset);

		ArmorStand bullet = spawnBullet(detLoc, small);
		double hitboxRadius = mHitboxRadius / (small ? 2 : 1);
		return new BukkitRunnable() {
			BoundingBox mBox = BoundingBox.of(detLoc, hitboxRadius, hitboxRadius, hitboxRadius);
			int mTicks = 0;
			double mInnerVelocity = mVelocity;

			Vector mDir = new Vector(0, 0, 1).rotateAroundY(Math.random() * 2 * Math.PI);

			@Override
			public void run() {
				// Iterate two times and half the velocity so that way we can have more accurate travel for intersection.
				if (mTicks + offsetTicks == 50) {
					mDir = new Vector(0, 0, 1).rotateAroundY(Math.random() * 2 * Math.PI);
					mInnerVelocity = 0.05;
				}
				if (mTicks + offsetTicks == 30) {
					mDir = new Vector(0, 0, 0);
				}
				if (mTicks + offsetTicks >= 50 && mTicks + offsetTicks <= 54) {
					mInnerVelocity += 0.025;
				}
				checkForCollisions(mBox, mInnerVelocity, mDir, bullet, players, this);
				Location loc = mBox.getCenter().toLocation(mCaster.getWorld());
				mTicks++;
				bullet.teleport(loc.clone().add(0, -ARMOR_STAND_HEAD_OFFSET / (small ? 2 : 1), 0));
				checkIfBulletExpired(mTicks + offsetTicks, mBulletDuration, bullet, this);
			}
		};
	}

	private void launchParametrizedBullet(BukkitRunnable runnable) {
		mCastAction.run(mCaster);

		runnable.runTaskTimer(mPlugin, 0, 1);
	}

	private void launchAcceleratingBullet(List<Player> players, Vector dir, boolean small, int playerHitboxSize) {
		launchAcceleratingBullet(players, mCaster.getLocation().clone().add(mOffset), mBulletDuration, dir, 0, 0, 0, small, playerHitboxSize);
	}

	private void launchAcceleratingBullet(List<Player> players, Vector dir, double accel, int accelStart, int accelEnd) {
		launchAcceleratingBullet(players, mCaster.getLocation().clone().add(mOffset), mBulletDuration, dir, accel, accelStart, accelEnd);
	}

	private void launchAcceleratingBullet(List<Player> players, Vector dir, double accel, int accelStart, int accelEnd, boolean small) {
		launchAcceleratingBullet(players, mCaster.getLocation().clone().add(mOffset), mBulletDuration, dir, accel, accelStart, accelEnd, small);
	}

	private void launchAcceleratingBullet(List<Player> players, Location detLoc, int bulletDuration, Vector dir, double accel, int accelStart, int accelEnd) {
		launchAcceleratingBullet(players, detLoc, bulletDuration, dir, accel, accelStart, accelEnd, false);
	}

	private void launchAcceleratingBullet(List<Player> players, Location detLoc, int bulletDuration, Vector dir, double accel, int accelStart, int accelEnd, boolean small) {
		launchAcceleratingBullet(players, detLoc, bulletDuration, dir, accel, accelStart, accelEnd, small, 1);
	}

	private void launchAcceleratingBullet(List<Player> players, Location detLoc, int bulletDuration, Vector dir, double accel, int accelStart, int accelEnd, boolean small, int playerHitboxSize) {
		mCastAction.run(mCaster);

		ArmorStand bullet = spawnBullet(detLoc, small);
		double hitboxRadius = mHitboxRadius / (small ? 2 : 1);

		new BukkitRunnable() {
			BoundingBox mBox = BoundingBox.of(detLoc, hitboxRadius, hitboxRadius, hitboxRadius);
			int mTicks = 0;
			double mInnerVelocity = mVelocity;

			@Override
			public void run() {
				checkForCollisions(mBox, mInnerVelocity, dir, bullet, players, this, playerHitboxSize);
				Location loc = mBox.getCenter().toLocation(mCaster.getWorld());
				mTicks++;
				bullet.teleport(loc.clone().add(0, -ARMOR_STAND_HEAD_OFFSET / (small ? 2 : 1), 0));
				checkIfBulletExpired(mTicks, bulletDuration, bullet, this);
				if (mTicks >= accelStart && mTicks < accelEnd) {
					mInnerVelocity += accel;
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	public ArmorStand spawnBullet(Location detLoc, boolean small) {
		ArmorStand bullet = mCaster.getWorld().spawn(detLoc.clone().add(0, -ARMOR_STAND_HEAD_OFFSET / (small ? 2 : 1), 0), ArmorStand.class);
		bullet.setSmall(small);
		bullet.setVisible(false);
		bullet.setGravity(false);
		bullet.setMarker(true);
		bullet.setCollidable(false);
		bullet.getEquipment().setHelmet(new ItemStack(mBulletMaterial));
		return bullet;
	}

	public void checkForCollisions(BoundingBox box, double velocity, Vector dir, ArmorStand bullet, List<Player> players, BukkitRunnable runnable, int playerHitboxSize) {
		for (int j = 0; j < 2; j++) {
			Location prevLoc = box.getCenter().toLocation(mCaster.getWorld());
			box.shift(dir.clone().multiply(velocity * 0.5));
			Location loc = box.getCenter().toLocation(mCaster.getWorld());
			for (Player player : players) {
				BoundingBox playerBox = BoundingBox.of(player.getLocation().clone().add(0, PLAYER_HITBOX_HEIGHT / 2, 0), playerHitboxSize * 0.125, PLAYER_HITBOX_HEIGHT / 2, playerHitboxSize * 0.125);
				if (playerBox.overlaps(box)) {
					mIntersectAction.run(player, loc, false, prevLoc);
					bullet.remove();
					runnable.cancel();
					return;
				}
			}
			if (loc.getBlock().getType().isSolid() && !mPassThrough) {
				bullet.remove();
				mIntersectAction.run(null, loc, true, prevLoc);
				runnable.cancel();
				return;
			}
		}
	}

	public void checkForCollisions(BoundingBox box, double velocity, Vector dir, ArmorStand bullet, List<Player> players, BukkitRunnable runnable) {
		checkForCollisions(box, velocity, dir, bullet, players, runnable, 1);
	}

	public void checkIfBulletExpired(int totalTicks, int bulletDuration, ArmorStand bullet, BukkitRunnable runnable) {
		if (totalTicks >= bulletDuration || mCaster.isDead()) {
			bullet.remove();
			runnable.cancel();
		}
	}

	/* If there are players in range of the attack, put it on cooldown. Otherwise, skip and move on*/
	@Override
	public int cooldownTicks() {
		if (!PlayerUtils.playersInRange(mCaster.getLocation(), mDetectRange, true).isEmpty()) {
			if (mPattern == Pattern.SANAE) {
				return mCooldown + mDuration + mDelay + mBulletDuration;
			}
			return mCooldown + mDuration + mDelay;
		} else {
			return 1;
		}
	}

}
