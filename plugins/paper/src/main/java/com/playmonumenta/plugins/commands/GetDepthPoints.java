package com.playmonumenta.plugins.commands;

import java.util.LinkedHashMap;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.DelveInfo;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.StringArgument;

public class GetDepthPoints extends GenericCommand {

	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.getdepthpoints");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("player", new EntitySelectorArgument(EntitySelector.ONE_ENTITY));
		arguments.put("dungeon", new StringArgument());

		new CommandAPICommand("getdepthpoints")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				return run((Entity) args[0], (String) args[1]);
			})
			.register();
	}

	private static int run(Entity entity, String dungeon) {
		if (entity instanceof Player) {
			DelveInfo delveInfo = DelvesUtils.getDelveInfo((Player) entity, dungeon);
			if (delveInfo != null) {
				return delveInfo.getDepthPoints();
			}
		}

		return 0;
	}

}
