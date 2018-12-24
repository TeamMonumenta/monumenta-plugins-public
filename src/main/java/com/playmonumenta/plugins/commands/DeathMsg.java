package com.playmonumenta.plugins.commands;

import java.util.LinkedHashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.BooleanArgument;

public class DeathMsg {
	public static void register() {
		final String command = "deathmsg";
		final CommandPermission perms = CommandPermission.fromString("monumenta.command.deathmsg");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		CommandAPI.getInstance().register(command,
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
											  run(sender, null);
		                                  }
		);

		arguments.put("true/false", new BooleanArgument());
		CommandAPI.getInstance().register(command,
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
											  run(sender, (Boolean)args[0]);
		                                  }
		);
	}

	private static void run(CommandSender sender, Boolean newState) throws CommandSyntaxException {
		Player player = null;

		if (sender instanceof ProxiedCommandSender) {
			sender = ((ProxiedCommandSender)sender).getCallee();
		}

		if (sender instanceof Player) {
			player = (Player)sender;
		} else {
			CommandAPI.fail("This command must be run by/as a player!");
		}

		/* NOTE
		 *
		 * Scoreboard value is inverted (false = 1, true = 0)
		 * This is because the default (0) should be to display the death message
		 */
		if (newState != null) {
			// If a value was given, then update
			ScoreboardUtils.setScoreboardValue(player, Constants.SCOREBOARD_DEATH_MESSAGE, newState ? 0 : 1);
		} else {
			// Otherwise, get the existing value
			newState = ScoreboardUtils.getScoreboardValue(player, Constants.SCOREBOARD_DEATH_MESSAGE) == 0 ? true : false;
		}

		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Death Message Settings");
		player.sendMessage(ChatColor.AQUA + "When you die, your death message will be shown to:");

		if (newState) {
			player.sendMessage(ChatColor.GREEN + "  All players on the current shard");
		} else {
			player.sendMessage(ChatColor.GREEN + "  Only you");
		}

		player.sendMessage(ChatColor.AQUA + "Change this with " + ChatColor.GOLD + "/deathmsg true" +
		                   ChatColor.AQUA + " or " + ChatColor.GOLD + "/deathmsg false");
	}
}
