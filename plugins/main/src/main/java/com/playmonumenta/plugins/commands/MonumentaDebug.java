package com.playmonumenta.plugins.commands;

import java.util.LinkedHashMap;
import java.util.logging.Level;

import com.playmonumenta.plugins.Plugin;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.LiteralArgument;

public class MonumentaDebug {
	public static void register(Plugin plugin) {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("level", new LiteralArgument("INFO"));
		CommandAPI.getInstance().register("monumentadebug",
		                                  CommandPermission.fromString("monumenta.command.monumentadebug"),
		                                  arguments,
		                                  (sender, args) -> {
											  plugin.getLogger().setLevel(Level.INFO);
		                                  }
		);

		arguments.clear();
		arguments.put("level", new LiteralArgument("FINE"));
		CommandAPI.getInstance().register("monumentadebug",
		                                  CommandPermission.fromString("monumenta.command.monumentadebug"),
		                                  arguments,
		                                  (sender, args) -> {
											  plugin.getLogger().setLevel(Level.FINE);
		                                  }
		);

		arguments.clear();
		arguments.put("level", new LiteralArgument("FINER"));
		CommandAPI.getInstance().register("monumentadebug",
		                                  CommandPermission.fromString("monumenta.command.monumentadebug"),
		                                  arguments,
		                                  (sender, args) -> {
											  plugin.getLogger().setLevel(Level.FINER);
		                                  }
		);

		arguments.clear();
		arguments.put("level", new LiteralArgument("FINEST"));
		CommandAPI.getInstance().register("monumentadebug",
		                                  CommandPermission.fromString("monumenta.command.monumentadebug"),
		                                  arguments,
		                                  (sender, args) -> {
											  plugin.getLogger().setLevel(Level.FINEST);
		                                  }
		);
	}
}


