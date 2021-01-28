package com.playmonumenta.plugins.commands;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;

public class SetViewDistance {
	private static final Map<UUID, Integer> INITIAL_VIEW_DISTANCES = new HashMap<>();

	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.setviewdistance");
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("location", new LocationArgument(LocationType.PRECISE_POSITION));
		arguments.put("value", new IntegerArgument(-1, 32));

		new CommandAPICommand("setviewdistance")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				World world = ((Location)args[0]).getWorld();
				int distance = (Integer)args[1];

				if (distance > 0) {
					if (!INITIAL_VIEW_DISTANCES.containsKey(world.getUID())) {
						INITIAL_VIEW_DISTANCES.put(world.getUID(), world.getViewDistance());
					}

					world.setViewDistance(distance);
					sender.sendMessage("View distance for world '" + world.getName() + "' set to " + Integer.toString(distance));
				} else {
					Integer initial = INITIAL_VIEW_DISTANCES.get(world.getViewDistance());
					if (initial == null) {
						sender.sendMessage("Original view distance for world '" + world.getName() + "' unchanged, currently " + Integer.toString(world.getViewDistance()));
					} else {
						world.setViewDistance(initial);
						sender.sendMessage("View distance for world '" + world.getName() + "' reset to " + Integer.toString(initial));
					}
				}
			})
			.register();
	}
}
