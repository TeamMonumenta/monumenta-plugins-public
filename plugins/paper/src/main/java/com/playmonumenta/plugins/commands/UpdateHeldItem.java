package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.ItemUtils;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class UpdateHeldItem extends GenericCommand {
	public static void register() {
		registerPlayerCommand("updatehelditem", "monumenta.command.updatehelditem", UpdateHeldItem::run);
	}

	private static void run(CommandSender sender, Player player) throws WrapperCommandSyntaxException {
		ItemStack item = player.getEquipment().getItemInMainHand();
		if (item == null || item.getAmount() <= 0) {
			CommandAPI.fail("Player must have a valid item in their main hand!");
		}
		ItemUtils.setPlainTag(item);
		player.getEquipment().setItemInMainHand(item, true);
	}
}
