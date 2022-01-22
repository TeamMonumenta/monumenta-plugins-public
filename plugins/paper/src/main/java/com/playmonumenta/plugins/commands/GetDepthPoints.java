package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.DelveInfo;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class GetDepthPoints extends GenericCommand {

	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.getdepthpoints");

		List<Argument> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));
		arguments.add(new StringArgument("dungeon"));

		new CommandAPICommand("getdepthpoints")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				return run((Player) args[0], (String) args[1]);
			})
			.register();
	}

	private static int run(Player entity, String dungeon) {
		DelveInfo delveInfo = DelvesUtils.getDelveInfo((Player) entity, dungeon);
		if (delveInfo != null) {
			return delveInfo.getDepthPoints();
		}

		return 0;
	}

}
