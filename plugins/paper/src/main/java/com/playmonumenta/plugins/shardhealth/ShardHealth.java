package com.playmonumenta.plugins.shardhealth;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Iterator;
import org.jetbrains.annotations.Nullable;

public class ShardHealth {
	private @Nullable Double mHealthScore = null;
	private double mMemoryHealth;
	private double mTickHealth;

	private ShardHealth(double memoryHealth, double tickHealth) {
		mMemoryHealth = memoryHealth;
		mTickHealth = tickHealth;
	}

	/**
	 * Gets the shard's average of the shard's current health
	 * over a reasonable number of ticks.
	 *
	 * @return The ShardHealth over a reasonable average time
	 */
	public static ShardHealth averageHealth() {
		return averageHealth(ShardHealthManager.REASONABLE_AVERAGE_DURATION_TICKS);
	}

	/**
	 * Gets the shard's average health over the last specified number of ticks
	 * If there aren't that many ticks available, the available average is used.
	 *
	 * @return The shard's health over a specified number of ticks
	 */
	public static ShardHealth averageHealth(int ticks) {
		Iterator<ShardHealth> previousHealthIt = ShardHealthManager.previousInstantHealthIterator();
		int remainingTicks = ticks;
		int actualTicks = 0;
		double aveMem = 0.0;
		double aveTickUnused = 0.0;
		while (remainingTicks > 0 && previousHealthIt.hasNext()) {
			remainingTicks--;
			actualTicks++;
			ShardHealth prevHealth = previousHealthIt.next();
			aveMem += prevHealth.mMemoryHealth;
			aveTickUnused += prevHealth.mTickHealth;
		}

		if (actualTicks <= 0) {
			actualTicks = 1;
		}

		return new ShardHealth(
			aveMem / actualTicks,
			aveTickUnused / actualTicks
		);
	}

	/**
	 * Gets a shard's current health from the latest tick's available information.
	 *
	 * @return The latest ShardHealth for the current tick
	 */
	public static ShardHealth instantHealth() {
		return new ShardHealth(
			ShardHealthManager.unallocatedMemoryPercent(),
			ShardHealthManager.lastTickUnusedPercent()
		);
	}

	public static ShardHealth defaultTargetHealth() {
		return new ShardHealth(
			0.4,
			0.7
		);
	}

	public static ShardHealth unacceptableTargetHealth() {
		return new ShardHealth(
			0.0,
			0.0
		);
	}

	public static ShardHealth fromJson(JsonObject object) {
		ShardHealth result = defaultTargetHealth();

		if (
			object.get("healthScore") instanceof JsonPrimitive healthScorePrimitive
				&& healthScorePrimitive.isNumber()
		) {
			result.healthScore(healthScorePrimitive.getAsDouble());
		}

		if (
			object.get("memoryHealth") instanceof JsonPrimitive memoryHealthPrimitive
				&& memoryHealthPrimitive.isNumber()
		) {
			result.memoryHealth(memoryHealthPrimitive.getAsDouble());
		}

		if (
			object.get("tickHealth") instanceof JsonPrimitive tickHealthPrimitive
				&& tickHealthPrimitive.isNumber()
		) {
			result.tickHealth(tickHealthPrimitive.getAsDouble());
		}

		return result;
	}

	public JsonObject toJson() {
		JsonObject result = new JsonObject();

		result.addProperty("healthScore", healthScore());
		result.addProperty("memoryHealth", memoryHealth());
		result.addProperty("tickHealth", tickHealth());

		return result;
	}

	public double memoryHealth() {
		return mMemoryHealth;
	}

	public ShardHealth memoryHealth(double memoryHealth) {
		mMemoryHealth = memoryHealth;
		return this;
	}

	public double tickHealth() {
		return mTickHealth;
	}

	public ShardHealth tickHealth(double tickHealth) {
		mTickHealth = tickHealth;
		return this;
	}

	public double healthScore() {
		if (mHealthScore != null) {
			return mHealthScore;
		}
		return mMemoryHealth * mTickHealth;
	}

	public ShardHealth healthScore(@Nullable Double healthScore) {
		mHealthScore = healthScore;
		return this;
	}
}
