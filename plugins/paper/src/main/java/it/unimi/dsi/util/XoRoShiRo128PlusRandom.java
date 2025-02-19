package it.unimi.dsi.util;

/*
 * DSI utilities
 *
 * Copyright (C) 2013-2020 Sebastiano Vigna
 *
 *  This library is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by the Free
 *  Software Foundation; either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses/>.
 *
 */

import java.util.Random;
import java.util.SplittableRandom;

/** A fast, high-quality {@linkplain Random pseudorandom number generator} for floating-point generation.
 * It has excellent speed,
 * but its state space (128 bits) is large enough for
 * mild parallelism only. It passes all tests we are aware of except for the four
 * lower bits, which might fail linearity tests (and just those), so if
 * low linear complexity is not considered an issue (as it is usually the
 * case) it can be used to generate integer outputs, too; moreover, this
 * generator has a very mild Hamming-weight dependency making <a href="http://prng.di.unimi.it/hwd.php">our test</a>
 * fail after 8 TB of output; we believe
 * this slight bias cannot affect any application. If you are concerned,
 * use {@link XoRoShiRo128StarStarRandom} or {@link XoShiRo256PlusRandom}.
 * More information can be found at our <a href="http://prng.di.unimi.it/">PRNG page</a>.
 *
 * <p><strong>Warning</strong>: the constants used in this generator differ from the ones used in the 2016 version.
 *
 * <p>If you need a general PRNG, use {@link XoRoShiRo128StarStarRandom}. If you can use more space,
 * you might try {@link XoShiRo256PlusRandom}.
 *
 * <p>By using the supplied {@link #jump()} method it is possible to generate non-overlapping long sequences
 * for parallel computations; {@link #longJump()} makes it possible to create several
 * starting points, each providing several non-overlapping sequences, for distributed computations. This class provides also a {@link #split()} method to support recursive parallel computations, in the spirit of
 * {@link SplittableRandom}.
 *
 * <p><strong>Warning</strong>: before release 2.6.3, the {@link #split()} method
 * would not alter the state of the caller, and it would return instances initialized in the same
 * way if called multiple times. This was a major mistake in the implementation, and it has been fixed,
 * but as a consequence the output of the caller after a call to {@link #split()} is
 * now different, and the result of {@link #split()} is initialized in a different way.
 *
 * <p>Note that this is not a {@linkplain SecureRandom secure generator}.
 *
 * @version 1.0
 * @see it.unimi.dsi.util
 * @see RandomGenerator
 * @see XoRoShiRo128PlusRandomGenerator
 */

public class XoRoShiRo128PlusRandom extends Random {
	private static final long serialVersionUID = 1L;
	/** The internal state of the algorithm. */
	private long mS0;
	private long mS1;

	protected XoRoShiRo128PlusRandom(final long s0, final long s1) {
		this.mS0 = s0;
		this.mS1 = s1;
	}

	/** Creates a new generator seeded using {@link Util#randomSeed()}. */
	/* Monumenta NOTE: Seed with time instead, which lacks uniqueness properties */
	public XoRoShiRo128PlusRandom() {
		this(System.nanoTime());
	}

	/** Creates a new generator using a given seed.
	 *
	 * @param seed a seed for the generator.
	 */
	public XoRoShiRo128PlusRandom(final long seed) {
		setSeed(seed);
	}


	/** Returns a copy of this generator. The sequences produced by this generator and by the returned generator will be identical.
	 *
	 * <p>This method is particularly useful in conjunction with the {@link #jump()} (or {@link #longJump()}) method: by calling repeatedly
	 * {@link #jump() jump().copy()} over a generator it is possible to create several generators producing non-overlapping sequences.
	 *
	 * @return a copy of this generator.
	 */
	public XoRoShiRo128PlusRandom copy() {
		return new XoRoShiRo128PlusRandom(mS0, mS1);
	}

	@Override
	public int nextInt() {
		return (int)(nextLong() >>> 32);
	}

	@Override
	public int nextInt(final int n) {
		return (int)nextLong(n);
	}

