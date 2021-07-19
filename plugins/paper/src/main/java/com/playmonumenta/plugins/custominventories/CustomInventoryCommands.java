package com.playmonumenta.plugins.custominventories;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;

public class CustomInventoryCommands {
	public static void register(Plugin plugin) {
		new CommandAPICommand("openteleportergui")
			.withPermission("monumenta.command.openteleportergui")
			.withArguments(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
			.executes((sender, args) -> {
				Player player = (Player)args[0];
				new OrinCustomInventory(player).openInventory(player, plugin);
			})
			.register();
		new CommandAPICommand("openPEB")
			.withPermission("monumenta.command.openpeb")
			.withArguments(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
			.executes((sender, args) -> {
				Player player = (Player)args[0];
				new PEBCustomInventory(player).openInventory(player, plugin);
			})
			.register();

		new CommandAPICommand("openinfusiongui")
			.withPermission("monumenta.command.openinfusiongui")
			.withArguments(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
			.executes((sender, args) -> {
				Player player = (Player)args[0];
				new InfusionCustomInventory(player).openInventory(player, plugin);
			})
			.register();

		new CommandAPICommand("opendelveinfusiongui")
			.withPermission("monumenta.command.opendelveinfusiongui")
			.withArguments(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
			.executes((sender, args) -> {
				Player player = (Player)args[0];
				new DelveInfusionCustomInventory(player).openInventory(player, plugin);
			})
			.register();

		new CommandAPICommand("openparrotgui")
			.withPermission("monumenta.command.openparrotgui")
			.withArguments(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
			.executes((sender, args) -> {
				Player player = (Player)args[0];
				new ParrotCustomInventory(player).openInventory(player, plugin);
			})
			.register();
	}
}
