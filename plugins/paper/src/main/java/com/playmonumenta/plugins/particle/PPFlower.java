package com.playmonumenta.plugins.particle;

import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import javax.annotation.Nullable;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class PPFlower extends AbstractPartialParticle<PPFlower> {

	protected double mRadius;
	protected int mPetals = 3;
	protected double mAngleStep = 0.1;
	protected Vector mNormal = new Vector(0, 1, 0);
	protected @Nullable Color mInnerColor = null;
	protected @Nullable Color mOuterColor = null;
	protected float mSize = 0;
	protected boolean mSharp = false;

	/*-------------------------------------------------------------------------------
	 * Constructors
	 */

	public PPFlower(Particle particle, Location centerLocation, double radius) {
		super(particle, centerLocation);
		mRadius = radius;
	}

	/*-------------------------------------------------------------------------------
	 * Parameter getters and setters
	 */

	public PPFlower petals(int count) {
		mPetals = Math.max(3, count);
		return this;
	}

	public int petals() {
		return mPetals;
	}

	public PPFlower radius(double radius) {
		mRadius = radius;
		return this;
	}

	public double radius() {
		return mRadius;
	}

	public PPFlower angleStep(double angleStep) {
		mAngleStep = angleStep;
		return this;
	}

	public double angleStep() {
		return mAngleStep;
	}

	public PPFlower sharp(boolean sharp) {
		mSharp = sharp;
		return this;
	}

	public boolean sharp() {
		return mSharp;
	}

	public PPFlower normal(Vector normal) {
		mNormal = normal.normalize();
		return this;
	}

	public PPFlower transitionColors(Color innerColor, Color outerColor, float size) {
		mInnerColor = innerColor;
		mOuterColor = outerColor;
		mSize = size;
		return this;
	}

	/*-------------------------------------------------------------------------------
	 * Methods
	 */

	@Override
	protected int getPartialCount(double multiplier, Player player, ParticleCategory source) {
		int finalPetals = mSharp ? (mPetals % 2 == 0 ? mPetals * 2 : mPetals) : mPetals;
		double endAngle = mSharp ? Math.PI * 4 : Math.PI * (finalPetals - 2);
		double finalAngleStep = mSharp ? mAngleStep / 4.0 : mAngleStep;
		return Math.max(mMinimumCount, (int) Math.ceil(endAngle / finalAngleStep * multiplier));
	}

	@Override
	protected void doSpawn(PartialParticleBuilder packagedValues) {
		double partialCount = packagedValues.count();
		Location centerLoc = packagedValues.location();
		if (centerLoc == null) {
			return;
		}
		packagedValues.count(1);

		Vector right = VectorUtils.rotateTargetDirection(mNormal.clone(), 90, 0);
		Vector up = VectorUtils.rotateTargetDirection(mNormal.clone(), 0, -90);

		// If sharp and even, return double the petals. In other cases, return the specified number of petals.
		int finalPetals = mSharp ? (mPetals % 2 == 0 ? mPetals * 2 : mPetals) : mPetals;
		double endAngle = mSharp ? Math.PI * 4 : Math.PI * (finalPetals - 2);

		// Scale the amount of particles with particle settings
		double partialAngleStep = endAngle / partialCount;

		Location currLoc;
		for (double theta = 0; theta <= endAngle; theta += partialAngleStep) {
			currLoc = centerLoc.clone();

			if (mSharp) {
				currLoc.add(up.clone().multiply(
					mRadius * (1 - Math.abs(FastUtils.cos((double) finalPetals / 4.0 * theta))) * FastUtils.sin(theta)
				));
				currLoc.add(right.clone().multiply(
					mRadius * (1 - Math.abs(FastUtils.cos((double) finalPetals / 4.0 * theta))) * FastUtils.cos(theta)
				));
			} else {
				currLoc.add(up.clone().multiply(
					mRadius * FastUtils.cos((double) finalPetals / ((double) finalPetals - 2) * theta) * FastUtils.sin(theta)
				));
				currLoc.add(right.clone().multiply(
					mRadius * FastUtils.cos((double) finalPetals / ((double) finalPetals - 2) * theta) * FastUtils.cos(theta)
				));
			}

			packagedValues.location(currLoc);

			if (packagedValues.particle().equals(Particle.REDSTONE) && mInnerColor != null && mOuterColor != null) {
				double distanceSquared = currLoc.distanceSquared(centerLoc);
				double percent = distanceSquared / (mRadius * mRadius);
				packagedValues.color(ParticleUtils.getTransition(mInnerColor, mOuterColor, percent), mSize);
			}

			spawnUsingSettings(packagedValues);
		}
	}
}
