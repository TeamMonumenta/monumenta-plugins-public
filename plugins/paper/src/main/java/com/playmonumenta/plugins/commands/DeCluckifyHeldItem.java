package com.playmonumenta.plugins.commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.exceptions.WrapperCommandSyntaxException;


/*
 * NOTICE!
 * If this enchantment gets changed, make sure someone updates the Python item replacement code to match!
 * Constants and new enchantments included!
 * This most likely means @NickNackGus or @Combustible
 * If this does not happen, your changes will NOT persist across weekly updates!
 */
public class DeCluckifyHeldItem extends GenericCommand {
	public static void register() {
		registerPlayerCommand("decluckifyhelditem", "monumenta.command.decluckifyhelditem",
		                      (sender, player) -> {
		                          run(sender, player);
		                      });
	}

	private static void run(CommandSender sender, Player player) throws WrapperCommandSyntaxException {
		ItemStack item = player.getEquipment().getItemInMainHand();
		if (item == null) {
			CommandAPI.fail("Player must have a Clucking item in their main hand!");
		}

		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			CommandAPI.fail("Player must have a Clucking item in their main hand!");
		}

		List<String> lore = meta.getLore();
		if (lore == null || lore.isEmpty()) {
			CommandAPI.fail("Player must have a Clucking item in their main hand!");
		}

		List<String> newLore = new ArrayList<>();
		boolean hasClucking = false;
		for (String loreEntry : lore) {
			if (loreEntry.contains(ChatColor.GRAY + "Clucking")) {
				hasClucking = true;
			} else {
				newLore.add(loreEntry);
			}
		}

		if (!hasClucking) {
			CommandAPI.fail("Player must have a Clucking item in their main hand!");
		} else {
			meta.setLore(newLore);
			item.setItemMeta(meta);

			sender.sendMessage("Succesfully removed Clucking from the player's held item");
		}
	}
}
