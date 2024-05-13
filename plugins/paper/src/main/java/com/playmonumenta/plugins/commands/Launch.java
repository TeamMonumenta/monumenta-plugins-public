package com.playmonumenta.plugins.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class Launch extends GenericCommand {
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.launch");

		EntitySelectorArgument.OnePlayer playerArg = new EntitySelectorArgument.OnePlayer("player");
		DoubleArgument horizontalArg = new DoubleArgument("horizontal");
		DoubleArgument verticalArg = new DoubleArgument("vertical");
		DoubleArgument xArg = new DoubleArgument("x");
		DoubleArgument yArg = new DoubleArgument("y");
		DoubleArgument zArg = new DoubleArgument("z");

		new CommandAPICommand("launch")
			.withPermission(perms)
			.withArguments(playerArg)
			.withArguments(horizontalArg)
			.withArguments(verticalArg)
			.executes((sender, args) -> {
				Player player = args.getByArgument(playerArg);
				Vector v = player.getEyeLocation().getDirection();
				v.multiply(args.getByArgument(horizontalArg));
				v.setY(args.getByArgument(verticalArg));
				player.setVelocity(v);
			})
			.register();

		new CommandAPICommand("launch")
			.withPermission(perms)
			.withArguments(playerArg)
			.withArguments(xArg)
			.withArguments(yArg)
			.withArguments(zArg)
			.executes((sender, args) -> {
				Player player = args.getByArgument(playerArg);
				Vector v = player.getEyeLocation().getDirection();
				v.setX(args.getByArgument(xArg));
				v.setY(args.getByArgument(yArg));
				v.setZ(args.getByArgument(zArg));
				player.setVelocity(v);
			})
			.register();
	}
}
