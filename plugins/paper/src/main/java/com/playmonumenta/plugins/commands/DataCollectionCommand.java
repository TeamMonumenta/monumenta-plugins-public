package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class DataCollectionCommand {
	public static void register() {
		new CommandAPICommand("datacollection")
			.withPermission("monumenta.command.datacollection")
			.withSubcommands(
				new CommandAPICommand("pause")
					.executesPlayer((player, args) -> {
						boolean result = Plugin.getInstance().mDataCollectionManager.pauseDataCollection();
						if (result) {
							player.sendMessage(Component.text("Data collection paused!", NamedTextColor.GREEN));
						} else {
							player.sendMessage(Component.text("Data collection is not enabled on this shard.", NamedTextColor.RED));
						}
					}),
				new CommandAPICommand("resume")
					.executesPlayer((player, args) -> {
						boolean result = Plugin.getInstance().mDataCollectionManager.resumeDataCollection();
						if (result) {
							player.sendMessage(Component.text("Data collection resumed!", NamedTextColor.GREEN));
						} else {
							player.sendMessage(Component.text("Data collection is not enabled on this shard.", NamedTextColor.RED));
						}
					})
			)
			.register();
	}
}
