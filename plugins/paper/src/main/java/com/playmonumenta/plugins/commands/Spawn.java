package com.playmonumenta.plugins.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerTeleportEvent;

public class Spawn {
	public static String COMMAND = "spawn";

	@SuppressWarnings("unchecked")
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.spawn");

		List<Argument> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument("Targets", EntitySelectorArgument.EntitySelector.MANY_ENTITIES));

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Collection<Entity> targets = (Collection<Entity>)args[0];
				run(targets);
			})
			.register();
	}

	public static void run(Collection<Entity> targets) {
		for (Entity target : targets) {
			target.teleport(target.getWorld().getSpawnLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
		}
	}
}
