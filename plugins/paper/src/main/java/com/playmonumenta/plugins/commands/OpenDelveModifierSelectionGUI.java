package com.playmonumenta.plugins.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.listeners.DelvesListener;
import com.playmonumenta.plugins.utils.DelvesUtils.DelveModifierSelectionGUI;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.StringArgument;

public class OpenDelveModifierSelectionGUI extends GenericCommand {

	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.opendmsgui");

		List<Argument> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_ENTITY));
		arguments.add(new StringArgument("dungeon"));

		new CommandAPICommand("opendmsgui")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				run((Entity) args[0], (String) args[1]);
			})
			.register();
	}

	private static void run(Entity entity, String dungeon) {
		if (entity instanceof Player) {
			DelveModifierSelectionGUI gui = new DelveModifierSelectionGUI((Player) entity, dungeon);
			DelvesListener.openGUI((Player) entity, gui);
		}
	}

}
