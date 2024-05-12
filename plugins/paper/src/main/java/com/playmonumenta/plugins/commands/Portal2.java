package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.portals.PortalManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import java.util.Collection;
import org.bukkit.entity.Player;

public class Portal2 extends GenericCommand {
	@SuppressWarnings("unchecked")
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.portal2");
		new CommandAPICommand("portal2")
			.withPermission(perms)
			.withArguments(new EntitySelectorArgument.ManyPlayers("players"))
			.withOptionalArguments(new IntegerArgument("gun id", 0, 3))
			.executes((sender, args) -> {
				for (Player player : (Collection<Player>) args.get("players")) {
					run(player, args.getOrDefaultUnchecked("gun id", 0));
				}
			})
			.register();
	}

	private static void run(Player player, int gunId) {
		PortalManager.spawnPortal(player, 2, gunId);
	}
}
