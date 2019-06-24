package com.playmonumenta.plugins.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ItemUtils.ItemRegion;

import io.github.jorelali.commandapi.api.CommandAPI;
import net.md_5.bungee.api.ChatColor;


public class ReforgeInventory extends GenericCommand {

	public static void register() {
		registerPlayerCommand("reforgeinventory", "monumenta.command.reforgeinventory", (sender, player) -> {
			run(sender, player);
		});
	}

	private static void run(CommandSender sender, Player player) throws CommandSyntaxException {
		if (player.hasMetadata("PlayerCanReforge")) {
			player.removeMetadata("PlayerCanReforge", Plugin.getInstance());
			Map<ItemStack, List<String>> toReforge = new HashMap<>();
			for (ItemStack item : player.getInventory()) {
				if (item != null && item.getLore() != null) {
					List<String> oldLore = item.getLore();
					List<String> newLore = new ArrayList<>();
					boolean isShattered = false;
					for (String loreEntry : oldLore) {
						if (loreEntry.contains(ChatColor.DARK_RED + "" + ChatColor.BOLD + "* SHATTERED *") ||
						    loreEntry.contains(ChatColor.DARK_RED + "Maybe a Master Repairman") ||
						    loreEntry.contains(ChatColor.DARK_RED + "could reforge it...")) {
							isShattered = true;
						} else {
							newLore.add(loreEntry);
						}
					}
					if (isShattered) {
						toReforge.put(item, newLore);
					}
				}
			}
			if (!toReforge.isEmpty()) {
				// Get the cost to reforge the entire inventory
				Map<ItemRegion, Integer> reforgeCost = ItemUtils.getReforgeCosts(toReforge.keySet());
				int cxpCost = reforgeCost.getOrDefault(ItemRegion.KINGS_VALLEY, 0);
				int ccsCost = reforgeCost.getOrDefault(ItemRegion.CELSIAN_ISLES, 0);
				int cmmCost = reforgeCost.getOrDefault(ItemRegion.MONUMENTA, 0);
				if (cmmCost != 0) {
					// If the player has any "Monumenta :" items, convert the cost to repair that item into the current region's currency, or CXP by default.
					if (player.getWorld().getName() == "Project_Epic-region_1") {
						cxpCost += cmmCost;
					} else if (player.getWorld().getName() == "Project_Epic-region_2") {
						ccsCost += cmmCost;
					} else {
						cxpCost += cmmCost;
					}
				}
				boolean paid = false;
				ItemStack cxp = CalculateReforge.mCXP.clone();
				ItemStack hxp = CalculateReforge.mHXP.clone();
				ItemStack ccs = CalculateReforge.mCCS.clone();
				ItemStack hcs = CalculateReforge.mHCS.clone();
				PlayerInventory inventory = player.getInventory();
				// Check if the player has any valid combination of the required currency.
				// If they do, set the amount on the fake itemstacks to the correct cost and remove that many from the inventory.
				if (cxpCost != 0 && ccsCost != 0) {
					if (inventory.containsAtLeast(cxp, cxpCost) &&
					    inventory.containsAtLeast(ccs, ccsCost)) {
						cxp.setAmount(cxpCost);
						ccs.setAmount(ccsCost);
						inventory.removeItem(cxp);
						inventory.removeItem(ccs);
						paid = true;
					} else if (inventory.containsAtLeast(hxp, cxpCost / 64) &&
					           inventory.containsAtLeast(cxp, cxpCost % 64) &&
					           inventory.containsAtLeast(hcs, ccsCost / 64) &&
					           inventory.containsAtLeast(ccs, ccsCost % 64)) {
						hxp.setAmount(cxpCost / 64);
						cxp.setAmount(cxpCost % 64);
						hcs.setAmount(ccsCost / 64);
						ccs.setAmount(ccsCost % 64);
						inventory.removeItem(hxp);
						inventory.removeItem(cxp);
						inventory.removeItem(hcs);
						inventory.removeItem(ccs);
						paid = true;
					}
				} else if (cxpCost != 0) {
					if (inventory.containsAtLeast(cxp, cxpCost)) {
						cxp.setAmount(cxpCost);
						inventory.removeItem(cxp);
						paid = true;
					} else if (inventory.containsAtLeast(hxp, cxpCost / 64) &&
					           inventory.containsAtLeast(cxp, cxpCost % 64)) {
						hxp.setAmount(cxpCost / 64);
						cxp.setAmount(cxpCost % 64);
						inventory.removeItem(hxp);
						inventory.removeItem(cxp);
						paid = true;
					}
				} else if (ccsCost != 0) {
					if (inventory.containsAtLeast(ccs, ccsCost)) {
						ccs.setAmount(ccsCost);
						inventory.removeItem(ccs);
						paid = true;
					} else if (inventory.containsAtLeast(hcs, ccsCost / 64) &&
					           inventory.containsAtLeast(ccs, ccsCost % 64)) {
						hcs.setAmount(ccsCost / 64);
						ccs.setAmount(ccsCost % 64);
						inventory.removeItem(hcs);
						inventory.removeItem(ccs);
						paid = true;
					}
				}
				if (paid) {
					// Set the lore to the stored version with the Shattered entries removed
					for (Entry<ItemStack, List<String>> entry : toReforge.entrySet()) {
						entry.getKey().setLore(entry.getValue());
					}
					player.sendMessage("All of your items have been reforged!");
				} else {
					player.sendMessage("You can't afford that");
					CommandAPI.fail("Player doesn't have enough currency");
				}
			} else {
				player.sendMessage("You don't have any shattered items");
				CommandAPI.fail("Player must have a Shattered item in their inventory!");
			}
		}
	}
}
