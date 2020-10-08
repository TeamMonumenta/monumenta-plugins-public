package com.playmonumenta.plugins.commands;

import java.util.LinkedHashMap;

import com.playmonumenta.plugins.Plugin;

import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class MonumentaReload {
	public static void register(Plugin plugin) {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		new CommandAPICommand("monumentareload")
			.withPermission(CommandPermission.fromString("monumenta.command.monumentareload"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				run(plugin, sender);
			})
			.register();
	}

	private static void run(Plugin plugin, CommandSender sender) throws WrapperCommandSyntaxException {
		plugin.reloadMonumentaConfig(sender);
	}
}

