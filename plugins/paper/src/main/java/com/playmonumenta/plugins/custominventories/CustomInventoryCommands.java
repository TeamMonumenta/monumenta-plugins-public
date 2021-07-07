package com.playmonumenta.plugins.custominventories;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;

public class CustomInventoryCommands {
	public static void register(Plugin plugin) {
		new CommandAPICommand("openteleportergui")
			.withArguments(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
			.executes((sender, args) -> {
				Player player = (Player)args[0];
				new OrinCustomInventory(player).openInventory(player, plugin);
			})
			.register();
		new CommandAPICommand("openPEB")
			.withArguments(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
			.executes((sender, args) -> {
				Player player = (Player)args[0];
				new PEBCustomInventory(player).openInventory(player, plugin);
			})
			.register();
	}
}
