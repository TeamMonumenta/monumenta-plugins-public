package com.playmonumenta.plugins.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.plugins.Plugin;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

public class ShardHealthUtils {
	private static final AtomicBoolean createdAutoHeapDump = new AtomicBoolean(false);

	/**
	 * Pause execution until shard performance improves enough to continue
	 * If the health starts in good condition, continues immediately
	 * This version attempts to provide sane defaults
	 * @param audience The players to notify of paused execution via actionbar messages
	 * @return A CompletableFuture to wait for completion of before continuing
	 */
	public static CompletableFuture<Void> awaitShardHealth(@Nullable Audience audience) {
		return awaitShardHealth(
			audience,
			0.7,
			ShardHealthUtils.ShardHealth.defaultTargetHealth(),
			10L, // Half a second
			300L // 15 seconds
		);
	}

	/**
	 * Pause execution until shard performance improves enough to continue
	 * If the health starts in good condition, continues immediately
	 * @param audience               The players to notify of paused execution via actionbar messages
	 * @param minHealthScore         The minimum overall health required to continue;
	 *                                  1.0 is impossibly good, 0.0 is lagging or dead
	 * @param minTargetHealth        The specific health score targets to meet
	 * @param maintainHealthForTicks The number of ticks the shard must remain healthy before the pause ends; 0 to disable
	 * @param heapDumpAfterTicks     If memory is an issue for this many ticks, create a heap dump; -1L to disable
	 * @return A CompletableFuture to wait for completion of before continuing
	 */
	public static CompletableFuture<Void> awaitShardHealth(
		@Nullable Audience audience,
		double minHealthScore,
		@Nullable ShardHealth minTargetHealth,
		long maintainHealthForTicks,
		long heapDumpAfterTicks
	) {
		CompletableFuture<Void> future = new CompletableFuture<>();

		if (audience == null) {
			audience = Audience.empty();
		}
		Audience finalAudience = audience;

		if (minTargetHealth == null) {
			minTargetHealth = ShardHealth.unacceptableTargetHealth();
		}
		ShardHealth finalMinTargetHealth = minTargetHealth;

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			boolean firstRun = true;
			long healthyTicksRemaining = maintainHealthForTicks;
			long heapDumpTicksRemaining = heapDumpAfterTicks;
			while (true) {
				List<Component> delayBecauseOf = new ArrayList<>();

				ShardHealth currentHealth = ShardHealth.currentHealth();

				if (currentHealth.healthScore() < minHealthScore) {
					delayBecauseOf.add(Component.text("low overall shard health"));
				}

				if (currentHealth.memoryHealth() < finalMinTargetHealth.memoryHealth()) {
					delayBecauseOf.add(Component.text("low memory"));
					heapDumpTicksRemaining--;
					if (heapDumpTicksRemaining == 0 && !createdAutoHeapDump.get()) {
						createdAutoHeapDump.set(true);
						Bukkit.getScheduler().runTask(Plugin.getInstance(),
							() -> Bukkit.getServer().dispatchCommand(
								Bukkit.getServer().getConsoleSender(),
								"spark heapdump"
							));
					}
				} else {
					heapDumpTicksRemaining = heapDumpAfterTicks;
				}

				if (currentHealth.tickHealth() < finalMinTargetHealth.tickHealth()) {
					delayBecauseOf.add(Component.text("low mspt"));
				}

				if (delayBecauseOf.isEmpty()) {
					healthyTicksRemaining--;
					if (firstRun || healthyTicksRemaining <= 0L) {
						break;
					}
					delayBecauseOf.add(Component.text("health verification"));
				} else {
					firstRun = false;
					healthyTicksRemaining = maintainHealthForTicks;
				}

				JoinConfiguration joinConfig;
				if (delayBecauseOf.size() == 2) {
					joinConfig = JoinConfiguration.separator(Component.text(" and "));
				} else {
					joinConfig = JoinConfiguration.separators(
						Component.text(", "),
						Component.text(", and ")
					);
				}

				finalAudience.sendActionBar(Component.text(
						"Delaying due to ",
						NamedTextColor.YELLOW
					)
					.append(Component.join(
						joinConfig,
						delayBecauseOf
					)));

				CompletableFuture<Void> delayThreadFuture = new CompletableFuture<>();
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(),
					() -> delayThreadFuture.complete(null), 1L);
				delayThreadFuture.join();
			}

			future.complete(null);
		});

		return future;
	}

	/**
	 * Returns a combined score of server performance
	 * @return Combined server performance, 1.0 is impossible good
	 */
	public static double serverHealthScore() {
		return unallocatedMemoryPercent() * averageTickUnusedPercent();
	}

	/**
	 * Check how much memory is unallocated
	 * @return How much memory is unallocated on a scale of 0.0 to 1.0
	 */
	public static double unallocatedMemoryPercent() {
		Runtime runtime = Runtime.getRuntime();
		return Math.max(0.0, (double) runtime.totalMemory() / runtime.maxMemory());
	}

	/**
	 * Returns what percent of a tick is in use; 1.0 or higher indicates lag, anything less is doing ok
	 * @return The average percent of tick usage on a scale of 0.0 to 1.0 when not lagging
	 */
	public static double averageTickUsagePercent() {
		return Bukkit.getServer().getAverageTickTime() / 50.0;
	}

	/**
	 * Returns what percent of a tick is not being used - will be 0 if ticks take too long!
	 * @return The average percent of a tick that is not being used on a scale of 0.0 to 1.0
	 */
	public static double averageTickUnusedPercent() {
		return Math.max(0.0, 1.0 - averageTickUsagePercent());
	}

	public static class ShardHealth {
		private @Nullable Double mHealthScore = null;
		private double mMemoryHealth;
		private double mTickHealth;

		private ShardHealth(double memoryHealth, double tickHealth) {
			mMemoryHealth = memoryHealth;
			mTickHealth = tickHealth;
		}

		public static ShardHealth currentHealth() {
			return new ShardHealth(
				unallocatedMemoryPercent(),
				averageTickUnusedPercent()
			);
		}

		public static ShardHealth defaultTargetHealth() {
			return new ShardHealth(
				0.7,
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
}
