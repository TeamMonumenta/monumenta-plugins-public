package com.playmonumenta.plugins.utils;

import it.unimi.dsi.util.XoRoShiRo128PlusRandom;

public class FastUtils {
	public static final XoRoShiRo128PlusRandom RANDOM = new XoRoShiRo128PlusRandom();

	public static final double randomDoubleInRange(double min, double max) {
		return (RANDOM.nextDouble() * (max - min)) - min;
	}
}
