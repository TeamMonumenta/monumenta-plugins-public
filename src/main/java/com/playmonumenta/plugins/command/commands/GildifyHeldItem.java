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

public class GildifyHeldItem extends AbstractPlayerCommand {

	public GildifyHeldItem(Plugin plugin) {
		super(
		    "gildifyHeldItem",
		    "Adds the Gilded enchant and the player's name to their held item",
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
		boolean gildedAdded = false;
		boolean nameAdded = false;
		boolean kingsValleyFound = false;
		boolean duplicateItem = true;
		for (String loreEntry : lore) {
			if (loreEntry.contains(ChatColor.GRAY + "Gilded")) {
				sendErrorMessage(context, "Player's item already gilded");
				return false;
			}

			if (loreEntry.contains("King's Valley")) {
				kingsValleyFound = true;
			}

			String loreStripped = ChatColor.stripColor(loreEntry).trim();
			if (loreStripped.contains("Ephemeral Corridors") ||
				loreStripped.contains("King's Valley : Epic") ||
				loreStripped.contains("King's Valley : Artifact") ||
				loreStripped.contains("King's Valley : Enhanced Rare") ||
				loreStripped.contains("King's Valley : Enhanced Uncommon")) {
				duplicateItem = false;
			}

			if (!gildedAdded && (loreEntry.contains("King's Valley") || ChatColor.stripColor(loreEntry).trim().isEmpty())) {
				newLore.add(ChatColor.GRAY + "Gilded");
				gildedAdded = true;
			}

			if (!nameAdded && ChatColor.stripColor(loreEntry).trim().isEmpty()) {
				newLore.add("Gilded by " + player.getName());
				nameAdded = true;
			}

			newLore.add(loreEntry);
		}

		if (!nameAdded) {
			newLore.add("Gilded by " + player.getName());
		}

		if (!kingsValleyFound) {
			sendErrorMessage(context, "Player must have a King's Valley item in their main hand!");
			return false;
		}

		ItemStack dupe = null;
		if (duplicateItem) {
			// Give the player a copy of their un-modified item
			dupe = item.clone();
			dupe.setAmount(1);
		}

		meta.setLore(newLore);
		item.setItemMeta(meta);
		item.setAmount(1);

		if (duplicateItem) {
			player.getInventory().addItem(dupe);
		}

		sendMessage(context, "Succesfully added Gilded to player's held item");

		return true;
	}
}
