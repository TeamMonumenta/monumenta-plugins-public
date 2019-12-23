package com.playmonumenta.plugins.commands;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.packets.BungeeGetVotesUnclaimedPacket;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.FunctionWrapper;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.FunctionArgument;
import io.github.jorelali.commandapi.api.arguments.StringArgument;

/*
 * This is obnoxiously complicated.
 *
 * 1) A command is run on the player like: /redeemvoterewards @p temp monumenta:reward_function
 * 2) The run() method executes here to handle that command
 * 3) The function is cached here, and the player's UUID is sent to bungee to retrieve rewards
 * 4) Bungee retrieves the vote rewards and sends back a message with the player's UUID and reward count
 * 5) The player's reward count is saved into the specified scoreboard and the function is retrieved and executed
 */
public class RedeemVoteRewards extends GenericCommand {
	private static class PendingRewardContext {
		private final Player mPlayer;
		private final String mScoreboardName;
		private final FunctionWrapper[] mFunctions;

		private PendingRewardContext(Player player, String scoreboardName, FunctionWrapper[] functions) {
			mPlayer = player;
			mScoreboardName = scoreboardName;
			mFunctions = functions;
		}

		private void run(int rewardCount) {
			if (mPlayer.isOnline() && mPlayer.isValid()) {
				ScoreboardUtils.setScoreboardValue(mPlayer, mScoreboardName, rewardCount);
				for (FunctionWrapper func : mFunctions) {
					func.runAs(mPlayer);
				}
			}
		}
	}

	private static final Map<UUID, PendingRewardContext> mPendingRewards = new HashMap<UUID, PendingRewardContext>();
	public static void register(Plugin plugin) {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("player", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));
		arguments.put("scoreboard", new StringArgument());
		arguments.put("function", new FunctionArgument());

		CommandAPI.getInstance().register("redeemvoterewards",
		                                  CommandPermission.fromString("monumenta.command.redeemvoterewards"),
		                                  arguments,
		                                  (sender, args) -> {
											  run(plugin, (Player)args[0], (String)args[1], (FunctionWrapper[])args[2]);
		                                  }
		);
	}

	private static void run(Plugin plugin, Player player, String scoreboardName, FunctionWrapper[] functions) throws CommandSyntaxException {
		PendingRewardContext context = new PendingRewardContext(player, scoreboardName, functions);

		mPendingRewards.put(player.getUniqueId(), context);

		// Count = 0 means request count
		plugin.mSocketManager.sendPacket(new BungeeGetVotesUnclaimedPacket(player.getUniqueId(), 0));

		plugin.getLogger().info("Requested vote rewards for " + player.getName());
	}

	public static void gotVoteRewardMessage(Plugin plugin, UUID uuid, int rewardCount) {
		PendingRewardContext context = mPendingRewards.get(uuid);

		if (context != null) {
			context.run(rewardCount);
			mPendingRewards.remove(uuid);
		} else if (rewardCount > 0) {
			plugin.mSocketManager.sendPacket(new BungeeGetVotesUnclaimedPacket(uuid, rewardCount));
			plugin.getLogger().info("Sending " + Integer.toString(rewardCount) + " votes back to bungee for " + uuid.toString());
		}
	}
}
