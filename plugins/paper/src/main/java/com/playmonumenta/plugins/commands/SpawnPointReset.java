package com.playmonumenta.plugins.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import java.util.Collection;
import java.util.Objects;
import org.bukkit.entity.Player;

public class SpawnPointReset {

	@SuppressWarnings("unchecked")
	public static void register() {
		new CommandAPICommand("spawnpointreset")
				.withSubcommand(new CommandAPICommand("reset"))
				.withPermission(CommandPermission.fromString("monumenta.spawnpointreset"))
				.withArguments(new EntitySelectorArgument.ManyPlayers("players"))
				.executes((sender, args) -> {
					Collection<Player> targets = (Collection<Player>) Objects.requireNonNull(args.get("players"));
					for (Player target : targets) {
						target.setRespawnLocation(null, true);
					}
				})
				.register();
	}

}
