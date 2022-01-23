package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.ItemStatUtils;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PhylacteryifyHeldItem extends GenericCommand {
	public static void register() {
		registerPlayerCommand("phylacteryifyhelditem", "monumenta.command.phylacteryifyhelditem", PhylacteryifyHeldItem::run);
	}

	public static void run(CommandSender sender, Player player) throws WrapperCommandSyntaxException {
		ItemStack item = player.getItemInHand();
		ItemStatUtils.addInfusion(item, ItemStatUtils.InfusionType.PHYLACTERY, 1, player.getUniqueId());
		ItemStatUtils.generateItemStats(item);
	}
}
