package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.ItemStatUtils;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/*
 * NOTICE!
 * If this enchantment gets changed, make sure someone updates the Python item replacement code to match!
 * Constants and new enchantments included!
 * This most likely means @NickNackGus or @Combustible
 * If this does not happen, your changes will NOT persist across weekly updates!
 */
public class GildifyHeldItem extends GenericCommand {
	public static void register() {
		registerPlayerCommand("gildifyhelditem", "monumenta.command.gildifyhelditem", GildifyHeldItem::run);
	}

	public static void run(CommandSender sender, Player player) throws WrapperCommandSyntaxException {
		ItemStack item = player.getItemInHand();
		ItemStatUtils.addInfusion(item, ItemStatUtils.InfusionType.GILDED, 1, player.getUniqueId());
		ItemStatUtils.generateItemStats(item);
	}
}
