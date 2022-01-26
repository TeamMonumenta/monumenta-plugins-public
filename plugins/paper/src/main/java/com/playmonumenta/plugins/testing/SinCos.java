package com.playmonumenta.plugins.testing;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
public class SinCos {
	// Example values used in the tests:
	//   15 deg = 0.2617993878 rad
	//   105 deg = 1.8325957146 rad
	//   285 deg= 4.9741883682 rad
	public double mValueDoubleA = 0.2617993878;
	public double mValueDoubleB = 1.8325957146;
	public double mValueDoubleC = 4.9741883682;

	///////////////////////////////////////
	// Default sin
	///////////////////////////////////////

	@Benchmark
	public double math_default_sin() {
		return Math.sin(mValueDoubleA) + Math.sin(mValueDoubleB) + Math.sin(mValueDoubleC);
	}

	@Benchmark
	public double math_default_cos() {
		return Math.cos(mValueDoubleA) + Math.cos(mValueDoubleB) + Math.cos(mValueDoubleC);
	}

	///////////////////////////////////////
	// FastCosSin.java posted by kappa  ( http://www.java-gaming.org/topics/extremely-fast-atan2/36467/msg/346117/view.html#msg346117 )
	///////////////////////////////////////

	public static final class Fast {

		private static final double PI = 3.1415927f;
		private static final double MINUS_PI = -PI;
		private static final double DOUBLE_PI = PI * 2f;
		private static final double PI_2 = PI / 2f;

		private static final double CONST_1 = 4f / PI;
		private static final double CONST_2 = 4f / (PI * PI);

		public static double sin(double x) {
			if (x < MINUS_PI) {
				x += DOUBLE_PI;
			} else if (x > PI) {
				x -= DOUBLE_PI;
			}

			return (x < 0f) ? (CONST_1 * x + CONST_2 * x * x)
				: (CONST_1 * x - CONST_2 * x * x);
		}

		public static double cos(double x) {
			if (x < MINUS_PI) {
				x += DOUBLE_PI;
			} else if (x > PI) {
				x -= DOUBLE_PI;
			}

			x += PI_2;

			if (x > PI) {
				x -= DOUBLE_PI;
			}

			return (x < 0f) ? (CONST_1 * x + CONST_2 * x * x)
				: (CONST_1 * x - CONST_2 * x * x);
		}
	}

	@Benchmark
	public double math_fast_sin() {
		return Fast.sin(mValueDoubleA) + Fast.sin(mValueDoubleB) + Fast.sin(mValueDoubleC);
	}

	@Benchmark
	public double math_fast_cos() {
		return Fast.cos(mValueDoubleA) + Fast.cos(mValueDoubleB) + Fast.cos(mValueDoubleC);
	}

	///////////////////////////////////////
	// Devmaster's sine/cosine ( http://forum.devmaster.net/t/fast-and-accurate-sine-cosine/9648 )
	///////////////////////////////////////

	public static final class Devmaster {

		public static final double PI = 3.1415927f;
		public static final double PI_2 = PI / 2f;
		public static final double DOUBLE_PI = PI * 2f;
		public static final double B = 4 / PI;
		public static final double C = -4 / (PI * PI);
		public static final double P = 0.225f;

		public static double sin(double x) {
			double x1 = x % PI;
			double x2 = x % DOUBLE_PI;

			double y;
			if (x > 0) {
				y = x1 * (B + C * x1);
				y = (y > 0) ? (P * (y * y - y) + y)
					: (P * (-y * y - y) + y);
				double xp = x2 - DOUBLE_PI;
				if (!(xp < 0 && xp < -PI)) {
					y = -y;
				}
			} else {
				y = x1 * (B - C * x1);
				y = (y > 0) ? (P * (y * y - y) + y)
					: (P * (-y * y - y) + y);
				double xp = x2 + DOUBLE_PI;
				if (xp > 0 && xp < PI) {
					y = -y;
				}
			}
			return y;
		}

		public static double cos(double x) {
			double x0 = x + PI_2;
			double x1 = x0 % PI;
			double x2 = x0 % DOUBLE_PI;

			double y;
			if (x0 > 0) {
				y = x1 * (B + C * x1);
				y = (y > 0) ? (P * (y * y - y) + y)
					: (P * (-y * y - y) + y);
				double xp = x2 - DOUBLE_PI;
				if (!(xp < 0 && xp < -PI)) {
					y = -y;
				}
			} else {
				y = x1 * (B - C * x1);
				y = (y > 0) ? (P * (y * y - y) + y)
					: (P * (-y * y - y) + y);
				double xp = x2 + DOUBLE_PI;
				if (xp > 0 && xp < PI) {
					y = -y;
				}
			}
			return y;
		}
	}

	@Benchmark
	public double math_devmaster_sin() {
		return Devmaster.sin(mValueDoubleA) + Devmaster.sin(mValueDoubleB) + Devmaster.sin(mValueDoubleC);
	}

	@Benchmark
	public double math_devmaster_cos() {
		return Devmaster.cos(mValueDoubleA) + Devmaster.cos(mValueDoubleB) + Devmaster.cos(mValueDoubleC);
	}

	///////////////////////////////////////
	// Riven's sine/cosine ( http://www.java-gaming.org/topics/fast-math-sin-cos-lookup-tables/24191/view.html )
	///////////////////////////////////////

	public static final class Riven {

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

		public static double sin(double rad) {
			return sin[(int) (rad * radToIndex) & SIN_MASK];
		}

		public static double cos(double rad) {
			return cos[(int) (rad * radToIndex) & SIN_MASK];
		}
	}

	@Benchmark
	public double math_riven_sin() {
		return Riven.sin(mValueDoubleA) + Riven.sin(mValueDoubleB) + Riven.sin(mValueDoubleC);
	}

	@Benchmark
	public double math_riven_cos() {
		return Riven.cos(mValueDoubleA) + Riven.cos(mValueDoubleB) + Riven.cos(mValueDoubleC);
	}

	///////////////////////////////////////
	// Icecore's sine/cosine ( http://www.java-gaming.org/topics/extremely-fast-sine-cosine/36469/msg/346190/view.html#msg346190 )
	///////////////////////////////////////

	public static final class Icecore {

		private static final int Size_SC_Ac = 5000;
		private static final int Size_SC_Ar = Size_SC_Ac + 1;
		private static final double[] Sin = new double[Size_SC_Ar];
		private static final double[] Cos = new double[Size_SC_Ar];
		private static final double Pi = Math.PI;
		private static final double Pi_D = Pi * 2;
		private static final double Pi_SC_D = Pi_D / Size_SC_Ac;

		static {
			for (int i = 0; i < Size_SC_Ar; i++) {
				double d = i * Pi_SC_D;
				Sin[i] = Math.sin(d);
				Cos[i] = Math.cos(d);
			}
		}

		public static double sin(double r) {
			double rp = r % Pi_D;
			if (rp < 0) {
				rp += Pi_D;
			}
			return Sin[(int) (rp / Pi_SC_D)];
		}

		public static double cos(double r) {
			double rp = r % Pi_D;
			if (rp < 0) {
				rp += Pi_D;
			}
			return Cos[(int) (rp / Pi_SC_D)];
		}
	}

	@Benchmark
	public double math_icecore_sin() {
		return Icecore.sin(mValueDoubleA) + Icecore.sin(mValueDoubleB) + Icecore.sin(mValueDoubleC);
	}

	@Benchmark
	public double math_icecore_cos() {
		return Icecore.cos(mValueDoubleA) + Icecore.cos(mValueDoubleB) + Icecore.cos(mValueDoubleC);
	}
}
