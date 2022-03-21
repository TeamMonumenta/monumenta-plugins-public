package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.ItemStatUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class ShatterHeldItem extends GenericCommand {
	public static void register() {
		registerPlayerCommand("shatterhelditem", "monumenta.command.shatterhelditem", ShatterHeldItem::run);
	}

	private static void run(CommandSender sender, Player player) {
		ItemStack item = player.getInventory().getItemInMainHand();
		if (ItemStatUtils.shatter(item)) {
			player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 0.8f);
			sender.sendMessage("Successfully shattered player's held item.");
		} else {
			sender.sendMessage(ChatColor.DARK_RED + "Item can not be shattered! (Are you sure it's an item that can shatter?)");
		}
	}
}
