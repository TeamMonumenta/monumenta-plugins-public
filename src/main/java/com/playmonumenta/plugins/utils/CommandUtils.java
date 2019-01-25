package com.playmonumenta.plugins.utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.playmonumenta.plugins.point.AreaBounds;
import com.playmonumenta.plugins.point.Point;

import io.github.jorelali.commandapi.api.CommandAPI;

public class CommandUtils {

	/**
	 * Gets a CommandSender's location (player, command block, /execute, etc.)
	 *
	 * @return sender's location or raises an exception
	 */
	public static Location getLocation(CommandSender sender) throws Exception {
		return getLocation(sender, false);
	}

	public static Location getLocation(CommandSender sender, boolean doSubtractEntityOffset) throws Exception {
		if (sender == null) {
			throw new Exception("sender is null!");
		} else if (sender instanceof Entity) {
			Location senderLoc = ((Entity)sender).getLocation();
			if (doSubtractEntityOffset) {
				senderLoc.subtract(0.5, 0.5, 0.5);
			}
			return senderLoc;
		} else if (sender instanceof BlockCommandSender) {
			return ((BlockCommandSender)sender).getBlock().getLocation();
		} else if (sender instanceof ProxiedCommandSender) {
			return getLocation(((ProxiedCommandSender)sender).getCallee(), doSubtractEntityOffset);
		} else {
			throw new Exception("Failed to get required command sender coordinates");
		}
	}

	public static int parseIntFromString(CommandSender sender, String str) throws Exception {
		int value = 0;

		try {
			value = Integer.parseInt(str);
		} catch (NumberFormatException e) {
			if (sender != null) {
				error(sender, "Invalid parameter " + str + ". Must be whole number value between " + Integer.MIN_VALUE + " and " + Integer.MAX_VALUE);
			}
			throw new Exception(e);
		}

		return value;
	}

	public static double parseDoubleFromString(CommandSender sender, String str) throws Exception {
		double value = 0;

		try {
			value = Float.parseFloat(str);
		} catch (Exception e) {
			if (sender != null) {
				error(sender, "Invalid parameter " + str + ". Must be a value between " + Float.MIN_VALUE + " and " + Float.MAX_VALUE);
			}
			throw new Exception(e);
		}

		return value;
	}

	public static AreaBounds parseAreaFromString(CommandSender sender,
	                                             String xStr1, String yStr1, String zStr1,
	                                             String xStr2, String yStr2, String zStr2) throws Exception {
		Point pos1;
		Point pos2;

		try {
			pos1 = Point.fromString(sender, xStr1, yStr1, zStr1, true);
		} catch (Exception e) {
			if (sender != null) {
				error(sender, "Failed to parse first coordinate");
			}
			throw new Exception(e);
		}

		try {
			pos2 = Point.fromString(sender, xStr2, yStr2, zStr2, true);
		} catch (Exception e) {
			if (sender != null) {
				error(sender, "Failed to parse second coordinate");
			}
			throw new Exception(e);
		}

		pos2.mX += 1;
		pos2.mY += 1;
		pos2.mZ += 1;

		return new AreaBounds(pos1, pos2);
	}

	public static double parseCoordFromString(CommandSender sender,
	                                          double senderPos, String str) throws Exception {
		try {
			if (str.equals("~")) {
				return senderPos;
			} else if (str.startsWith("~")) {
				return senderPos + parseDoubleFromString(sender, str.substring(1));
			} else {
				return parseDoubleFromString(sender, str);
			}
		} catch (Exception e) {
			if (sender != null) {
				error(sender, "Failed to parse coordinate '" + str + "'");
			}
			throw new Exception(e);
		}
	}

	public static void error(CommandSender sender, String msg) {
		if ((sender instanceof Player)
			|| ((sender instanceof ProxiedCommandSender) && (((ProxiedCommandSender)sender).getCaller() instanceof Player))) {
			sender.sendMessage(ChatColor.RED + msg);
		} else {
			sender.sendMessage(msg);
		}
	}

	public static void enchantify(CommandSender sender, Player player, String region, String enchantment) throws CommandSyntaxException {
		enchantify(sender, player, region, enchantment, null);
	}

	public static void enchantify(CommandSender sender, Player player, String region, String enchantment, String ownerPrefix) throws CommandSyntaxException {
		enchantify(sender, player, region, enchantment, ownerPrefix, false);
	}

	public static void enchantify(CommandSender sender, Player player, String region, String enchantment, String ownerPrefix, boolean duplicateItem) throws CommandSyntaxException {
		ItemStack item = player.getEquipment().getItemInMainHand();
		if (item == null) {
			CommandAPI.fail("Player must have a " + region + " item in their main hand!");
		}

		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			CommandAPI.fail("Player must have a " + region + " item in their main hand!");
		}

		List<String> lore = meta.getLore();
		if (lore == null || lore.isEmpty()) {
			CommandAPI.fail("Player must have a " + region + " item in their main hand!");
		}

		List<String> newLore = new ArrayList<>();
		boolean enchantmentFound = false;
		boolean nameAdded = (ownerPrefix == null);
		boolean regionFound = false;
		for (String loreEntry : lore) {
			if (loreEntry.contains(ChatColor.GRAY + enchantment)) {
				if (duplicateItem) {
					CommandAPI.fail("Player's item already has the " + enchantment + " enchantment");
				} else {
					enchantmentFound = true;
				}
			}

			if (loreEntry.contains(region)) {
				regionFound = true;
			}

			String loreStripped = ChatColor.stripColor(loreEntry).trim();
			if (loreStripped.contains("Ephemeral Corridors") ||
			    loreStripped.contains("King's Valley : Epic") ||
			    loreStripped.contains("King's Valley : Artifact") ||
			    loreStripped.contains("King's Valley : Enhanced Rare") ||
			    loreStripped.contains("King's Valley : Enhanced Uncommon")) {
				duplicateItem = false;
			}

			if (!enchantmentFound && (loreEntry.contains(region) ||
			                          loreEntry.contains("Armor") ||
			                          loreEntry.contains("Magic Wand") ||
			                          ChatColor.stripColor(loreEntry).trim().isEmpty())) {
				newLore.add(ChatColor.GRAY + enchantment);
				enchantmentFound = true;
			}

			if (!nameAdded && ChatColor.stripColor(loreEntry).trim().isEmpty()) {
				newLore.add(ownerPrefix + " " + player.getName());
				nameAdded = true;
			}

			newLore.add(loreEntry);
		}

		if (!nameAdded) {
			newLore.add(ownerPrefix + " " + player.getName());
		}

		if (!regionFound) {
			CommandAPI.fail("Player must have a " + region + " item in their main hand!");
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

		sender.sendMessage("Succesfully added " + enchantment + " to player's held item");
	}
}
