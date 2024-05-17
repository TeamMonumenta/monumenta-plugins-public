package com.playmonumenta.plugins.commands;

import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.DungeonUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.PlayerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class ShardSorterCommand {

	public enum ShardType {
		time,
		instance
	}

	public static class ShardSorterData {
		public String mShardName;
		public ShardType mShardType;

		public ShardSorterData(String shardName, ShardType shardType) {
			mShardName = shardName;
			mShardType = shardType;
		}
	}

	private static final List<ShardSorterData> SHARD_SORTER_DATA = new ArrayList<>(
		List.of(
			new ShardSorterData("ring", ShardType.time),
			new ShardSorterData("isles", ShardType.time),
			new ShardSorterData("valley", ShardType.time)
		)
	);

	public static void register() {
		new CommandAPICommand("sort-to-shard")
			.withPermission(CommandPermission.fromString("monumenta.command.sort-to-shard"))
			.withArguments(new StringArgument("shard")
				.replaceSuggestions(ArgumentSuggestions.stringCollection(
					info -> {
						List<String> shards = new ArrayList<>();
						for (ShardSorterData shardSorterData : SHARD_SORTER_DATA) {
							shards.add(shardSorterData.mShardName);
						}
						for (DungeonUtils.DungeonCommandMapping mapping : DungeonUtils.DungeonCommandMapping.values()) {
							String shardName = mapping.getShardName();
							if (shardName != null) {
								shards.add(shardName);
							}
						}
						return shards;
					})))
			.withArguments(new PlayerArgument("target"))
			.executes((sender, args) -> {
				sortToShard(args.getUnchecked("target"), args.getUnchecked("shard"), sender);
			})
			.register();
	}

	public static void sortToShard(Player player, String shard, @Nullable CommandSender sender) throws WrapperCommandSyntaxException {
		if (shard == null || shard.isEmpty()
			    || ServerProperties.getShardName().startsWith(shard)) {
			if (sender != null) {
				sender.sendMessage("No shard specified or player is already on shard");
			}
			return;
		}
		ShardType shardType = null;
		for (ShardSorterData shardSorterData : SHARD_SORTER_DATA) {
			if (shardSorterData.mShardName.equals(shard)) {
				shardType = shardSorterData.mShardType;
				break;
			}
		}
		if (shardType == null) {
			DungeonAccessCommand.send(player, shard);
		} else {
			List<String> possibleShards;
			try {
				possibleShards = NetworkRelayAPI.getOnlineShardNames().stream().filter(shardName -> shardName.startsWith(shard)).toList();
			} catch (Exception e) {
				MMLog.severe("Failed to get shard names from network relay for sort-to-shard command, user="
					             + player.getName() + ", shard=" + shard + " error=" + e.getMessage());
				return;
			}
			int shardCount = possibleShards.size();
			if (shardCount == 0) {
				if (sender != null) {
					sender.sendMessage("No shards found for " + shard);
				}
				return;
			}
			possibleShards = sortShardNames(possibleShards, shard);
			String worldName = player.getWorld().getName();
			int instanceNumber;
			if (!worldName.contains("plot")) {
				instanceNumber = StringUtils.getEndingInteger(worldName);
			} else {
				instanceNumber = -1;
			}
			int index;
			if (instanceNumber > 0) {
				index = instanceNumber % shardCount;
			} else {
				long minute = DateUtils.getSecondsSinceEpoch() / 60L;
				index = (int) (minute % shardCount);
			}
			String targetShard = possibleShards.get(index);
			if (sender != null) {
				sender.sendMessage("Sending " + player.getName() + " to shard " + targetShard);
			}
			try {
				MonumentaRedisSyncAPI.sendPlayer(player, targetShard);
			} catch (Exception e) {
				MMLog.severe("Failed to send player to shard for sort-to-shard command, user="
					+ player.getName() + ", shard=" + shard + ", targetShard=" + targetShard + " error=" + e);
			}
		}
	}

	public static List<String> sortShardNames(List<String> shardNames, String baseShardName) {
		ArrayList<Integer> resultSortedList = new ArrayList<>();
		for (String shard : shardNames) {
			if (shard.equalsIgnoreCase(baseShardName)) {
				resultSortedList.add(0);
			} else {
				resultSortedList.add(Integer.parseInt(shard.split("-")[1]));
			}
		}
		Collections.sort(resultSortedList);

		String shardName;
		List<String> result = new ArrayList<>();
		for (Integer shard : resultSortedList) {
			if (shard == 0) {
				shardName = baseShardName;
			} else {
				shardName = baseShardName + "-" + shard;
			}
			result.add(shardName);
		}
		return result;
	}
}
