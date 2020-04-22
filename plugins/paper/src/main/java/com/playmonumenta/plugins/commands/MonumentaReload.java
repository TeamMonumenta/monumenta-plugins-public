package com.playmonumenta.plugins.commands;

import java.util.LinkedHashMap;

import org.bukkit.command.CommandSender;

import com.playmonumenta.plugins.Plugin;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.exceptions.WrapperCommandSyntaxException;

public class MonumentaReload {
	public static void register(Plugin plugin) {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		CommandAPI.getInstance().register("monumentareload",
		                                  CommandPermission.fromString("monumenta.command.monumentareload"),
		                                  arguments,
		                                  (sender, args) -> {
											  run(plugin, sender);
		                                  }
		);
	}

	private static void run(Plugin plugin, CommandSender sender) throws WrapperCommandSyntaxException {
		plugin.reloadMonumentaConfig(sender);
	}
}

