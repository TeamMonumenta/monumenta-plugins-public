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
		JUNKO("junko"),
		SANAE("sanae"),
		POLYGRAPH("polygraph"),
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
	// Junko Specific
	private int mAccelStart;
	private int mAccelEnd;
	private double mAccel;
	private boolean mPassThrough;
	private double mRotationSpeed;

	public SpellBullet(Plugin plugin, LivingEntity caster, int duration, int delay, int emissionSpeed, double velocity, double detectRange, double hitboxRadius, int cooldown, int bulletDuration, String pattern,
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
	}

	@Override
	public void run() {
		new BukkitRunnable() {
			int mTicks = 0;
			int mCasts = 0;
			double mRotation = 0;
			int mJunkoOffset = (int)(Math.random() * 2);
			double mRandomAngle = Math.random() * 3.14;
			Location mSanaeLoc = mCaster.getLocation().clone().add(new Vector(7.75, 0, 0).rotateAroundY(mRandomAngle));
			double mSanaeAngle = 162 / 180.0 * 3.14 + mRandomAngle;

			@Override
			public void run() {
				if (mCaster == null || mCaster.isDead() || EntityUtils.isStunned(mCaster) || EntityUtils.isSilenced(mCaster)) {
					this.cancel();
					return;
				}
				if (mTicks < mDelay) {
					mTickAction.run(mCaster, mTicks);
				} else if (mTicks < mDuration + mDelay && mTicks % mEmissionSpeed == 0) {
					if (PlayerUtils.playersInRange(mCaster.getLocation(), mDetectRange, false).size() > 0) {
						if (mPattern == Pattern.BORDER) {
							launchAcceleratingBullet(new Vector(1, 0, 0).rotateAroundY(mRotation), 0, 0, 0);
							launchAcceleratingBullet(new Vector(-1, 0, 0).rotateAroundY(mRotation), 0, 0, 0);
							launchAcceleratingBullet(new Vector(0, 0, -1).rotateAroundY(mRotation), 0, 0, 0);
							launchAcceleratingBullet(new Vector(0, 0, 1).rotateAroundY(mRotation), 0, 0, 0);
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
									launchAcceleratingBullet(mCaster.getLocation().add(new Vector(1, 0, 0).rotateAroundY(rotation).multiply(distance)), mBulletDuration, new Vector(), 0, 0, 0);
									launchAcceleratingBullet(mCaster.getLocation().add(new Vector(-1, 0, 0).rotateAroundY(rotation).multiply(distance)), mBulletDuration, new Vector(), 0, 0, 0);
									launchAcceleratingBullet(mCaster.getLocation().add(new Vector(0, 0, 1).rotateAroundY(rotation).multiply(distance)), mBulletDuration, new Vector(), 0, 0, 0);
									launchAcceleratingBullet(mCaster.getLocation().add(new Vector(0, 0, -1).rotateAroundY(rotation).multiply(distance)), mBulletDuration, new Vector(), 0, 0, 0);
								}
							}
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

	private void launchAcceleratingBullet(Vector dir, double accel, int accelStart, int accelEnd) {
		launchAcceleratingBullet(mCaster.getLocation(), mBulletDuration, dir, accel, accelStart, accelEnd);
	}

	private void launchAcceleratingBullet(Location detLoc, int bulletDuration, Vector dir, double accel, int accelStart, int accelEnd) {
		mCastAction.run(mCaster);

		List<Player> players = PlayerUtils.playersInRange(detLoc, 75, false);

		ArmorStand bullet = mCaster.getWorld().spawn(detLoc.clone().add(0, -1.5, 0), ArmorStand.class);
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
				bullet.teleport(loc.clone().add(0, -1.5, 0));
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
