package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.ItemStatUtils;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/*
 * NOTICE!
 * If this enchantment gets changed, make sure someone updates the Python item replacement code to match!
 * Constants and new enchantments included!
 * This most likely means @NickNackGus or @Combustible
 * If this does not happen, your changes will NOT persist across weekly updates!
 */
public class FestiveHeldItem extends GenericCommand {
	public static void register() {
		registerPlayerCommand("festivehelditem", "monumenta.command.festivehelditem", FestiveHeldItem::run);
	}

	public static void run(CommandSender sender, Player player) throws WrapperCommandSyntaxException {
		ItemStatUtils.addInfusion(player.getInventory().getItemInMainHand(), ItemStatUtils.InfusionType.FESTIVE, 1, player.getUniqueId());
	}
}
