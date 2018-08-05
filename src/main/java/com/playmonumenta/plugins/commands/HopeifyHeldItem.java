package com.playmonumenta.plugins.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.playmonumenta.plugins.Plugin;

public class HopeifyHeldItem implements CommandExecutor {
	Plugin mPlugin;

	public HopeifyHeldItem(Plugin plugin) {
		mPlugin = plugin;
	}


	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length != 0) {
			sender.sendMessage(ChatColor.RED + "This command takes no arguments!");
			return false;
		}

		Player player = null;
		if (sender instanceof Player) {
			player = (Player)sender;
		} else if (sender instanceof ProxiedCommandSender) {
			CommandSender callee = ((ProxiedCommandSender)sender).getCallee();
			if (callee instanceof Player) {
				player = (Player)callee;
			}
		}

		if (player == null) {
            sender.sendMessage(ChatColor.RED + "This command must be run by/on a player!");
			return false;
		}


		ItemStack item = player.getEquipment().getItemInMainHand();
		if (item == null) {
            sender.sendMessage(ChatColor.RED + "Player must have a King's Valley item in their main hand!");
			return false;
		}

		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
            sender.sendMessage(ChatColor.RED + "Player must have a King's Valley item in their main hand!");
			return false;
		}

		List<String> lore = meta.getLore();
		if (lore == null || lore.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Player must have a King's Valley item in their main hand!");
			return false;
		}

		List<String> newLore = new ArrayList<String>();
		boolean hopeAdded = false;
		boolean nameAdded = false;
		boolean kingsValleyFound = false;
		for (String loreEntry : lore) {
			if (loreEntry.contains(ChatColor.GRAY + "Hope")) {
				sender.sendMessage(ChatColor.RED + "Player's item already has hope");
				return false;
			}

			if (loreEntry.contains("King's Valley")) {
				kingsValleyFound = true;
			}

			if (hopeAdded == false && (loreEntry.contains("King's Valley")
			                           || ChatColor.stripColor(loreEntry).trim().isEmpty())) {
				newLore.add(ChatColor.GRAY + "Hope");
				hopeAdded = true;
			}

			if (nameAdded == false && ChatColor.stripColor(loreEntry).trim().isEmpty()) {
				newLore.add("Infused by " + player.getName());
				nameAdded = true;
			}

			newLore.add(loreEntry);
		}

		if (nameAdded == false) {
			newLore.add("Infused by " + player.getName());
		}

		if (!kingsValleyFound) {
            sender.sendMessage(ChatColor.RED + "Player must have a King's Valley item in their main hand!");
			return false;
		}

		meta.setLore(newLore);
		item.setItemMeta(meta);
		sender.sendMessage("Succesfully added hope to player's held item");

		return true;
	}
}
