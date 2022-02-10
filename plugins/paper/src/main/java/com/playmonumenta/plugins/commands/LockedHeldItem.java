package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.ItemStatUtils;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LockedHeldItem extends GenericCommand {
	public static void register() {
		registerPlayerCommand("lockhelditem", "monumenta.command.lockhelditem", LockedHeldItem::run);
	}

	public static void run(CommandSender sender, Player player) throws WrapperCommandSyntaxException {
		ItemStatUtils.addInfusion(player.getInventory().getItemInMainHand(), ItemStatUtils.InfusionType.LOCKED, 1, player.getUniqueId());
	}
}
