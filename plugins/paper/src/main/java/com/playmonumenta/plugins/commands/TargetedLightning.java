package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;

public class TargetedLightning {
	public static void register() {
		new CommandAPICommand("targetedlightning")
			.withPermission(CommandPermission.fromString("monumenta.command.targetedlightning"))
			.withArguments(
				new LocationArgument("location", LocationType.PRECISE_POSITION),
				new EntitySelectorArgument.ManyPlayers("players")
			)
			.executes((sender, args) -> {
				Location loc = args.getUnchecked("location");
				Collection<Player> players = args.getUnchecked("players");
				if (loc != null && players != null) {
					LightningStrike lightning = loc.getWorld().strikeLightning(loc);
					for (Player player : loc.getWorld().getPlayers()) {
						if (!players.contains(player)) {
							player.hideEntity(Plugin.getInstance(), lightning);
						}
					}
				}
			})
			.register();
	}
}
