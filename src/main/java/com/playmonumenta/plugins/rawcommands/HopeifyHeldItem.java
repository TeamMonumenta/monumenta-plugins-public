package com.playmonumenta.plugins.rawcommands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class HopeifyHeldItem extends GenericCommand {
	public static void register() {
		registerPlayerCommand("hopeifyhelditem", "monumenta.command.hopeifyhelditem",
		                      (sender, player) -> {
		                          run(sender, player);
		                      });
	}

	private static void run(CommandSender sender, Player player) {
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
		boolean hopeAdded = false;
		boolean nameAdded = false;
		boolean kingsValleyFound = false;
		for (String loreEntry : lore) {
			if (loreEntry.contains(ChatColor.GRAY + "Hope")) {
				hopeAdded = true;
			}

			if (loreEntry.contains("King's Valley")) {
				kingsValleyFound = true;
			}

			if (!hopeAdded && (loreEntry.contains("King's Valley") ||
			                   loreEntry.contains("Armor") ||
			                   loreEntry.contains("Magic Wand") ||
			                   ChatColor.stripColor(loreEntry).trim().isEmpty())) {
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
			error(sender, "Player must have a King's Valley item in their main hand!");
			return;
		}

		meta.setLore(newLore);
		item.setItemMeta(meta);
		item.setAmount(1);
		sender.sendMessage("Succesfully added hope to player's held item");
	}
}
