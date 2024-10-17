package com.playmonumenta.plugins.particle;

import com.playmonumenta.plugins.Plugin;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link PartialParticle} that takes a list of control points and draws a Bézier curve using the Bernstein polynomial formula.
 * Note that in general a Bézier curve will not pass through any control point other than the start and end points.
 *
 * @see PPCircle PPCircle to draw circles and rings
 * @see PPLine PPLine to draw simple lines
 * @see PPParametric PPParametric to draw parametric curves with your own parametric function
 */
public class PPBezier extends AbstractPartialParticle<PPBezier> {

	public static final int MAX_CONTROL_POINTS = 10;
	private final List<Location> mControlPoints;
	private final int[][] mBinomialCoefficients = {
		{1},
		{1, 1},
		{1, 2, 1},
		{1, 3, 3, 1},
		{1, 4, 6, 4, 1},
		{1, 5, 10, 10, 5, 1},
		{1, 6, 15, 20, 15, 6, 1},
		{1, 7, 21, 35, 35, 21, 7, 1},
		{1, 8, 28, 56, 70, 56, 28, 8, 1},
		{1, 9, 36, 84, 126, 126, 84, 36, 9, 1},
		{1, 10, 45, 120, 210, 252, 210, 120, 45, 10, 1}
	};
	private int mAnimationTicks = 1;

	/**
	 * An n-point Bézier curve, with an upper bound of {@value MAX_CONTROL_POINTS}
	 * @param particle A particle.
	 * @param controlPoints A list of locations beginning with a starting location (t = 0.0) and ending with an ending location (t = 1.0)
	 *                      The list can contain one element, such that the starting and ending points are the same, but not zero elements.
	 *                      If the list size is >= {@value MAX_CONTROL_POINTS}, the list will be sliced at the 10th element.
	 */
	public PPBezier(Particle particle, @NotNull List<Location> controlPoints) {
		super(particle, controlPoints.get(0));

		if (controlPoints.size() == 1) {
			controlPoints.add(controlPoints.get(0).clone());
		}

		 if (controlPoints.size() > MAX_CONTROL_POINTS) {
			 mControlPoints = controlPoints.subList(0, MAX_CONTROL_POINTS - 1);
		 } else {
			 mControlPoints = controlPoints;
		 }

		mLocation = controlPoints.get(0);
	}

	/**
	 * A linear Bezier curve.
	 */
	public PPBezier(Particle particle, Location startLocation, Location endLocation) {
		super(particle, startLocation);
		mControlPoints = Arrays.asList(startLocation, endLocation);
		mLocation = startLocation;
	}

	/**
	 * A quadratic Bezier curve.
	 */
	public PPBezier(Particle particle, Location startLocation, Location middleLocation, Location endLocation) {
		super(particle, startLocation);
		mControlPoints = Arrays.asList(startLocation, middleLocation, endLocation);
		mLocation = startLocation;
	}

	public PPBezier delay(int animationTicks) {
		if (animationTicks < 1) {
			animationTicks = 1;
		}
		mAnimationTicks = animationTicks;
		return this;
	}

	@Override
	protected void doSpawn(PartialParticleBuilder packagedValues) {
		int count = packagedValues.count();
		int n = mControlPoints.size() - 1;

		if (mAnimationTicks > 1) {
			new BukkitRunnable() {
				int mT = 0;
				final int mCountPerTick = (int) Math.ceil((float) count / mAnimationTicks);
				int mI = 0;

				@Override
				public void run() {
					for (int j = 0; j <= mCountPerTick; j++) {
						if (mI > count) {
							break;
						}

						double t = 1.0 * mI / count;
						double x = 0f;
						double y = 0f;
						double z = 0f;

						for (int k = 0; k <= n; k++) {
							double blend = bernstein(n, k, t);
							x += blend * mControlPoints.get(k).getX();
							y += blend * mControlPoints.get(k).getY();
							z += blend * mControlPoints.get(k).getZ();
						}

						packagedValues.location(mLocation.clone().set(x, y, z));
						spawnUsingSettings(packagedValues);

						mI++;
					}

					mT++;
					if (mT >= mAnimationTicks || mI > count) {
						this.cancel();
					}
				}
			}.runTaskTimerAsynchronously(Plugin.getInstance(), 0, 1);
		} else {
			for (int i = 0; i <= count; i++) {
				double t = 1.0 * i / count;
				double x = 0f;
				double y = 0f;
				double z = 0f;

				for (int j = 0; j <= n; j++) {
					double blend = bernstein(n, j, t);
					x += blend * mControlPoints.get(j).getX();
					y += blend * mControlPoints.get(j).getY();
					z += blend * mControlPoints.get(j).getZ();
				}

				packagedValues.location(mLocation.clone().set(x, y, z));
				spawnUsingSettings(packagedValues);
			}
		}
	}

	private double bernstein(int n, int i, double t) {
		return binomialCoefficient(n, i) * Math.pow(t, i) * Math.pow(1 - t, n - i);
	}

	private int binomialCoefficient(int n, int k) {
		if (n <= MAX_CONTROL_POINTS && k <= MAX_CONTROL_POINTS) {
			return mBinomialCoefficients[n][k];
		}

		int[][] dp = new int[n + 1][k + 1];

		for (int i = 0; i <= n; i++) {
			for (int j = 0; j < Math.min(i, k); j++) {
				if (j == 0 || j == i) { // i.e. the sides of Pascal's triangle
					dp[i][j] = 1; // are 1's
				} else {
					dp[i][j] = dp[i - 1][j - 1] + dp[i - 1][j];
				}
			}
		}

		return dp[n][k];
	}
}
