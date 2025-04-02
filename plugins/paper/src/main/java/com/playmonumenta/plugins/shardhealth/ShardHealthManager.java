package com.playmonumenta.plugins.shardhealth;

import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.TextArgument;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class ShardHealthManager {
	public static final int REASONABLE_AVERAGE_DURATION_TICKS = 5 * 20;
	public static final int MAX_TICKS_FOR_AVERAGES = 60 * 20;
	private static final long TICK_EXPECTED_NANOS = 50_000_000L;

	private static final double HEAP_DUMP_MEMORY_HEALTH_THRESHOLD = 0.15;
	private static final int HEAP_DUMP_AFTER_TICKS = 20 * 60 * 20;
	private static boolean mCreatedAutoHeapDump = false;

	private static @Nullable BukkitRunnable mRunnable = null;
	private static int mRotatingShardHealthLastUpdate = 0;
	private static final List<ShardHealth> mRotatingShardHealth = new ArrayList<>();

	public static void registerCommands() {
		CommandAPICommand instantSubcommand = new CommandAPICommand("instant")
			.executes((sender, args) -> {
				sender.sendMessage(Component.text("", NamedTextColor.GOLD).append(ShardHealth.instantHealth()));
			});

		CommandAPICommand averageSubcommand = new CommandAPICommand("average")
			.executes((sender, args) -> {
				sender.sendMessage(Component.text("", NamedTextColor.GOLD).append(ShardHealth.averageHealth()));
			});

		CommandAPICommand averageDebugSubcommand = new CommandAPICommand("averagedebug")
			.executes((sender, args) -> {
				sender.sendMessage(Component.text("Rotating shard health length: " + mRotatingShardHealth.size(), NamedTextColor.GOLD));
				int i = 0;
				Iterator<ShardHealth> it = previousInstantHealthIterator();
				while (it.hasNext()) {
					i++;
					it.next();
				}
				sender.sendMessage(Component.text("Iterator size: " + i, NamedTextColor.GOLD));
			});

		CommandAPICommand remoteSubcommand = new CommandAPICommand("remote")
			.withArguments(new TextArgument("shard")
				.replaceSuggestions(ArgumentSuggestions.strings(
					info -> NetworkRelayAPI.getOnlineShardNames().toArray(String[]::new))))
			.executes((sender, args) -> {
				String shard = args.getUnchecked("shard");
				ShardHealth shardHealth = MonumentaNetworkRelayIntegration.remoteShardHealth(shard);
				if (shardHealth == null) {
					sender.sendMessage(Component.text("No such shard " + shard, NamedTextColor.RED));
					return;
				}
				sender.sendMessage(Component.text("", NamedTextColor.GOLD).append(shardHealth));
			});

		new CommandAPICommand("shardhealth")
			.withPermission("monumenta.command.shardhealth")
			.withSubcommand(instantSubcommand)
			.withSubcommand(averageSubcommand)
			.withSubcommand(averageDebugSubcommand)
			.withSubcommand(remoteSubcommand)
			.register();
	}

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
			0.2,
			ShardHealth.defaultTargetHealth(),
			10L, // Maintain health for half a second
			10 // Average over half a second
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
	 * @param averageOverTicks       Get the average shard health over this many ticks
	 * @return A CompletableFuture to wait for completion of before continuing
	 */
	public static CompletableFuture<Void> awaitShardHealth(
		@Nullable Audience audience,
		double minHealthScore,
		@Nullable ShardHealth minTargetHealth,
		long maintainHealthForTicks,
		int averageOverTicks
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
			while (true) {
				List<Component> delayBecauseOf = new ArrayList<>();

				ShardHealth currentHealth = ShardHealth.averageHealth(averageOverTicks);

				if (currentHealth.healthScore() < minHealthScore) {
					delayBecauseOf.add(Component.text("low overall shard health"));
				}

				if (currentHealth.memoryHealth() < finalMinTargetHealth.memoryHealth()) {
					delayBecauseOf.add(Component.text("low memory"));
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
	 * @return Combined server performance, 1.0 is impossibly good
	 */
	public static double serverHealthScore() {
		return ShardHealth.averageHealth().healthScore();
	}

	/**
	 * Check how much memory is unallocated
	 * @return How much memory is unallocated on a scale of 0.0 to 1.0
	 */
	public static double unallocatedMemoryPercent() {
		// Major thanks to https://stackoverflow.com/users/158847/leebutts on StackOverflow:
		// https://stackoverflow.com/a/16517624

		Runtime runtime = Runtime.getRuntime();
		long totalMemory = runtime.totalMemory(); // current heap allocated to the VM process
		long freeMemory = runtime.freeMemory(); // out of the current heap, how much is free
		long maxMemory = runtime.maxMemory(); // Max heap VM can use e.g. Xmx setting
		long usedMemory = totalMemory - freeMemory; // how much of the current heap the VM is using
		long availableMemory = maxMemory - usedMemory; // available memory i.e. Maximum heap size minus the current amount used

		return Math.max(0.0, (double) availableMemory / maxMemory);
	}

	/**
	 * Returns what percent of the last tick was not used - will be equal to 0 if that tick took too long!
	 * Note that the current tick's info isn't available until the next tick.
	 * @return The percent of the last tick that is not being used on a scale of 0.0 to 1.0
	 */
	public static double lastTickUnusedPercent() {
		long[] tickTimes = Bukkit.getServer().getTickTimes();
		int numTickTimes = tickTimes.length;
		int currentTick = Bukkit.getCurrentTick();
		long lastTickLength = Math.min(tickTimes[(numTickTimes + currentTick - 1) % numTickTimes], TICK_EXPECTED_NANOS);
		return (double) (TICK_EXPECTED_NANOS - lastTickLength) / TICK_EXPECTED_NANOS;
	}

	public static void startRunningAverageClock(Plugin plugin) {
		if (mRunnable != null) {
			return;
		}

		mRunnable = new BukkitRunnable() {
			int mTicksUntilHeapDump = HEAP_DUMP_AFTER_TICKS;

			@Override
			public void run() {
				ShardHealth instantHealth = ShardHealth.instantHealth();

				// First tick (0) has no data to record
				int lastTick = Bukkit.getCurrentTick() - 1;
				if (lastTick < 0) {
					return;
				}

				// Intentionally 1 higher to avoid issues with async code
				int maxSizeForAverages = MAX_TICKS_FOR_AVERAGES + 1;
				if (mRotatingShardHealth.size() < maxSizeForAverages) {
					mRotatingShardHealth.add(instantHealth);
					mRotatingShardHealthLastUpdate = lastTick;
				} else {
					int writeIndex = lastTick % maxSizeForAverages;
					mRotatingShardHealth.set(writeIndex, instantHealth);
					mRotatingShardHealthLastUpdate = writeIndex;
				}

				// Memory check for automated heap dumps
				if (instantHealth.memoryHealth() >= HEAP_DUMP_MEMORY_HEALTH_THRESHOLD) {
					mTicksUntilHeapDump = HEAP_DUMP_AFTER_TICKS;
				} else {
					mTicksUntilHeapDump--;
					if (mTicksUntilHeapDump == 0 && !mCreatedAutoHeapDump) {
						mCreatedAutoHeapDump = true;
						Bukkit.getServer().dispatchCommand(
							Bukkit.getServer().getConsoleSender(),
							"spark heapdump"
						);
						MonumentaNetworkRelayIntegration.sendAdminMessage("<" + ServerProperties.getShardName() + "> Automatic heap dump due to low memory");

						// TODO Schedule a restart; details not yet determined, but we don't want to kick people from strikes by accident
					}
				}
			}
		};

		mRunnable.runTaskTimer(plugin, 0L, 1L);
	}

	public static void stopRunningAverageClock() {
		if (mRunnable == null) {
			return;
		}
		mRunnable.cancel();
		mRunnable = null;
	}

	/**
	 * Returns an iterator of shard health for each of the previous ticks, starting with the previous tick and working backwards.
	 * Stops iterating when out of ticks with health data, or when the rotating list catches up with the last written tick
	 * @return An iterator of previous shard health
	 */
	public static Iterator<ShardHealth> previousInstantHealthIterator() {
		int initialRawIndex = mRotatingShardHealthLastUpdate - 1;
		if (initialRawIndex < 0) {
			initialRawIndex += mRotatingShardHealth.size();
		}
		int finalInitialRawIndex = initialRawIndex;

		return new Iterator<>() {
			int mRemaining = mRotatingShardHealth.size() - 1;
			int mRawIndex = finalInitialRawIndex;

			@Override
			public boolean hasNext() {
				return mRemaining > 0 && mRawIndex != mRotatingShardHealthLastUpdate;
			}

			@Override
			public ShardHealth next() {
				ShardHealth result = mRotatingShardHealth.get(mRawIndex);
				mRawIndex--;
				if (mRawIndex < 0) {
					mRawIndex += mRotatingShardHealth.size();
				}
				mRemaining--;
				return result;
			}
		};
	}
}
