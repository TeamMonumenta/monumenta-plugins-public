package com.playmonumenta.plugins.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class Launch extends GenericCommand {
	public static void register() {

		CommandPermission perms = CommandPermission.fromString("monumenta.command.launch");

		List<Argument> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));
		arguments.add(new DoubleArgument("horizontal"));
		arguments.add(new DoubleArgument("vertical"));

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
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));
		arguments.add(new DoubleArgument("x"));
		arguments.add(new DoubleArgument("y"));
		arguments.add(new DoubleArgument("z"));

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
