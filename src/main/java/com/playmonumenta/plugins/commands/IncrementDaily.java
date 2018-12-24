package com.playmonumenta.plugins.commands;

import java.util.LinkedHashMap;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.reset.DailyReset;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;

public class IncrementDaily {
	public static void register(Plugin plugin) {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		CommandAPI.getInstance().register("incrementdaily",
		                                  CommandPermission.fromString("monumenta.command.incrementdaily"),
		                                  arguments,
		                                  (sender, args) -> {
											  run(plugin, sender);
		                                  }
		);
	}

	private static void run(Plugin plugin, CommandSender sender) throws CommandSyntaxException {
		//  Increment the servers Daily version.
		plugin.incrementDailyVersion();

		//  Loop through all online players, reset their scoreboards and message them about the Daily reset.
		for (Player player : plugin.getServer().getOnlinePlayers()) {
			DailyReset.handle(plugin, player);
		}
	}
}

