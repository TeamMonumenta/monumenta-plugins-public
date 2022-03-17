package com.playmonumenta.plugins.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class RunWithPlaceholdersCommand {

	public static void register() {
		new CommandAPICommand("papiexecute")
			.withPermission("monumenta.command.papiexecute")
			.withArguments(
				new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER),
				new GreedyStringArgument("command"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				String command = (String) args[1];

				command = PlaceholderAPI.setPlaceholders(player, command);
				Bukkit.dispatchCommand(sender, command);
			})
			.register();
	}

}
