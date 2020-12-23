package com.playmonumenta.plugins.commands;

import java.util.LinkedHashMap;

import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;

public class Launch extends GenericCommand {
	public static void register() {

		CommandPermission perms = CommandPermission.fromString("monumenta.command.launch");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("player", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));
		arguments.put("horizontal", new DoubleArgument());
		arguments.put("vertical", new DoubleArgument());

		new CommandAPICommand("launch")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				Vector v = player.getEyeLocation().getDirection();
				v.multiply((double) args[1]);
				v.setY((double) args[2]);
				player.setVelocity(v);
			})
			.register();

		arguments.clear();
		arguments.put("player", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));
		arguments.put("x", new DoubleArgument());
		arguments.put("y", new DoubleArgument());
		arguments.put("z", new DoubleArgument());

		new CommandAPICommand("launch")
		.withPermission(perms)
		.withArguments(arguments)
		.executes((sender, args) -> {
			Player player = (Player) args[0];
			Vector v = player.getEyeLocation().getDirection();
			v.setX((double) args[1]);
			v.setY((double) args[2]);
			v.setZ((double) args[3]);
			player.setVelocity(v);
		})
		.register();
	}
}
