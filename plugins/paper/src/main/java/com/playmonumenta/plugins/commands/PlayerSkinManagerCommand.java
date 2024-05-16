package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.managers.PlayerSkinManager;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class PlayerSkinManagerCommand {
	public static void register() {
		new CommandAPICommand("playerskinmanager")
			.withPermission("monumenta.command.playerskinmanager")
			.withSubcommand(new CommandAPICommand("reload").executes((sender, args) -> {
				boolean success = PlayerSkinManager.readFile();
				if (success) {
					sender.sendMessage(Component.text("PlayerSkinManager config reload was sucessful!", NamedTextColor.GREEN));
				} else {
					sender.sendMessage(Component.text("PlayerSkinManager config reload failed!", NamedTextColor.RED));
				}
			})).register();
	}
}
