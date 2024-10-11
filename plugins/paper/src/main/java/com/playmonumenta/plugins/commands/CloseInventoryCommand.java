package com.playmonumenta.plugins.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import java.util.Objects;
import org.bukkit.entity.Player;

// Impossible to do with vanilla commands.
public class CloseInventoryCommand {

	private static final String COMMAND = "closeinventory";
	private static final String PERMISSION = "monumenta.command.closeinventory";

	public static void register() {
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(new PlayerArgument("who"))
			.executes((commandSender, commandArguments) -> {
				((Player) Objects.requireNonNull(commandArguments.get(0))).closeInventory();
			})
			.register();
	}
}
