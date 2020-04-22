package com.playmonumenta.plugins.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.utils.CommandUtils;

import io.github.jorelali.commandapi.api.exceptions.WrapperCommandSyntaxException;

/*
 * NOTICE!
 * If this enchantment gets changed, make sure someone updates the Python item replacement code to match!
 * Constants and new enchantments included!
 * This most likely means @NickNackGus or @Combustible
 * If this does not happen, your changes will NOT persist across weekly updates!
 */
public class FestiveHeldItem extends GenericCommand {
	public static void register() {
		registerPlayerCommand("festivehelditem", "monumenta.command.festivehelditem",
		                      (sender, player) -> {
		                          run(sender, player);
		                      });
	}

	private static void run(CommandSender sender, Player player) throws WrapperCommandSyntaxException {
		CommandUtils.enchantify(sender, player, "Festive", "Decorated by");
	}
}
