package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.hunts.bosses.SteelWingHawk;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.NmsUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class PassivePhantomControl extends Spell {
	//Time between swapping points
	public static final int COOLDOWN = 6 * 20;
	//should be even at all times
	private static final int POINT_COUNT = 24;
	private static final int RADIUS = 20;
	//how fast it moves
	private static final float DISTANCE_PER_TICK = 0.3f;
	//ticks of rotation should be a multiple of 2
	private static final int ROTATION_TICKS = 24;

	private final Entity mBoss;
	private final Plugin mPlugin;
	private final SteelWingHawk mHawk;

	private final List<Location> mPoints = new ArrayList<>();
	public final double mMaxHeight;

	private int mTicks = 0;
	private int mCooldown = COOLDOWN;
	private int mRotationTicks = ROTATION_TICKS;
	private @Nullable Vector mVec = null;
	private float mYawDifference = 0.0f;
	private float mPitchDifference = 0.0f;
	private boolean mRotateFirst = false;

	public float mYaw;
	public float mPitch;

	public PassivePhantomControl(LivingEntity boss, Plugin plugin, SteelWingHawk hawk) {
		mBoss = boss;
		mMaxHeight = hawk.mSpawnLoc.getY() + 20;
		mPlugin = plugin;
		mHawk = hawk;
		for (int i = 0; i < POINT_COUNT; i++) {
			addPoint(i, mMaxHeight, RADIUS);
			addPoint(i, mMaxHeight + 5, RADIUS - 5);
		}
		setRandomPoint();
	}

	private void addPoint(int i, double height, double radius) {
		mPoints.add(new Location(mBoss.getWorld(), mHawk.mSpawnLoc.getX() + radius * FastUtils.cos((i * Math.PI) / (POINT_COUNT / 2)), height, mHawk.mSpawnLoc.getZ() + radius * FastUtils.sin((i * Math.PI) / (POINT_COUNT / 2))));
	}

	@Override
	public void run() {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (!mBoss.isValid()) {
					this.cancel();
					return;
				}

				int cooldown = mCooldown;
				if (mRotateFirst) {
					mCooldown += mRotationTicks;
				}
				if (mTicks >= cooldown) {
					setRandomPoint();
					//little trick to tell players when its turning.
					mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_PHANTOM_FLAP, SoundCategory.HOSTILE, 5f, 1f);
				}

				if (mVec != null) {
					boolean rotate = mTicks < mRotationTicks;
					if (rotate) {
						mYaw += mYawDifference;
						mPitch += mPitchDifference;
						mBoss.setRotation(mYaw, mPitch);
					}
					if (!(mRotateFirst && rotate)) {
						NmsUtils.getVersionAdapter().moveEntity(mBoss, mVec);
					}
				}

				mTicks += 2;
			}
		}.runTaskTimer(mPlugin, 0, 2);
	}

	public void setRandomPoint() {
		setRandomPoint(1);
	}

	public void setRandomPoint(double speedMultiplier) {
		Location point = null;
		Collections.shuffle(mPoints);
		for (Location l : mPoints) {
			// We don't want to choose a location very close to where we are (so that we don't go too far away from the center)
			if (l.distanceSquared(mBoss.getLocation()) > RADIUS * RADIUS / 4.0) {
				point = l;
				break;
			}
		}
		if (point == null) {
			point = mPoints.get(0);
			MMLog.warning("[Steel Wing Hawk] Failed to find a random point! Choosing the first one in the list.");
		}
		setNextPoint(point, DISTANCE_PER_TICK * speedMultiplier, ROTATION_TICKS, (int) (COOLDOWN / speedMultiplier), false);
	}

	public void setNextPoint(Location point, double distancePerTick, int rotationTicks, int cooldown, boolean rotateFirst) {
		MMLog.fine("[Steel Wing Hawk] Set next point to " + point.toVector() + ". Currently at " + mBoss.getLocation().toVector());
		Vector vecToPoint = LocationUtils.getDirectionTo(point, mBoss.getLocation());
		mVec = vecToPoint.clone().multiply(2 * distancePerTick);
		float pitchToPoint = (float) Math.toDegrees(Math.asin(vecToPoint.getY()));
		float yawToPoint = (float) Math.toDegrees(Math.atan2(-vecToPoint.getX(), vecToPoint.getZ()));
		// The 2 here is because the rotation is only done every 2 ticks
		mYawDifference = 2 * (yawToPoint - mYaw) / rotationTicks;
		mPitchDifference = 2 * (pitchToPoint - mPitch) / rotationTicks;
		mTicks = 0;
		mCooldown = cooldown;
		mRotationTicks = rotationTicks;
		mRotateFirst = rotateFirst;
	}

	public void freeze(int cooldown) {
		MMLog.fine("[Steel Wing Hawk] Froze for " + cooldown + " ticks.");
		mVec = null;
		mTicks = 0;
		mCooldown = cooldown;
		mRotationTicks = 0;
		// No need to set other variables because they are only used if mVec is not null
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
