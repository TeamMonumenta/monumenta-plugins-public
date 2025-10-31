package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import org.bukkit.Location;

public class BlockDisplayCommand {
	public static void register() {
		new CommandAPICommand("blockdisplay")
			.withPermission("monumenta.command.blockdisplay")
			.withSubcommands(
				new CommandAPICommand("blocksToDisplays")
					.withArguments(
						new LocationArgument("corner1", LocationType.BLOCK_POSITION),
						new LocationArgument("corner2", LocationType.BLOCK_POSITION)
					)
					.executes((sender, args) -> {
						Location corner1 = args.getUnchecked("corner1");
						Location corner2 = args.getUnchecked("corner2");
						DisplayEntityUtils.turnBlockCuboidIntoBlockDisplays(corner1, corner2);
					}),
				new CommandAPICommand("displaysToBlocks")
					.withArguments(
						new LocationArgument("corner1", LocationType.BLOCK_POSITION),
						new LocationArgument("corner2", LocationType.BLOCK_POSITION)
					)
					.executes((sender, args) -> {
						Location corner1 = args.getUnchecked("corner1");
						Location corner2 = args.getUnchecked("corner2");
						DisplayEntityUtils.turnBlockDisplayCuboidIntoBlocks(corner1, corner2);
					})
			).register();
	}
}
