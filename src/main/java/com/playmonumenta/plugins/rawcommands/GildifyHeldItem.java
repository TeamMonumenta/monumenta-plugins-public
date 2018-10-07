package com.playmonumenta.plugins.rawcommands;

import com.playmonumenta.plugins.Plugin;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GildifyHeldItem extends GenericPlayerCommand {
	public static void register(Plugin plugin) {
		registerPlayerCommand("gildifyhelditem", "monumenta.command.gildifyhelditem",
		                      (sender, player) -> {
		                          run(plugin, sender, player);
		                      });
	}

	private static void run(Plugin plugin, CommandSender sender, Player player) {
		ItemStack item = player.getEquipment().getItemInMainHand();
		if (item == null) {
			error(sender, "Player must have a King's Valley item in their main hand!");
			return;
		}

		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			error(sender, "Player must have a King's Valley item in their main hand!");
			return;
		}

		List<String> lore = meta.getLore();
		if (lore == null || lore.isEmpty()) {
			error(sender, "Player must have a King's Valley item in their main hand!");
			return;
		}

		List<String> newLore = new ArrayList<>();
		boolean gildedAdded = false;
		boolean nameAdded = false;
		boolean kingsValleyFound = false;
		boolean duplicateItem = true;
		for (String loreEntry : lore) {
			if (loreEntry.contains(ChatColor.GRAY + "Gilded")) {
				error(sender, "Player's item already gilded");
				return;
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
			error(sender, "Player must have a King's Valley item in their main hand!");
			return;
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

		sender.sendMessage("Succesfully added Gilded to player's held item");
	}
}
