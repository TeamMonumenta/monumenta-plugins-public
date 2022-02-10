package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.ItemStatUtils;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ColossalifyHeldItem extends GenericCommand {
	public static void register() {
		registerPlayerCommand("colossalifyhelditem", "monumenta.command.colossalifyhelditem", ColossalifyHeldItem::run);
	}

	public static void run(CommandSender sender, Player player) throws WrapperCommandSyntaxException {
		ItemStatUtils.addInfusion(player.getInventory().getItemInMainHand(), ItemStatUtils.InfusionType.COLOSSAL, 1, player.getUniqueId());
	}
}