	@Override
	public long nextLong() {
		final long s0 = this.mS0;
		long s1 = this.mS1;
		final long result = s0 + s1;
		s1 ^= s0;
		this.mS0 = Long.rotateLeft(s0, 24) ^ s1 ^ s1 << 16;
		this.mS1 = Long.rotateLeft(s1, 37);
		return result;
	}

	/** Returns a pseudorandom uniformly distributed {@code long} value
	 * between 0 (inclusive) and the specified value (exclusive), drawn from
	 * this random number generator's sequence. The algorithm used to generate
	 * the value guarantees that the result is uniform, provided that the
	 * sequence of 64-bit values produced by this generator is.
	 *
	 * @param n the positive bound on the random number to be returned.
	 * @return the next pseudorandom {@code long} value between {@code 0} (inclusive) and {@code n} (exclusive).
	 */
	@Override
	public long nextLong(final long n) {
		if (n <= 0) {
			throw new IllegalArgumentException("illegal bound " + n + " (must be positive)");
		}
		long t = nextLong();
		final long nMinus1 = n - 1;
		// Shortcut for powers of two--high bits
		if ((n & nMinus1) == 0) {
			return (t >>> Long.numberOfLeadingZeros(nMinus1)) & nMinus1;
		}
		// Rejection-based algorithm to get uniform integers in the general case
		for (long u = t >>> 1; u + nMinus1 - (t = u % n) < 0; u = nextLong() >>> 1) {
			// Loop
		}
		return t;
	}

	/**
	 * Returns the next pseudorandom, uniformly distributed
	 * {@code double} value between {@code 0.0} and
	 * {@code 1.0} from this random number generator's sequence,
	 * using a fast multiplication-free method which, however,
	 * can provide only 52 significant bits.
	 *
	 * <p>This method is faster than {@link #nextDouble()}, but it
	 * can return only dyadic rationals of the form <var>k</var> / 2<sup>&minus;52</sup>,
	 * instead of the standard <var>k</var> / 2<sup>&minus;53</sup>. Before
	 * version 2.4.1, this was actually the standard implementation of
	 * {@link #nextDouble()}, so you can use this method if you need to
	 * reproduce exactly results obtained using previous versions.
	 *
	 * <p>The only difference between the output of this method and that of
	 * {@link #nextDouble()} is an additional least significant bit set in half of the
	 * returned values. For most applications, this difference is negligible.
	 *
	 * @return the next pseudorandom, uniformly distributed {@code double}
	 * value between {@code 0.0} and {@code 1.0} from this
	 * random number generator's sequence, using 52 significant bits only.
	 *
	 * @since 2.4.1
	 */
	/* Monumenta NOTE: Renamed to nextDouble(), which was removed */
	@Override
	public double nextDouble() {
		return Double.longBitsToDouble(0x3FFL << 52 | nextLong() >>> 12) - 1.0;
	}

	@Override
	public float nextFloat() {
		return (nextLong() >>> 40) * 0x1.0p-24f;
	}

	@Override
	public boolean nextBoolean() {
		return nextLong() < 0;
	}

	@Override
	public void nextBytes(final byte[] bytes) {
		int i = bytes.length;
		int n = 0;
		while (i != 0) {
			n = Math.min(i, 8);
			for (long bits = nextLong(); n-- != 0; bits >>= 8) {
				bytes[--i] = (byte)bits;
			}
		}
	}

	private static final long[] JUMP = { 0xdf900294d8f554a5L, 0x170865df4b3201fcL };

	protected XoRoShiRo128PlusRandom jump(final long[] jump) {
		long s0 = 0;
		long s1 = 0;
		for (final long element : jump) {
			for (int b = 0; b < 64; b++) {
				if ((element & 1L << b) != 0) {
					s0 ^= this.mS0;
					s1 ^= this.mS1;
				}
				nextLong();
			}
		}

		this.mS0 = s0;
		this.mS1 = s1;
		return this;
	}


	/** The jump function for this generator. It is equivalent to 2<sup>64</sup>
	 * calls to {@link #nextLong()}; it can be used to generate 2<sup>64</sup>
	 * non-overlapping subsequences for parallel computations.
	 *
	 * @return this generator.
	 * @see #copy()
	 */

	public XoRoShiRo128PlusRandom jump() {
		return jump(JUMP);
	}

