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
}
