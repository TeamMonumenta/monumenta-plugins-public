package com.playmonumenta.plugins.command.commands;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.command.AbstractPlayerCommand;
import com.playmonumenta.plugins.command.CommandContext;
import net.sourceforge.argparse4j.inf.*;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.plugin.Plugin;

public class DeathMsg extends AbstractPlayerCommand {

	public DeathMsg(Plugin plugin) {
		super(
		    "deathMsg",
		    "Displays or toggles death messages on and off",
		    plugin
		);
	}

	@Override
	protected void configure(final ArgumentParser parser) {
		parser.addArgument("state")
		.help("if true, all player will see")
		.nargs("?")
		.choices("on", "off", "true", "false")
		.type((argParser, arg, value) -> {
			if (value.equals("on") || value.equals("true")) {
				return 0;
			} else {
				return 1;
			}
		})
		.setDefault(0);
	}

	@Override
	protected boolean run(final CommandContext context) {
		//noinspection OptionalGetWithoutIsPresent - checked before being called
		final Player player = context.getPlayer().get();
		Integer newState = context.getNamespace().get("state");

		if (newState != null) {
			// If a value was given, then update
			ScoreboardUtils.setScoreboardValue(player, Constants.SCOREBOARD_DEATH_MESSAGE, newState);
		} else {
			// Otherwise, get the existing value
			newState = ScoreboardUtils.getScoreboardValue(player, Constants.SCOREBOARD_DEATH_MESSAGE);
		}

		sendMessage(context, ChatColor.GOLD + "" + ChatColor.BOLD + "Death Message Settings");
		sendMessage(context, ChatColor.AQUA + "When you die, your death message will be shown to:");

		if (newState == 0) {
			sendMessage(context, ChatColor.GREEN + "  All players on the current shard");
		} else {
			sendMessage(context, ChatColor.GREEN + "  Only you");
		}

		sendMessage(context, ChatColor.AQUA + "Change this with " + ChatColor.GOLD + "/deathmsg on" +
		            ChatColor.AQUA + " or " + ChatColor.GOLD + "/deathmsg off");

		return true;
	}
}
