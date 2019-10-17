package com.playmonumenta.plugins.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ItemUtils.ItemRegion;

import io.github.jorelali.commandapi.api.CommandAPI;


public class ReforgeInventory extends GenericCommand {

	public static void register() {
		registerPlayerCommand("reforgeinventory", "monumenta.command.reforgeinventory", (sender, player) -> {
			run(sender, player);
		});
	}

	private static void run(CommandSender sender, Player player) throws CommandSyntaxException {
		if (player.hasMetadata("PlayerCanReforge")) {
			player.removeMetadata("PlayerCanReforge", Plugin.getInstance());
			// Get the cost to reforge the entire inventory
			List<ItemStack> toReforge = new ArrayList<>();
			for (ItemStack item : player.getInventory()) {
				if (ItemUtils.isItemShattered(item)) {
					toReforge.add(item);
				}
			}
			Map<ItemRegion, Integer> reforgeCost = ItemUtils.getReforgeCosts(toReforge);
			int cxpCost = reforgeCost.getOrDefault(ItemRegion.KINGS_VALLEY, 0);
			int ccsCost = reforgeCost.getOrDefault(ItemRegion.CELSIAN_ISLES, 0);
			int cmmCost = reforgeCost.getOrDefault(ItemRegion.MONUMENTA, 0);
			if (cmmCost != 0) {
				// ItemRegion.MONUMENTA currently only exists to allow items from
				// Celsian Isles to be used in King's Valley. So the cost to reforge is Celsian Isles currency
				// We need to figure out a more permanent solution to this at some point.
				ccsCost += cmmCost;
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
			} else {
				// No items were shattered
				player.sendMessage("You don't have any shattered items");
				CommandAPI.fail("Player must have a Shattered item in their inventory!");
				return;
			}
			if (paid) {
				for (ItemStack item : toReforge) {
					ItemUtils.reforgeItem(item);
				}
				player.sendMessage("All of your items have been reforged!");
			} else {
				player.sendMessage("You can't afford that");
				CommandAPI.fail("Player doesn't have enough currency");
			}
		}
	}
}
