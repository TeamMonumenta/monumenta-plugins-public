package com.playmonumenta.plugins.command.commands;

import com.playmonumenta.plugins.command.AbstractCommand;
import com.playmonumenta.plugins.command.CommandContext;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

public class GetScore extends AbstractCommand {

	public GetScore(Plugin plugin) {
		super(
		    "getScore",
		    "Gets a scoreboard value. Can be run on/by players or entities via /execute.",
		    plugin
		);
	}

	@Override
	protected void configure(ArgumentParser parser) {
		parser.addArgument("objective")
		.help("name of the objective");
		parser.addArgument("name")
		.help("name of the entity/player")
		.nargs("?");
	}

	@Override
	protected boolean run(CommandContext context) {
		final String objective = context.getNamespace().get("objective");
		String targetName = getTargetNameFromContext(context);

		if (targetName == null) {
			sendErrorMessage(context, "Unable to determine execute target");
			return false;
		}

		Optional<Integer> scoreboardValue = ScoreboardUtils.getScoreboardValue(targetName, objective);
		if (!scoreboardValue.isPresent()) {
			sendErrorMessage(context, "Unable to get score '" + objective + "' for '" + targetName + "'");
			return false;
		}

		sendMessage(context, ChatColor.GREEN + targetName + "'s score for '" + objective + "': " + scoreboardValue.get());
		return true;
	}

	/**
	 * Attempts to get the target name in the following order:
	 *
	 * 1) Optional name argument
	 * 2) Player sending command
	 * 3) Proxied player
	 * 4) Proxied Entity
	 *
	 * @param context contains all command related data
	 * @return the target name or null if none found
	 */
	private String getTargetNameFromContext(CommandContext context) {
		String targetName = context.getNamespace().get("name");

		if (targetName == null) {
			final Optional<Player> player = context.getPlayer();
			final CommandSender sender = context.getSender();

			if (player.isPresent()) {
				targetName = player.get().getName();
			} else if (context.isProxiedSender()) {
				CommandSender callee = ((ProxiedCommandSender) sender).getCallee();
				if (callee instanceof Entity) {
					targetName = ((Entity) callee).getUniqueId().toString();
				}
			}
		}

		return targetName;
	}
}
