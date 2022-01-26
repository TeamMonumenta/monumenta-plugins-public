package com.playmonumenta.plugins.point;

import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Snow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;

public class Raycast {

	private final Vector mDir;

	public int mIterations = 10;
	public double mHitRange = 0.5;
	public double mDirMultiplier = 1;
	public double mDistanceCheck = 1;

	public boolean mTargetPlayers = false;
	public boolean mTargetNonPlayers = true;
	public boolean mThroughBlocks = false;
	public boolean mThroughNonOccluding = false;
	public boolean mNoIterations = false;

	public Particle mParticle = null;

	public boolean mPrecision = false;
	public int mPrecisionTicks = 0;

	//Locations
	private final Location mStart;
	private Location mEnd = null;

	public Raycast(Location p1, Location p2) {
		this.mDir = LocationUtils.getDirectionTo(p2, p1);
		this.mStart = p1;
		this.mEnd = p2;

		//If we're going from one point to another,
		//ignore using iteration by default.
		this.mNoIterations = true;
	}

	public Raycast(Location start, Vector dir, int iterations) {
		this.mDir = dir;
		this.mIterations = iterations;
		this.mStart = start;
	}

	/**
	 * Fires the raycast with the given properties
	 * @return All entities hit by the raycast
	 */
	public RaycastData shootRaycast() {
		int i = 0;
		double dist = 0;
		RaycastData data = new RaycastData();
		List<LivingEntity> entities = data.getEntities();
		if (mEnd != null) {
			dist = mStart.distance(mEnd);
		}
		while (true) {
			// safety for reaching the end of the ray as precision ends.
			if (!mNoIterations && !mPrecision) {
				if (i >= mIterations) {
					break;
				}
			}

			// spawn particles along the ray if desired.
			if (mParticle != null) {
				mStart.getWorld().spawnParticle(mParticle, mStart, 1, 0, 0, 0, 0.0001);
			}

			mStart.add(mDir.clone().multiply(mDirMultiplier));
			if (!mStart.isChunkLoaded()) {
				break;
			}
			Block block = mStart.getBlock();
			if (!data.getBlocks().contains(block)) {
				data.getBlocks().add(block);
			}

			if (!mThroughBlocks) {
				// breakRay: determines if the ray should collide and end on this block.
				boolean breakRay = LocationUtils.collidesWithSolid(mStart, block);
				if (breakRay) {
					if (!mThroughNonOccluding) {
						break;
					} else if (block.getType().isOccluding()) {
						break;
					}
				}

				// Much higher precision is needed when going through semi-solid blocks.
				// When the unprecise ray reaches a semisolid block without colliding, the ray will retrace
				// both the previous and the next two steps with pixel precision to verify the result.
				if (mPrecision == false && (block.getType().isSolid()
				                           || block.getBlockData() instanceof Snow
				                           || block.getBlockData() instanceof Bed)) {
					mStart.subtract(mDir.clone().multiply(mDirMultiplier));
					mPrecision = true;
					mPrecisionTicks = 49;
					if (!mNoIterations) {
						if (mIterations - i < 3) {
							mPrecisionTicks = (mIterations - i) * 16 + 1;
						}
						i = i + 3;
					}
					mDirMultiplier = (1.0 / 16.0);
				}
			}

			for (Entity e : mStart.getWorld().getNearbyEntities(mStart, mHitRange, mHitRange, mHitRange)) {
				if (e instanceof LivingEntity) {
					//  Make sure we should be targeting this entity.
					if ((mTargetPlayers && (e instanceof Player)) || (mTargetNonPlayers && !(e instanceof Player))) {
						if (!entities.contains(e)) {
							data.getEntities().add((LivingEntity)e);
						}
					}
				}
			}

			if (mEnd != null) {
				if (mStart.distance(mEnd) < mDistanceCheck) {
					break;
				}

				//This is to prevent an infinite loop should somehow
				//the raycast goes further past the end point.
				if (mStart.distance(mEnd) > dist) {
					break;
				} else {
					dist = mStart.distance(mEnd);
				}
			}

			if (!mNoIterations && !mPrecision) {
				i++;
				if (i >= mIterations) {
					break;
				}
			}
			if (mPrecision) {
				mPrecisionTicks--;
				if (mPrecisionTicks <= 0) {
					mDirMultiplier = 1.0;
					mPrecision = false;
				}
			}
		}

		return data;
	}
}
