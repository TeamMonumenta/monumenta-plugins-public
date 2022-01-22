package com.playmonumenta.plugins.custominventories;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class CustomInventoryCommands {
	public static void register(Plugin plugin) {
		//Avoid unused arguments, make sure you have a permission tied to the GUI command,
		//and perform any checks that should reject the player from opening the GUI here.
		//Once in the constructor for the GUI, it's much more difficult to properly
		//reject the player.
		new CommandAPICommand("openexamplecustominvgui")
			.withPermission("monumenta.command.openexamplecustominvgui")
			.withArguments(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
			.executes((sender, args) -> {
				Player player = (Player)args[0];
				new ExampleCustomInventory(player).openInventory(player, plugin);
			})
			.register();

		new CommandAPICommand("openteleportergui")
			.withPermission("monumenta.command.openteleportergui")
			.withArguments(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
			.executes((sender, args) -> {
				Player player = (Player)args[0];
				new OrinCustomInventory(player, -1).openInventory(player, plugin);
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
				try {
					new ParrotCustomInventory(player).openInventory(player, plugin);
				} catch (Exception ex) {
					String msg = "Failed to open Parrot GUI: " + ex.getMessage();
					sender.sendMessage(msg);
					player.sendMessage(msg);
					ex.printStackTrace();
				}
			})
			.register();

		new CommandAPICommand("openclassgui")
			.withPermission("monumenta.command.openclassgui")
			.withArguments(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
			.executes((sender, args) -> {
				Player player = (Player)args[0];
				new ClassSelectionCustomInventory(player).openInventory(player, plugin);
			})
			.register();
	}
}
