package com.playmonumenta.plugins.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.utils.CommandUtils;

import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class ColossalifyHeldItem extends GenericCommand {
	public static void register() {
		registerPlayerCommand("colossalifyhelditem", "monumenta.command.colossalifyhelditem", ColossalifyHeldItem::run);
	}

	private static void run(CommandSender sender, Player player) throws WrapperCommandSyntaxException {
		CommandUtils.enchantify(sender, player, "Colossal", "Reinforced by");
	}
}
