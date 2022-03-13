package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.FunctionArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.entity.Player;

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

	public static void register(Logger logger) {
		List<Argument> arguments = new ArrayList<>();

		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));
		arguments.add(new StringArgument("scoreboard"));
		arguments.add(new FunctionArgument("function"));

		new CommandAPICommand("redeemvoterewards")
			.withPermission(CommandPermission.fromString("monumenta.command.redeemvoterewards"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				run(logger, (Player)args[0], (String)args[1], (FunctionWrapper[])args[2]);
			})
			.register();
	}

	private static void run(Logger logger, Player player, String scoreboardName, FunctionWrapper[] functions) throws WrapperCommandSyntaxException {
		PendingRewardContext context = new PendingRewardContext(player, scoreboardName, functions);

		mPendingRewards.put(player.getUniqueId(), context);

		// Count = 0 means request count
		MonumentaNetworkRelayIntegration.sendGetVotesUnclaimedPacket(player.getUniqueId(), 0);

		logger.info("Requested vote rewards for " + player.getName());
	}

	public static void gotVoteRewardMessage(Logger logger, UUID uuid, int rewardCount) {
		PendingRewardContext context = mPendingRewards.get(uuid);

		if (context != null) {
			context.run(rewardCount);
			mPendingRewards.remove(uuid);
		} else if (rewardCount > 0) {
			MonumentaNetworkRelayIntegration.sendGetVotesUnclaimedPacket(uuid, rewardCount);
			logger.info("Sending " + Integer.toString(rewardCount) + " votes back to bungee for " + uuid.toString());
		}
	}
}
