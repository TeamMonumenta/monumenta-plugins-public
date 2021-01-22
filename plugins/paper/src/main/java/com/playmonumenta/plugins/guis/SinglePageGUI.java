package com.playmonumenta.plugins.guis;

import java.util.LinkedHashMap;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.StringArgument;

public abstract class SinglePageGUI {

	/*
	 * Only abstract methods should be overridden in child classes.
	 *
	 * Refer to ExampleSinglePageGUI for an example.
	 *
	 * DO NOT OVERRIDE ANY OTHER METHODS!
	 */

	protected final Player mPlayer;
	protected final Inventory mInventory;

	// The child constructor should just call the parent constructor
	public SinglePageGUI(Player player, String[] args) {
		mPlayer = player;

		// If the player is null, this was called from the manager to register a command.
		if (player == null) {
			mInventory = null;
		} else {
			mInventory = getInventory(player, args);
		}
	}

	// Override this with a call to registerCommand(command, ...)
	public abstract void registerCommand();

	public void registerCommand(String command, String... potentialStringArgs) {
		CommandPermission perms = CommandPermission.fromString("monumenta.command." + command);

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("player", new EntitySelectorArgument(EntitySelector.ONE_ENTITY));
		for (String stringArg : potentialStringArgs) {
			arguments.put(stringArg, new StringArgument());
		}

		new CommandAPICommand(command).withPermission(perms).withArguments(arguments).executes(
				(sender, args) -> {
					String[] stringArgs = new String[args.length - 1];
					for (int i = 0; i < stringArgs.length; i++) {
						stringArgs[i] = (String) args[i + 1];
					}

					run((Entity) args[0], stringArgs);
				}).register();
	}

	private void run(Entity entity, String[] args) {
		if (entity instanceof Player) {
			Player player = (Player) entity;
			SinglePageGUIManager.openGUI(player, constructGUI(player, args));
		}
	}

	// Override this by simply returning the constructor
	public abstract SinglePageGUI constructGUI(Player player, String[] args);

	// Return the inventory that will be used for the GUI
	public abstract Inventory getInventory(Player player, String[] args);

	public void openGUI() {
		mPlayer.openInventory(mInventory);
	}

	public boolean contains(Inventory inventory) {
		return (mInventory.equals(inventory));
	}

	public void registerClick(InventoryClickEvent event) {
		if (mInventory.equals(event.getInventory())) {
			processClick(event);
		}
	}

	// This should handle any interaction with the GUI
	public abstract void processClick(InventoryClickEvent event);

}
