package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.CommandUtils;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class UnlockHeldItem extends GenericCommand {
	public static void register() {
		registerPlayerCommand("unlockhelditem", "monumenta.command.unlockhelditem", UnlockHeldItem::run);
	}

	public static void run(CommandSender sender, Player player) throws WrapperCommandSyntaxException {
		CommandUtils.deEnchantifyHeldItem(sender, player, "Locked");
	}
}
