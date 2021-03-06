package com.playmonumenta.plugins.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ItemUtils.ItemRegion;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

/*
 * NOTICE!
 * If this enchantment gets changed, make sure someone updates the Python item replacement code to match!
 * Constants and new enchantments included!
 * This most likely means @NickNackGus or @Combustible
 * If this does not happen, your changes will NOT persist across weekly updates!
 */
public class ReforgeInventory extends GenericCommand {

	public static void register() {
		registerPlayerCommand("reforgeinventory", "monumenta.command.reforgeinventory", (sender, player) -> {
			run(player);
		});
	}

	private static void run(Player player) throws WrapperCommandSyntaxException {
		// If the player is in creative, reforge for free.
		if (player.getGameMode() == GameMode.CREATIVE) {
			for (ItemStack item : player.getInventory()) {
				if (ItemUtils.isItemShattered(item)) {
					ItemUtils.reforgeItem(item);
				}
			}
			player.sendMessage("All of your items have been reforged!");
		}
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
			int csbCost = reforgeCost.getOrDefault(ItemRegion.SHULKER_BOX, 0);
			if (cmmCost != 0) {
				// ItemRegion.MONUMENTA currently only exists to allow items from
				// Celsian Isles to be used in King's Valley. So the cost to reforge is Celsian Isles currency
				// We need to figure out a more permanent solution to this at some point.
				ccsCost += cmmCost;
			}
			if (csbCost != 0) {
				// ItemRegion.SHULKER_BOX currently only exists to allow shulker boxes to be
				// reforged with either King's Valley or Celsian Isles currency.
				// We need to figure out a more permanent solution to this at some point.
				if (player.getWorld().getName().equals("Project_Epic-region_1")) {
					// King's Valley: Use XP
					cxpCost += csbCost;
				} else if (player.getWorld().getName().equals("Project_Epic-region_2")) {
					// Celsian Isles: Use CS
					ccsCost += csbCost;
				} else {
					// This shouldn't happen, but if it does, default to XP
					cxpCost += csbCost;
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
