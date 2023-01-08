package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.FunctionArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class RedeemVoteRewards extends GenericCommand {
	private static final String VOTES_UNCLAIMED = "votesUnclaimed";

	public static void register(Plugin plugin) {
		List<Argument<?>> arguments = new ArrayList<>();

		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new StringArgument("scoreboard"));
		arguments.add(new FunctionArgument("function"));

		new CommandAPICommand("redeemvoterewards")
			.withPermission(CommandPermission.fromString("monumenta.command.redeemvoterewards"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				run(plugin, (Player)args[0], (String)args[1], (FunctionWrapper[])args[2]);
			})
			.register();
	}

	private static void run(Plugin plugin, Player player, String scoreboardName, FunctionWrapper[] functions) {
		MonumentaRedisSyncAPI.runOnMainThreadWhenComplete(plugin, MonumentaRedisSyncAPI.remoteDataGet(player.getUniqueId(), VOTES_UNCLAIMED), (data, ex) -> {
			if (ex != null) {
				MessagingUtils.sendError(player, "Failed to get unclaimed vote rewards: " + ex.getMessage());
			} else {
				int amountAvailable = 0;
				if (data != null) {
					try {
						amountAvailable = Integer.parseInt(data);
					} catch (Exception e) {
						MessagingUtils.sendError(player, "Failed to parse unclaimed vote rewards as int: " + data);
						return;
					}
				}

				if (!player.isOnline() || !player.isValid()) {
					// Silently abort, the player left
					return;
				}

				if (amountAvailable <= 0) {
					// No vote rewards - abort early. Still run the function so that it can tell the player there were none
					ScoreboardUtils.setScoreboardValue(player, scoreboardName, 0);
					for (FunctionWrapper func : functions) {
						func.runAs(player);
					}
					return;
				}

				// amountAvailable stores last snapshot value at this point, which might have been modified since retrieved
				final int amountRedeemed = amountAvailable;

				// Now try to atomically remove that many unclaimed rewards from that value
				MonumentaRedisSyncAPI.runOnMainThreadWhenComplete(plugin, MonumentaRedisSyncAPI.remoteDataIncrement(player.getUniqueId(), VOTES_UNCLAIMED, -1 * amountRedeemed), (resultRemaining, resultEx) -> {
					if (resultEx != null) {
						MessagingUtils.sendError(player, "Failed to decrement unclaimed vote rewards: " + resultEx.getMessage());
					} else {
						if (resultRemaining == null) {
							MessagingUtils.sendError(player, "Somehow got null despite decrementing vote value - maybe try again?");
							return;
						}

						if (resultRemaining < 0 || !player.isOnline() || !player.isValid()) {
							// This can happen if you try to claim the rewards twice at the same time - need to abort this attempt and re-add the values
							if (player.isOnline()) {
								MessagingUtils.sendError(player, "Got negative remaining vote rewards after redeeming - be patient and only try once at a time");
							}
							// Put the amount redeemed back to redis
							MonumentaRedisSyncAPI.remoteDataIncrement(player.getUniqueId(), VOTES_UNCLAIMED, amountRedeemed);
						} else {
							// Hooray, successfully claimed rewards
							ScoreboardUtils.setScoreboardValue(player, scoreboardName, amountRedeemed);
							for (FunctionWrapper func : functions) {
								func.runAs(player);
							}
						}
					}
				});
			}
		});
	}
}
