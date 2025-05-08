package com.playmonumenta.plugins.shardhealth.g1;

import java.util.Arrays;
import java.util.Optional;

public record G1GcCycleInfo(
	G1GcCycleType type,
	int threads,
	long duration,
	long oldGenFreed,
	long survivorFreed,
	long edenFreed
) {
	public enum G1GcCycleType {
		YOUNG("G1 Young Generation"),
		CONCURRENT("G1 Concurrent GC"),
		OLD("G1 Old Generation");

		public final String mName;

		G1GcCycleType(String mName) {
			this.mName = mName;
		}

		public static Optional<G1GcCycleType> getType(String name) {
			return Arrays.stream(values()).filter(x -> x.mName.equals(name)).findFirst();
		}
	}

	public long totalMemoryFreed() {
		return oldGenFreed + survivorFreed + edenFreed;
	}
}
