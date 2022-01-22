package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.ItemStatUtils;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class LockedHeldItem extends GenericCommand {
	public static void register() {
		registerPlayerCommand("lockhelditem", "monumenta.command.lockhelditem", LockedHeldItem::run);
	}

	public static void run(CommandSender sender, Player player) throws WrapperCommandSyntaxException {
		ItemStack item = player.getItemInHand();
		ItemStatUtils.addInfusion(item, ItemStatUtils.InfusionType.LOCKED, 1, player.getUniqueId());
		ItemStatUtils.generateItemStats(item);
	}
}
