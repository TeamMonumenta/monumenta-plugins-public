package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.portals.PortalManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bukkit.entity.Player;

public class Portal2 extends GenericCommand {
	@SuppressWarnings("unchecked")
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.portal2");
		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument.ManyPlayers("players"));
		new CommandAPICommand("portal2")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				for (Player player : (Collection<Player>)args[0]) {
					run(player, 0);
				}
			})
			.register();
		arguments.add(new IntegerArgument("gun id", 0, 3));
		new CommandAPICommand("portal2")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				for (Player player : (Collection<Player>)args[0]) {
					run(player, (Integer) args[1]);
				}
			})
			.register();
	}

	private static void run(Player player, int gunId) {
		PortalManager.spawnPortal(player, 2, gunId);
	}
}
