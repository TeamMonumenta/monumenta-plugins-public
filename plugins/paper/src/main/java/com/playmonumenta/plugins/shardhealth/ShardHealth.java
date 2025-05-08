package com.playmonumenta.plugins.shardhealth;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.plugins.shardhealth.g1.G1GcHealth;
import java.util.Iterator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ShardHealth implements ComponentLike {
	private @Nullable Double mHealthScore = null;
	private double mMemoryHealth;
	private double mTickHealth;
	private @Nullable G1GcHealth mGcHealth;

	protected ShardHealth(double memoryHealth, double tickHealth, @Nullable G1GcHealth gcHealth) {
		mMemoryHealth = memoryHealth;
		mTickHealth = tickHealth;
		mGcHealth = gcHealth;
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
			aveTickUnused / actualTicks,
			ShardHealthManager.G1_LISTENER.getHealth()
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
			ShardHealthManager.lastTickUnusedPercent(),
			ShardHealthManager.G1_LISTENER.getHealth()
		);
	}

	public static ShardHealth defaultTargetHealth() {
		return new ShardHealth(
			0.3,
			0.7,
			null
		);
	}

	public static ShardHealth unacceptableTargetHealth() {
		return new ShardHealth(
			0.0,
			0.0,
			null
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

		result.gcHealth(G1GcHealth.fromJson(object.get("gcHealth")));

		return result;
	}

	public JsonObject toJson() {
		JsonObject result = new JsonObject();

		result.addProperty("healthScore", healthScore());
		result.addProperty("memoryHealth", memoryHealth());
		result.addProperty("tickHealth", tickHealth());

		if (gcHealth() != null) {
			result.add("gcHealth", gcHealth().toJson());
		}

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

	public ShardHealth gcHealth(@Nullable G1GcHealth health) {
		mGcHealth = health;
		return this;
	}

	@Contract(pure = true)
	@Nullable
	public G1GcHealth gcHealth() {
		return mGcHealth;
	}

	@Override
	public @NotNull Component asComponent() {
		return Component.empty()
			.append(Component.text(String.format("%5.1f%% Shard Health (lags/crashes at 0, only devs see this)", 100 * healthScore())))
			.append(Component.newline())
			.append(Component.text(String.format("- %5.1f%% Memory Available", 100 * memoryHealth())))
			.append(Component.newline())
			.append(Component.text(String.format("- %5.1f%% Tick Spent Idle", 100 * tickHealth())))
			;
	}
}
