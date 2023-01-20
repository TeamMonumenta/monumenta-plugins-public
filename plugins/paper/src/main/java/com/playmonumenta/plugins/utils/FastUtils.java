package com.playmonumenta.plugins.utils;

import it.unimi.dsi.util.XoRoShiRo128PlusRandom;
import java.math.BigInteger;

public class FastUtils {
	public static final XoRoShiRo128PlusRandom RANDOM = new XoRoShiRo128PlusRandom();

	private static final int SIN_BITS = 12;
	private static final int SIN_MASK = ~(-1 << SIN_BITS);
	private static final int SIN_COUNT = SIN_MASK + 1;
	private static final double radFull = Math.PI * 2.0;
	private static final double radToIndex = SIN_COUNT / radFull;
	private static final double degFull = 360.0;
	private static final double degToIndex = SIN_COUNT / degFull;
	private static final double[] sin;
	private static final double[] cos;
	private static final BigInteger[] fact = new BigInteger[300];

	static {
		sin = new double[SIN_COUNT];
		cos = new double[SIN_COUNT];

		for (int i = 0; i < SIN_COUNT; i++) {
			sin[i] = Math.sin((i + 0.5f) / SIN_COUNT * radFull);
			cos[i] = Math.cos((i + 0.5f) / SIN_COUNT * radFull);
		}

		// Four cardinal directions
		for (int i = 0; i < 360; i += 90) {
			sin[(int) (i * degToIndex) & SIN_MASK] = Math.sin(i * Math.PI / 180.0);
			cos[(int) (i * degToIndex) & SIN_MASK] = Math.cos(i * Math.PI / 180.0);
		}

		fact[0] = BigInteger.ONE;
		fact[1] = BigInteger.ONE;
		for (int i = 2; i < 300; i++) {
			fact[i] = fact[i - 1].multiply(BigInteger.valueOf(i));
		}
	}

	public static int randomIntInRange(int min, int max) {
		return min == max ? min : (RANDOM.nextInt() * (max - min)) + min;
	}

	public static double randomDoubleInRange(double min, double max) {
		return min == max ? min : (RANDOM.nextDouble() * (max - min)) + min;
	}

	public static float randomFloatInRange(float min, float max) {
		return min == max ? min : (RANDOM.nextFloat() * (max - min)) + min;
	}

	/**
	 * Turns a double value into an integer, rounding randomly, but weighted towards the closer integer.
	 * For example, 1.2 will result in 1 80% of the time, and 2 20% of the time.
	 */
	public static int roundRandomly(double value) {
		double fraction = ((value % 1) + 1) % 1;
		if (fraction == 0) {
			return (int) value;
		}
		return (int) Math.floor(value) + (RANDOM.nextDouble() <= fraction ? 1 : 0);
	}

	/**
	 * Fast, reduced-accuracy sin implementation
	 *
	 * @param rad Angle measure in radians
	 * @return Opposite length divided by hypotenuse
	 */

	public static double sin(double rad) {
		return sin[(int) (rad * radToIndex) & SIN_MASK];
	}

	/**
	 * Fast, reduced-accuracy sin implementation
	 *
	 * @param deg Angle measure in degrees
	 * @return Opposite length divided by hypotenuse
	 */

	public static double sinDeg(double deg) {
		return sin[(int) (deg * degToIndex) & SIN_MASK];
	}

	/**
	 * Fast, reduced-accuracy cos implementation
	 *
	 * @param rad Angle measure in radians
	 * @return Adjacent length divided by hypotenuse
	 */
	public static double cos(double rad) {
		return cos[(int) (rad * radToIndex) & SIN_MASK];
	}

	/**
	 * Fast, reduced-accuracy cos implementation
	 *
	 * @param deg Angle measure in degrees
	 * @return Adjacent length divided by hypotenuse
	 */
	public static double cosDeg(double deg) {
		return cos[(int) (deg * degToIndex) & SIN_MASK];
	}

	public static BigInteger bigFact(int num) throws RuntimeException {
		return fact[num];
	}
}
