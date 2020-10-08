package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;

import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import net.md_5.bungee.api.ChatColor;


public class ShatterHeldItem extends GenericCommand {
	public static void register() {
		registerPlayerCommand("shatterhelditem", "monumenta.command.shatterhelditem", ShatterHeldItem::run);
	}

	private static void run(CommandSender sender, Player player) throws WrapperCommandSyntaxException {
		ItemStack item = player.getInventory().getItemInMainHand();
		ItemUtils.shatterItem(item);
		if (InventoryUtils.testForItemWithLore(item, (ChatColor.DARK_RED + "" + ChatColor.BOLD + "* SHATTERED *"))) {
			player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 0.8f);
			sender.sendMessage("Successfully shattered player's held item.");
		} else {
			sender.sendMessage(ChatColor.DARK_RED + "Item can not be shattered! (Are you sure it's an item that can shatter?)");
		}
	}
}
