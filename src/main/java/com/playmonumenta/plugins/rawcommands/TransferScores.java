package com.playmonumenta.plugins.rawcommands;

import java.util.LinkedHashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.StringArgument;

public class TransferScores {
	public static void register(Plugin plugin) {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("from", new StringArgument());
		arguments.put("to", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));
		CommandAPI.getInstance().register("transferscores",
		                                  CommandPermission.fromString("monumenta.command.transferscores"),
		                                  arguments,
		                                  (sender, args) -> {
											  run(plugin, sender, (String)args[0], (Player)args[1]);
		                                  }
		);
	}

	private static void run(Plugin plugin, CommandSender sender, String from, Player to) throws CommandSyntaxException {
		if (!(sender instanceof Player || sender instanceof ConsoleCommandSender)) {
			CommandAPI.fail("This command can only be run by a player or the console");
		}

		try {
			ScoreboardUtils.transferPlayerScores(plugin, from, to);
		} catch (Exception e) {
			CommandAPI.fail(e.getMessage());
		}

		sender.sendMessage(ChatColor.GREEN + "Successfully transfered scoreboard values from '" + from + "' to '" + to + "'");
	}
}

