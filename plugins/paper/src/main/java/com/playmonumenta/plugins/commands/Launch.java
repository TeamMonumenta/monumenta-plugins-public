package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.FastUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.BooleanArgument;
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
		DoubleArgument yawArg = new DoubleArgument("yaw");
		DoubleArgument pitchArg = new DoubleArgument("pitch");
		DoubleArgument magnitudeArg = new DoubleArgument("magnitude");
		BooleanArgument relativeArg = new BooleanArgument("relative yaw");


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

		new CommandAPICommand("launch")
			.withPermission(perms)
			.withArguments(playerArg)
			.withArguments(yawArg)
			.withArguments(pitchArg)
			.withArguments(magnitudeArg)
			.withArguments(relativeArg)
			.executes((sender, args) -> {
				Player player = args.getByArgument(playerArg);
				double yaw = Math.toRadians(args.getByArgument(yawArg));
				double pitch = Math.toRadians(args.getByArgument(pitchArg));

				if (args.getByArgument(relativeArg)) {
					yaw += Math.toRadians(player.getYaw());
				}

				double m = args.getByArgument(magnitudeArg);
				Vector v = new Vector(-FastUtils.sin(yaw) * FastUtils.cos(pitch), -FastUtils.sin(pitch), FastUtils.cos(yaw) * FastUtils.cos(pitch));
				v.normalize().multiply(m);

				player.setVelocity(v);
			})
			.register();
	}
}
