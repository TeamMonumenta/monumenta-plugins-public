package com.playmonumenta.plugins.rawcommands;

import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;

import java.util.Collection;
import java.util.LinkedHashMap;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GenericPlayerCommand extends GenericCommand {
	@FunctionalInterface
	protected interface PlayerCommandExecutor {
		/**
		 * Called on each player targeted by the command
		 *
		 * @param sender Sender of the command
		 * @param player Target of the command
		 */
		void run(CommandSender sender, Player player);
	}

	@SuppressWarnings("unchecked")
	protected static void registerPlayerCommand(String command, String permission, PlayerCommandExecutor exec) {
		CommandPermission perms = new CommandPermission(permission);

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("players", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS));
		CommandAPI.getInstance().register(command,
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
		                                      for (Player player : (Collection<Player>)args[0]) {
												  exec.run(sender, player);
		                                      }
		                                  }
		);
	}
}
