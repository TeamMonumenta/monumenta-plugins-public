package com.playmonumenta.plugins.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;

public class Spawn {
	public static String COMMAND = "spawn";

	@SuppressWarnings("unchecked")
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.spawn");

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument.ManyEntities("Targets"));

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executesNative((sender, args) -> {
				Collection<Entity> targets = (Collection<Entity>) args.get("Targets");
				run(sender.getWorld(), targets);
			})
			.register();
	}

	public static void run(World world, Collection<Entity> targets) {
		Location spawnLocation = world.getSpawnLocation();
		for (Entity target : targets) {
			target.teleport(spawnLocation, PlayerTeleportEvent.TeleportCause.COMMAND);
		}
	}
}