	private static final long[] LONG_JUMP = { 0xd2a98b26625eee7bL, 0xdddf9b1090aa7ac1L };

	/** The long-jump function for this generator. It is equivalent to 2<sup>96</sup>
	 * calls to {@link #nextLong()}; it can be used to generate 2<sup>32</sup> starting points,
	 * from each of which {@link #jump()} will generate 2<sup>32</sup> non-overlapping
	 * subsequences for parallel distributed computations.
	 *
	 * @return this generator.
	 * @see #copy()
	 */

	public XoRoShiRo128PlusRandom longJump() {
		return jump(LONG_JUMP);
	}


	/**
	 * Returns a new instance that shares no mutable state
	 * with this instance. The sequence generated by the new instance
	 * depends deterministically on the state of this instance,
	 * but the probability that the sequence generated by this
	 * instance and by the new instance overlap is negligible.
	 *
	 * <p><strong>Warning</strong>: before release 2.6.3, this method
	 * would not alter the state of the caller, and it would return instances initialized in the same
	 * way if called multiple times. This was a major mistake in the implementation, and it has been fixed,
	 * but as a consequence the output of this instance after a call to this method is
	 * now different, and the returned instance is initialized in a different way.
	 *
	 * @return the new instance.
	 */
	public XoRoShiRo128PlusRandom split() {
		nextLong();
		final XoRoShiRo128PlusRandom split = copy();

		long h0 = mS0;
		long h1 = mS1;
		long h2 = mS0 + 0x55a650a4c1dac3e9L; // Random constants
		long h3 = mS1 + 0xb39ae98dfa439b73L;

		// A round of SpookyHash ShortMix
		h2 = Long.rotateLeft(h2, 50);
		h2 += h3;
		h0 ^= h2;
		h3 = Long.rotateLeft(h3, 52);
		h3 += h0;
		h1 ^= h3;
		h0 = Long.rotateLeft(h0, 30);
		h0 += h1;
		h2 ^= h0;
		h1 = Long.rotateLeft(h1, 41);
		h1 += h2;
		h3 ^= h1;
		h2 = Long.rotateLeft(h2, 54);
		h2 += h3;
		h0 ^= h2;
		h3 = Long.rotateLeft(h3, 48);
		h3 += h0;
		h1 ^= h3;
		h0 = Long.rotateLeft(h0, 38);
		h0 += h1;
		h2 ^= h0;
		h1 = Long.rotateLeft(h1, 37);
		h1 += h2;
		h3 ^= h1;
		h2 = Long.rotateLeft(h2, 62);
		h2 += h3;
		h0 ^= h2;
		h3 = Long.rotateLeft(h3, 34);
		h3 += h0;
		h1 ^= h3;
		h0 = Long.rotateLeft(h0, 5);
		h0 += h1;
		h2 ^= h0;
		h1 = Long.rotateLeft(h1, 36);
		h1 += h2;
		//h3 ^= h1;

		split.mS0 = h0;
		split.mS1 = h1;

		return split;
	}

	/** Sets the seed of this generator.
	 *
	 * <p>The argument will be used to seed a {@link SplitMix64RandomGenerator}, whose output
	 * will in turn be used to seed this generator. This approach makes &ldquo;warmup&rdquo; unnecessary,
	 * and makes the probability of starting from a state
	 * with a large fraction of bits set to zero astronomically small.
	 *
	 * @param seed a seed for this generator.
	 */
	/* Monumenta NOTE : Just use Java's Random instead to generate the seed */
	@Override
	public synchronized void setSeed(final long seed) {
		final SplittableRandom r = new SplittableRandom(seed); // We use SplittableRandom instead of Random for better performance and better seed generation
		mS0 = r.nextLong();
		mS1 = r.nextLong();
	}


	/** Sets the state of this generator.
	 *
	 * <p>The internal state of the generator will be reset, and the state array filled with the provided array.
	 *
	 * @param state an array of 2 longs; at least one must be nonzero.
	 */
	public void setState(final long[] state) {
		if (state.length != 2) {
			throw new IllegalArgumentException("The argument array contains " + state.length + " longs instead of " + 2);
		}
		mS0 = state[0];
		mS1 = state[1];
	}
}
