package com.playmonumenta.plugins.utils;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class MonumentUtils {
	public static void completeR2BonusMonument(Player player) {

		ItemStack[] inventory = player.getInventory().getContents();
		boolean hasBeigeWool = false;
		boolean hasFuschiaWool = false;
		boolean hasMaroonWool = false;
		boolean hasJadeWool = false;
		boolean hasLilacWool = false;
		for (ItemStack item : inventory) {
			if (item.getLore().contains("Beige Wool")) {
				hasBeigeWool = true;
			}
			if (item.getLore().contains("Fuschia Wool")) {
				hasFuschiaWool = true;
			}
			if (item.getLore().contains("Maroon Wool")) {
				hasMaroonWool = true;
			}
			if (item.getLore().contains("Jade Wool")) {
				hasJadeWool = true;
			}
			if (item.getLore().contains("Lilac Wool")) {
				hasLilacWool = true;
			}
		}

		if (hasBeigeWool && hasFuschiaWool && hasMaroonWool && hasJadeWool && hasLilacWool) {
			PlayerUtils.executeCommandOnNearbyPlayers(player.getLocation(), 5, "tellraw @s [\"\",{\"text\":\"You have completed the R2 Bonus Monument! You have gained an extra spec point!\",\"color\":\"dark_red\"}]");
		}
	}

}
