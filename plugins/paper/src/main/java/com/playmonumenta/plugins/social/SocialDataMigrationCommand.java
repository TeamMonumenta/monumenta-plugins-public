package com.playmonumenta.plugins.social;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.RedisAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;

public class SocialDataMigrationCommand {
	// TODO: remove this file after migration is complete

	public static void register() {
		new CommandAPICommand("socialdatamigration")
			.withSubcommand(new CommandAPICommand("player")
				.withArguments(new StringArgument("player"))
				.executesConsole((console, args) -> {
					Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
						String playerName = (String) args.get("player");
						UUID playerUuid;

						try {
							playerUuid = StringUtils.getUuidFromInput(playerName);
						} catch (WrapperCommandSyntaxException exception) {
							throw new RuntimeException(exception);
						}

						MMLog.info("[Social Migration] Starting social data migration for: " + playerName + " | " + playerUuid);

						performMigration("listOfFriends", "friends", playerUuid);
						performMigration("listOfBlockedPlayers", "blocked", playerUuid);
						deleteSocialType("listOfPendingFriendRequests", playerUuid);

						MMLog.info("[Social Migration] Finished migration for: " + playerName + " | " + playerUuid);
					});
				})
			)

			.withSubcommand(new CommandAPICommand("all")
				.executesConsole((console, args) -> {
					Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
						MMLog.info("[Social Migration] Starting migration for all players...");

						Set<UUID> allUuids = MonumentaRedisSyncAPI.getAllCachedPlayerUuids();
						int total = allUuids.size();
						int count = 0;

						for (UUID playerUuid : allUuids) {
							String playerName = SocialManager.resolveName(playerUuid);
							MMLog.info("[Social Migration] Migrating " + ++count + "/" + total + ": " + playerName + " | " + playerUuid);

							performMigration("listOfFriends", "friends", playerUuid);
							performMigration("listOfBlockedPlayers", "blocked", playerUuid);
							deleteSocialType("listOfPendingFriendRequests", playerUuid);
						}

						MMLog.info("[Social Migration] Finished migrating all players.");
					});
				})
			)

			.register();
	}

	private static void performMigration(String oldType, String newType, UUID playerUuid) {
		String oldKey = "socialData:" + oldType + ":" + playerUuid;
		String newKey = "social:" + newType + ":" + playerUuid;

		Map<String, String> oldData = RedisAPI.getInstance().async().hgetall(oldKey).toCompletableFuture().join();
		if (oldData == null || oldData.isEmpty()) {
			MMLog.info("[Social Migration] No data found under: " + oldKey);
			return;
		}

		Map<String, String> newData = new HashMap<>();

		for (Map.Entry<String, String> entry : oldData.entrySet()) {
			try {
				String oldRedisKey = entry.getKey();       // friendsWith|<UUID>
				String oldRedisValue = entry.getValue();   // <UUID>|<timestamp> or <UUID>

				UUID targetUuid = UUID.fromString(oldRedisKey.substring(oldRedisKey.indexOf("|") + 1));

				String timestamp;
				if (oldRedisValue.contains("|")) {
					timestamp = oldRedisValue.substring(oldRedisValue.indexOf("|") + 1);
				} else {
					timestamp = DateTimeFormatter.ISO_INSTANT.format(
						DateUtils.localDateTime(2025, 3, 6).toInstant(ZoneOffset.UTC)
					);

					RedisAPI.getInstance().async().hset(oldKey, oldRedisKey, targetUuid + "|" + timestamp).toCompletableFuture().join();
					MMLog.warning("[Social Migration] Repaired missing timestamp for: " + oldRedisKey);
				}

				newData.put(targetUuid.toString(), timestamp);
			} catch (Exception exception) {
				MMLog.warning("[Social Migration] Failed to convert entry: " + entry + " (Skipping)");
			}
		}

		if (!newData.isEmpty()) {
			RedisAPI.getInstance().async().hmset(newKey, newData).toCompletableFuture().join();
			MMLog.info("[Social Migration] Migrated " + newData.size() + " entries from " + oldKey + " to " + newKey);
		}

		RedisAPI.getInstance().async().del(oldKey).toCompletableFuture().join();
	}

	private static void deleteSocialType(String oldType, UUID playerUuid) {
		String oldKey = "socialData:" + oldType + ":" + playerUuid;

		Map<String, String> oldData = RedisAPI.getInstance().async().hgetall(oldKey).toCompletableFuture().join();
		if (oldData != null && !oldData.isEmpty()) {
			RedisAPI.getInstance().async().del(oldKey).toCompletableFuture().join();
			MMLog.info("[Social Migration] Deleted old pending request data under: " + oldKey);
		}
	}
}
