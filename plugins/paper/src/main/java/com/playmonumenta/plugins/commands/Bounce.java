package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.FastUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class Bounce {
	private static final String COMMAND = "bounce";
	private static final String ALIAS_ONE = "knockback";
	private static final String ALIAS_TWO = "testkb";
	private static final String PERMISSION = "monumenta.commands.bounce";
	private static final EntitySelectorArgument.OnePlayer PLAYER_ARGUMENT = new EntitySelectorArgument.OnePlayer("player");

	public static void register() {
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withAliases(ALIAS_ONE, ALIAS_TWO)
			.withArguments(PLAYER_ARGUMENT)
			.executes((sender, args) -> {
				Player player = args.getByArgument(PLAYER_ARGUMENT);
				Vector vector = player.getEyeLocation().getDirection();

				vector.setX(FastUtils.randomDoubleInRange(-0.11, 0.11));
				vector.setY(FastUtils.randomIntInRange(0, 100) > 80
					? FastUtils.randomDoubleInRange(0.11, 0.22) // 20% chance for more noticeable Y velocity
					: FastUtils.randomDoubleInRange(0, 0.11));
				vector.setZ(FastUtils.randomDoubleInRange(-0.11, 0.11));

				player.setVelocity(vector);
			}).register();
	}
}
