package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class CoreProtectLogCommand {

	public static void register() {
		new CommandAPICommand("coreprotectlog")
			.withPermission("monumenta.command.coreprotectlog")
			.withSubcommand(
				new CommandAPICommand("placement")
					.withArguments(
						new EntitySelectorArgument.OnePlayer("player"),
						new LocationArgument("block", LocationType.BLOCK_POSITION)
					)
					.executes((sender, args) -> {
						CoreProtectIntegration.logPlacement((Player) args[0], ((Location) args[1]).getBlock());
					})
			)
			.withSubcommand(
				new CommandAPICommand("removal")
					.withArguments(
						new EntitySelectorArgument.OnePlayer("player"),
						new LocationArgument("block", LocationType.BLOCK_POSITION)
					)
					.executes((sender, args) -> {
						CoreProtectIntegration.logRemoval((Player) args[0], ((Location) args[1]).getBlock());
					})
			)
			.withSubcommand(
				new CommandAPICommand("transaction")
					.withArguments(
						new EntitySelectorArgument.OnePlayer("player"),
						new LocationArgument("block", LocationType.BLOCK_POSITION)
					)
					.executes((sender, args) -> {
						CoreProtectIntegration.logContainerTransaction((Player) args[0], (Location) args[1]);
					})
			)
			.register();
	}

}
