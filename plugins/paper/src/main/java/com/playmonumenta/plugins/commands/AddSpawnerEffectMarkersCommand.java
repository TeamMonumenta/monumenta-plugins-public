package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.SpawnerUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;

public class AddSpawnerEffectMarkersCommand {
	public static void register() {
		new CommandAPICommand("addspawnereffectmarkers")
			.withPermission("monumenta.command.addspawnereffectmarkers")
			.withArguments(
				new LocationArgument("corner1", LocationType.BLOCK_POSITION),
				new LocationArgument("corner2", LocationType.BLOCK_POSITION)
			)
			.executes((sender, args) -> {
					Location corner1 = args.getUnchecked("corner1");
					Location corner2 = args.getUnchecked("corner2");
					SpawnerUtils.addSpawnerEffectMarkers(corner1, corner2);
					sender.sendMessage(Component.text("Spawners in the specified area have been updated!"));
				}
			).register();
	}
}
