package com.playmonumenta.plugins.shardhealth.g1;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.plugins.utils.MMLog;
import org.jetbrains.annotations.Nullable;

public record G1GcHealth(
	// old gen stats
	double oldGenCycleInInterval,
	double averageOldGenFreed,
	double averageOldGenTime,
	// young gen stats
	double youngGenCycleInInterval,
	double averageYoungGenFreed,
	double averageYoungGenTime,

	// concurrent stats
	double concurrentCycleInInterval,
	double averageConcurrentFreed,
	double averageConcurrentTime,

	// overall
	double overallCyclesInInterval,
	double averageOverallFreed,
	double averageOverallTime
) {
	public static G1GcHealth defaultTargetHealth() {
		// TODO Set actual default values
		return zeroHealth();
	}

	public static G1GcHealth zeroHealth() {
		return new G1GcHealth(
			0.0,
			0.0,
			0.0,
			0.0,
			0.0,
			0.0,
			0.0,
			0.0,
			0.0,
			0.0,
			0.0,
			0.0
		);
	}

	@Nullable
	public static G1GcHealth fromJson(@Nullable JsonElement element) {
		if (!(element instanceof JsonObject object)) {
			return null;
		}

		final var components = G1GcHealth.class.getRecordComponents();
		final var args = new Object[components.length];

		for (int i = 0; i < components.length; i++) {
			final var component = components[i];
			final var name = component.getName();

			if (object.get(name) instanceof JsonPrimitive primitive && primitive.isNumber()) {
				args[i] = primitive.getAsDouble();
			} else {
				args[i] = 0;
			}
		}

		try {
			return (G1GcHealth) G1GcHealth.class.getConstructors()[0].newInstance(args);
		} catch (ReflectiveOperationException e) {
			return null;
		}
	}

	public JsonObject toJson() {
		final var object = new JsonObject();

		for (final var component : G1GcHealth.class.getRecordComponents()) {
			try {
				object.addProperty(component.getName(), (Number) component.getAccessor().invoke(this));
			} catch (ReflectiveOperationException e) {
				MMLog.warning("while serializing gc health", e);
			}
		}

		return object;
	}

	public G1GcHealth add(@Nullable G1GcHealth other) {
		if (other == null) {
			return this;
		}
		return new G1GcHealth(
			oldGenCycleInInterval + other.oldGenCycleInInterval,
			averageOldGenFreed + other.averageOldGenFreed,
			averageOldGenTime + other.averageOldGenTime,
			youngGenCycleInInterval + other.youngGenCycleInInterval,
			averageYoungGenFreed + other.averageYoungGenFreed,
			averageYoungGenTime + other.averageYoungGenTime,
			concurrentCycleInInterval + other.concurrentCycleInInterval,
			averageConcurrentFreed + other.averageConcurrentFreed,
			averageConcurrentTime + other.averageConcurrentTime,
			overallCyclesInInterval + other.overallCyclesInInterval,
			averageOverallFreed + other.averageOverallFreed,
			averageOverallTime + other.averageOverallTime
		);
	}

	public G1GcHealth divide(int divisor) {
		return new G1GcHealth(
			oldGenCycleInInterval / divisor,
			averageOldGenFreed / divisor,
			averageOldGenTime / divisor,
			youngGenCycleInInterval / divisor,
			averageYoungGenFreed / divisor,
			averageYoungGenTime / divisor,
			concurrentCycleInInterval / divisor,
			averageConcurrentFreed / divisor,
			averageConcurrentTime / divisor,
			overallCyclesInInterval / divisor,
			averageOverallFreed / divisor,
			averageOverallTime / divisor
		);
	}
}
