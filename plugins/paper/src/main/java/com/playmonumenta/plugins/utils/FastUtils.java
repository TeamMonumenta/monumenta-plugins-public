package com.playmonumenta.plugins.utils;

import it.unimi.dsi.util.XoRoShiRo128PlusRandom;

public class FastUtils {
	public static final XoRoShiRo128PlusRandom RANDOM = new XoRoShiRo128PlusRandom();

	private static final int SIN_BITS;
	private static final int SIN_MASK;
	private static final int SIN_COUNT;
	private static final double radFull;
	private static final double radToIndex;
	private static final double degFull;
	private static final double degToIndex;
	private static final double[] sin;
	private static final double[] cos;

	static {
		SIN_BITS = 12;
		SIN_MASK = ~(-1 << SIN_BITS);
		SIN_COUNT = SIN_MASK + 1;

		radFull = Math.PI * 2.0;
		degFull = 360.0;
		radToIndex = SIN_COUNT / radFull;
		degToIndex = SIN_COUNT / degFull;

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
	}

	public static double randomDoubleInRange(double min, double max) {
		return (RANDOM.nextDouble() * (max - min)) + min;
	}

	/**
	 * Fast, reduced-accuracy sin implementation
	 * @param rad Angle measure in radians
	 * @return Opposite length divided by hypotenuse
	 */

	public static double sin(double rad) {
		return sin[(int) (rad * radToIndex) & SIN_MASK];
	}

	/**
	 * Fast, reduced-accuracy sin implementation
	 * @param deg Angle measure in degrees
	 * @return Opposite length divided by hypotenuse
	 */

	public static double sinDeg(double deg) {
		return sin[(int) (deg * degToIndex) & SIN_MASK];
	}

	/**
	 * Fast, reduced-accuracy cos implementation
	 * @param rad Angle measure in radians
	 * @return Adjacent length divided by hypotenuse
	 */
	public static double cos(double rad) {
		return cos[(int) (rad * radToIndex) & SIN_MASK];
	}

	/**
	 * Fast, reduced-accuracy cos implementation
	 * @param deg Angle measure in degrees
	 * @return Adjacent length divided by hypotenuse
	 */
	public static double cosDeg(double deg) {
		return cos[(int) (deg * degToIndex) & SIN_MASK];
	}
}
