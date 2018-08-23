package com.playmonumenta.plugins.command.commands;

import java.util.ArrayList;
import java.util.List;

import com.playmonumenta.plugins.command.AbstractPlayerCommand;
import com.playmonumenta.plugins.command.CommandContext;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class HopeifyHeldItem extends AbstractPlayerCommand {

	public HopeifyHeldItem(Plugin plugin) {
		super(
		    "hopeifyHeldItem",
		    "Adds the hope enchant and the player's name to their held item",
		    plugin
		);
	}

	@Override
	protected void configure(ArgumentParser parser) {
	}

	@Override
	protected boolean run(CommandContext context) {
		//noinspection OptionalGetWithoutIsPresent - checked before being called
		final Player player = context.getPlayer().get();

		// TODO consider refactoring

		ItemStack item = player.getEquipment().getItemInMainHand();
		if (item == null) {
			sendErrorMessage(context, "Player must have a King's Valley item in their main hand!");
			return false;
		}

		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			sendErrorMessage(context, "Player must have a King's Valley item in their main hand!");
			return false;
		}

		List<String> lore = meta.getLore();
		if (lore == null || lore.isEmpty()) {
			sendErrorMessage(context, "Player must have a King's Valley item in their main hand!");
			return false;
		}

		List<String> newLore = new ArrayList<>();
		boolean hopeAdded = false;
		boolean nameAdded = false;
		boolean kingsValleyFound = false;
		for (String loreEntry : lore) {
			if (loreEntry.contains(ChatColor.GRAY + "Hope")) {
				sendErrorMessage(context, "Player's item already has hope");
				return false;
			}

			if (loreEntry.contains("King's Valley")) {
				kingsValleyFound = true;
			}

			if (!hopeAdded && (loreEntry.contains("King's Valley") || ChatColor.stripColor(loreEntry).trim().isEmpty())) {
				newLore.add(ChatColor.GRAY + "Hope");
				hopeAdded = true;
			}

			if (!nameAdded && ChatColor.stripColor(loreEntry).trim().isEmpty()) {
				newLore.add("Infused by " + player.getName());
				nameAdded = true;
			}

			newLore.add(loreEntry);
		}

		if (!nameAdded) {
			newLore.add("Infused by " + player.getName());
		}

		if (!kingsValleyFound) {
			sendErrorMessage(context, "Player must have a King's Valley item in their main hand!");
			return false;
		}

		meta.setLore(newLore);
		item.setItemMeta(meta);
		sendMessage(context, "Succesfully added hope to player's held item");

		return true;
	}
}
