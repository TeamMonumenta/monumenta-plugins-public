package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import javax.annotation.Nullable;
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

public class SpellBullet extends Spell {

	public enum Pattern {
		BORDER("border"),
		BORDER_2("border_2"),
		BORDER_1("border_1"),
		JUNKO("junko"),
		SANAE("sanae"),
		POLYGRAPH("polygraph"),
		FREEZE("freeze"),
		NUE("nue"),
		SUWAKO("suwako"),
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
		void run(@Nullable Player player, Location loc, boolean blocked);
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
			double mRandomAngle = Math.random() * 3.14;
			Location mSanaeLoc = mCaster.getLocation().clone().add(new Vector(7.75, 0, 0).add(mOffset).rotateAroundY(mRandomAngle));
			double mSanaeAngle = 162 / 180.0 * 3.14 + mRandomAngle;

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
						if (mPattern == Pattern.BORDER || mPattern == Pattern.BORDER_2 || mPattern == Pattern.BORDER_1) {
							launchAcceleratingBullet(new Vector(1, 0, 0).rotateAroundY(mRotation), 0, 0, 0);
							if (mPattern == Pattern.BORDER || mPattern == Pattern.BORDER_2) {
								launchAcceleratingBullet(new Vector(-1, 0, 0).rotateAroundY(mRotation), 0, 0, 0);
								if (mPattern == Pattern.BORDER) {
									launchAcceleratingBullet(new Vector(0, 0, -1).rotateAroundY(mRotation), 0, 0, 0);
									launchAcceleratingBullet(new Vector(0, 0, 1).rotateAroundY(mRotation), 0, 0, 0);
								}
							}
							mCasts++;
							mRotation += Math.sin(mCasts / 30.0 * 3.14);
						} else if (mPattern == Pattern.JUNKO) {
							for (int i = 0; i < 40; i++) {
								launchAcceleratingBullet(new Vector(1, 0, 0).rotateAroundY((2 * i + mJunkoOffset) * 3.14 / 40.0), mAccel, mAccelStart, mAccelEnd);
							}
						} else if (mPattern == Pattern.SANAE) {
							int j = mTicks % 10;
							Vector direction = new Vector(1, 0, 0).rotateAroundY(mSanaeAngle + (90 + (j - 3) * 15) / 180.0 * 3.14);
							mSanaeLoc.add(new Vector(10 / 7.0, 0, 0).rotateAroundY(mSanaeAngle));
							launchAcceleratingBullet(mSanaeLoc, mBulletDuration + (50 - mTicks), direction, 0.15, 55 - (mTicks - mDelay), 57 - (mTicks - mDelay));
							if (j == 9) {
								mSanaeAngle -= 216 / 180.0 * 3.14;
							}
						} else if (mPattern == Pattern.POLYGRAPH) {
							double rotation = mTicks / mRotationSpeed * 3.14;
							if (mTicks % 10 == 0) {
								summonMarker(new Vector(1, 0, 0).rotateAroundY(rotation), mDetectRange);
								summonMarker(new Vector(-1, 0, 0).rotateAroundY(rotation), mDetectRange);
								summonMarker(new Vector(0, 0, -1).rotateAroundY(rotation), mDetectRange);
								summonMarker(new Vector(0, 0, 1).rotateAroundY(rotation), mDetectRange);
							}
							if (mTicks % 10 == 0) {
								List<Player> players = EntityUtils.getNearestPlayers(mCaster.getLocation(), mDetectRange);
								for (Player player : players) {
									double distance = player.getLocation().toVector().setY(0).distance(mCaster.getLocation().toVector().setY(0));
									launchAcceleratingBullet(mCaster.getLocation().add(new Vector(1, 0, 0).add(mOffset).rotateAroundY(rotation).multiply(distance)), mBulletDuration, new Vector(), 0, 0, 0);
									launchAcceleratingBullet(mCaster.getLocation().add(new Vector(-1, 0, 0).add(mOffset).rotateAroundY(rotation).multiply(distance)), mBulletDuration, new Vector(), 0, 0, 0);
									launchAcceleratingBullet(mCaster.getLocation().add(new Vector(0, 0, 1).add(mOffset).rotateAroundY(rotation).multiply(distance)), mBulletDuration, new Vector(), 0, 0, 0);
									launchAcceleratingBullet(mCaster.getLocation().add(new Vector(0, 0, -1).add(mOffset).rotateAroundY(rotation).multiply(distance)), mBulletDuration, new Vector(), 0, 0, 0);
								}
							}
						} else if (mPattern == Pattern.NUE && (correctedTicks / 60) % 2 == 0) {
							int even = ((correctedTicks / mEmissionSpeed) % 2 == 1) ? 1 : -1;
							double angleOffset = (correctedTicks / 60) / 8.0 * 3.14;
							launchParametrizedBullet(nueRunnable(new Vector(1, 0, 0).rotateAroundY(mRandomAngle + angleOffset), even, correctedTicks));
							launchParametrizedBullet(nueRunnable(new Vector(-1, 0, 0).rotateAroundY(mRandomAngle + angleOffset), even, correctedTicks));
							launchParametrizedBullet(nueRunnable(new Vector(0, 0, 1).rotateAroundY(mRandomAngle + angleOffset), even, correctedTicks));
							launchParametrizedBullet(nueRunnable(new Vector(0, 0, -1).rotateAroundY(mRandomAngle + angleOffset), even, correctedTicks));
						} else if (mPattern == Pattern.FREEZE) {
							launchParametrizedBullet(freezeRunnable(correctedTicks));
							launchParametrizedBullet(freezeRunnable(correctedTicks));
							launchParametrizedBullet(freezeRunnable(correctedTicks));
						} else if (mPattern == Pattern.INVALID) {
							launchAcceleratingBullet(mCaster.getLocation().clone().add(0, 3, 0), 5000, new Vector(), 0, 0, 0);
							/* HITBOX TESTING
							launchAcceleratingBullet(mCaster.getLocation().clone().add(0, 3, 0), 5000, new Vector(), 0, 0, 0);
							Location particleCenter = mCaster.getLocation().clone().add(0, 3, 0);
							mCaster.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleCenter.clone().add(mHitboxRadius, mHitboxRadius, mHitboxRadius), 1, 0, 0, 0, 0);
							mCaster.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleCenter.clone().add(-mHitboxRadius, mHitboxRadius, mHitboxRadius), 1, 0, 0, 0, 0);
							mCaster.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleCenter.clone().add(-mHitboxRadius, -mHitboxRadius, mHitboxRadius), 1, 0, 0, 0, 0);
							mCaster.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleCenter.clone().add(-mHitboxRadius, -mHitboxRadius, -mHitboxRadius), 1, 0, 0, 0, 0);
							mCaster.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleCenter.clone().add(-mHitboxRadius, mHitboxRadius, -mHitboxRadius), 1, 0, 0, 0, 0);
							mCaster.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleCenter.clone().add(mHitboxRadius, -mHitboxRadius, mHitboxRadius), 1, 0, 0, 0, 0);
							mCaster.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleCenter.clone().add(mHitboxRadius, -mHitboxRadius, -mHitboxRadius), 1, 0, 0, 0, 0);
							mCaster.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleCenter.clone().add(mHitboxRadius, mHitboxRadius, -mHitboxRadius), 1, 0, 0, 0, 0);
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
		new PPLine(Particle.DRAGON_BREATH, mCaster.getLocation(), dir, length).countPerMeter(10).spawnAsBoss();
	}

	private BukkitRunnable nueRunnable(Vector initialDir, int even, int offsetTicks) {
		Location detLoc = mCaster.getLocation().clone().add(mOffset);

		List<Player> players = PlayerUtils.playersInRange(detLoc, 75, false);

		ArmorStand bullet = mCaster.getWorld().spawn(detLoc.clone().add(0, -ARMOR_STAND_HEAD_OFFSET, 0), ArmorStand.class);
		bullet.setVisible(false);
		bullet.setGravity(false);
		bullet.setMarker(true);
		bullet.setCollidable(false);
		bullet.getEquipment().setHelmet(new ItemStack(mBulletMaterial));
		return new BukkitRunnable() {
			BoundingBox mBox = BoundingBox.of(detLoc, mHitboxRadius, mHitboxRadius, mHitboxRadius);
			int mTicks = 0;
			double mInnerVelocity = mVelocity;

			Vector mDir = initialDir;

			@Override
			public void run() {
				// Iterate two times and half the velocity so that way we can have more accurate travel for intersection.
				if (((mTicks + offsetTicks) / 60) % 2 == 1) {
					mDir = new Vector(bullet.getLocation().getZ() - mCaster.getLocation().getZ(), 0, mCaster.getLocation().getX() - bullet.getLocation().getX());
					mDir = mDir.multiply(even);
					mInnerVelocity = 3.14 / 60.0;
				} else {
					int shift = (((mTicks + offsetTicks) / 60) % 4 == 0) ? 1 : -1;
					mDir = initialDir.clone().multiply(shift);
					mInnerVelocity = mVelocity;
				}
				for (int j = 0; j < 2; j++) {
					mBox.shift(mDir.clone().multiply(mInnerVelocity * 0.5));
					Location loc = mBox.getCenter().toLocation(mCaster.getWorld());
					for (Player player : players) {
						if (player.getBoundingBox().overlaps(mBox)) {
							mIntersectAction.run(player, loc, false);
							bullet.remove();
							this.cancel();
							return;
						}
					}
					if (loc.getBlock().getType().isSolid() && !mPassThrough) {
						bullet.remove();
						this.cancel();
						mIntersectAction.run(null, loc, true);
					}
				}
				Location loc = mBox.getCenter().toLocation(mCaster.getWorld());
				mTicks++;
				bullet.teleport(loc.clone().add(0, -ARMOR_STAND_HEAD_OFFSET, 0));
				if (mTicks >= mBulletDuration || mCaster == null || mCaster.isDead()) {
					bullet.remove();
					this.cancel();
				}
			}
		};
	}

	private BukkitRunnable freezeRunnable(int offsetTicks) {
		Location detLoc = mCaster.getLocation().clone().add(mOffset);

		List<Player> players = PlayerUtils.playersInRange(detLoc, 75, false);

		ArmorStand bullet = mCaster.getWorld().spawn(detLoc.clone().add(0, -ARMOR_STAND_HEAD_OFFSET, 0), ArmorStand.class);
		bullet.setVisible(false);
		bullet.setGravity(false);
		bullet.setMarker(true);
		bullet.setCollidable(false);
		bullet.getEquipment().setHelmet(new ItemStack(mBulletMaterial));
		return new BukkitRunnable() {
			BoundingBox mBox = BoundingBox.of(detLoc, mHitboxRadius, mHitboxRadius, mHitboxRadius);
			int mTicks = 0;
			double mInnerVelocity = mVelocity;

			Vector mDir = new Vector(0, 0, 1).rotateAroundY(Math.random() * 2 * 3.14);

			@Override
			public void run() {
				// Iterate two times and half the velocity so that way we can have more accurate travel for intersection.
				if (mTicks + offsetTicks == 50) {
					mDir = new Vector(0, 0, 1).rotateAroundY(Math.random() * 2 * 3.14);
					mInnerVelocity = 0.05;
				}
				if (mTicks + offsetTicks == 30) {
					mDir = new Vector(0, 0, 0);
				}
				if (mTicks + offsetTicks >= 50 && mTicks + offsetTicks <= 54) {
					mInnerVelocity += 0.025;
				}
				for (int j = 0; j < 2; j++) {
					mBox.shift(mDir.clone().multiply(mInnerVelocity * 0.5));
					Location loc = mBox.getCenter().toLocation(mCaster.getWorld());
					for (Player player : players) {
						if (player.getBoundingBox().overlaps(mBox)) {
							mIntersectAction.run(player, loc, false);
							bullet.remove();
							this.cancel();
							return;
						}
					}
					if (loc.getBlock().getType().isSolid() && !mPassThrough) {
						bullet.remove();
						this.cancel();
						mIntersectAction.run(null, loc, true);
					}
				}
				Location loc = mBox.getCenter().toLocation(mCaster.getWorld());
				mTicks++;
				bullet.teleport(loc.clone().add(0, -ARMOR_STAND_HEAD_OFFSET, 0));
				if (mTicks + offsetTicks >= mBulletDuration || mCaster == null || mCaster.isDead()) {
					bullet.remove();
					this.cancel();
				}
			}
		};
	}

	private void launchParametrizedBullet(BukkitRunnable runnable) {
		mCastAction.run(mCaster);

		runnable.runTaskTimer(mPlugin, 0, 1);
	}

	private void launchAcceleratingBullet(Vector dir, double accel, int accelStart, int accelEnd) {
		launchAcceleratingBullet(mCaster.getLocation().clone().add(mOffset), mBulletDuration, dir, accel, accelStart, accelEnd);
	}

	private void launchAcceleratingBullet(Location detLoc, int bulletDuration, Vector dir, double accel, int accelStart, int accelEnd) {
		mCastAction.run(mCaster);

		List<Player> players = PlayerUtils.playersInRange(detLoc, 75, false);

		ArmorStand bullet = mCaster.getWorld().spawn(detLoc.clone().add(0, -ARMOR_STAND_HEAD_OFFSET, 0), ArmorStand.class);
		bullet.setVisible(false);
		bullet.setGravity(false);
		bullet.setMarker(true);
		bullet.setCollidable(false);
		bullet.getEquipment().setHelmet(new ItemStack(mBulletMaterial));

		new BukkitRunnable() {
			BoundingBox mBox = BoundingBox.of(detLoc, mHitboxRadius, mHitboxRadius, mHitboxRadius);
			int mTicks = 0;
			double mInnerVelocity = mVelocity;

			@Override
			public void run() {
				// Iterate two times and half the velocity so that way we can have more accurate travel for intersection.
				for (int j = 0; j < 2; j++) {
					mBox.shift(dir.clone().multiply(mInnerVelocity * 0.5));
					Location loc = mBox.getCenter().toLocation(mCaster.getWorld());
					for (Player player : players) {
						if (player.getBoundingBox().overlaps(mBox)) {
							mIntersectAction.run(player, loc, false);
							bullet.remove();
							this.cancel();
							return;
						}
					}
					if (loc.getBlock().getType().isSolid() && !mPassThrough) {
						bullet.remove();
						this.cancel();
						mIntersectAction.run(null, loc, true);
					}
				}
				Location loc = mBox.getCenter().toLocation(mCaster.getWorld());
				mTicks++;
				bullet.teleport(loc.clone().add(0, -ARMOR_STAND_HEAD_OFFSET, 0));
				if (mTicks >= bulletDuration || mCaster == null || mCaster.isDead()) {
					bullet.remove();
					this.cancel();
				}
				if (mTicks >= accelStart && mTicks < accelEnd) {
					mInnerVelocity += accel;
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	/* If there are players in range of the attack, put it on cooldown. Otherwise, skip and move on*/
	@Override
	public int cooldownTicks() {
		if (PlayerUtils.playersInRange(mCaster.getLocation(), mDetectRange, true).size() > 0) {
			if (mPattern == Pattern.SANAE) {
				return mCooldown + mDuration + mDelay + mBulletDuration;
			}
			return mCooldown + mDuration + mDelay;
		} else {
			return 1;
		}
	}

}
