package com.playmonumenta.plugins.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.FunctionArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

public class RunRegion extends GenericCommand {
	private static final int REGION_POS_MASK = 0xfffffe00;
	private static final int REGION_BLOCKS = 512;
	private static final int CHUNK_BLOCKS = 16;

	@SuppressWarnings("unchecked")
	public static void register() {
		new CommandAPICommand("runregion")
			.withPermission(CommandPermission.fromString("monumenta.command.runregion"))
			.withArguments(new LocationArgument("location", LocationType.BLOCK_POSITION))
			.withArguments(new MultiLiteralArgument("function"))
			.withArguments(new FunctionArgument("name"))
			.executes((sender, args) -> {
				Location loc = (Location) args[0];
				FunctionWrapper[] functions = (FunctionWrapper[]) args[2];

				int minX = ((int) loc.getX()) & REGION_POS_MASK;
				int minZ = ((int) loc.getZ()) & REGION_POS_MASK;
				int maxX = minX + REGION_BLOCKS;
				int maxZ = minZ + REGION_BLOCKS;
				int result = 0;
				for (int z = minZ; z < maxZ; z += CHUNK_BLOCKS) {
					loc.setZ((double) z);
					for (int x = minX; x < maxX; x += CHUNK_BLOCKS) {
						loc.setX((double) x);
						if (loc.isChunkLoaded()) {
							for (Entity entity : loc.getChunk().getEntities()) {
								if (!entity.isValid()) {
									// entity is dead
									continue;
								}
								for (FunctionWrapper function : functions) {
									result += function.runAs(entity);
								}
							}
						}
					}
				}
				return result;
			})
			.register();
	}
}
