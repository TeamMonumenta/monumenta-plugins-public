package com.playmonumenta.plugins.commands;

import java.util.LinkedHashMap;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class DeathMsg {
	private static final String COMMAND = "deathmsg";

	public static void register() {
		final CommandPermission perms = CommandPermission.fromString("monumenta.command.deathmsg");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				run(sender, null);
			})
			.register();

		arguments.put("true/false", new BooleanArgument());
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				run(sender, (Boolean)args[0]);
			})
			.register();
	}

	private static void run(CommandSender sender, Boolean newState) throws WrapperCommandSyntaxException {
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
