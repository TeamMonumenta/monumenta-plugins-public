package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.CommandUtils;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class LockedHeldItem extends GenericCommand {
	public static void register() {
		registerPlayerCommand("lockhelditem", "monumenta.command.lockhelditem", LockedHeldItem::run);
	}

	public static void run(CommandSender sender, Player player) throws WrapperCommandSyntaxException {
		CommandUtils.enchantify(sender, player, "Locked");
	}
}
