package com.playmonumenta.plugins.commands;

import java.util.ArrayList;
import java.util.List;

import com.playmonumenta.plugins.utils.CommandUtils;

import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;

/*
 * NOTICE!
 * If this enchantment gets changed, make sure someone updates the Python item replacement code to match!
 * Constants and new enchantments included!
 * This most likely means @NickNackGus or @Combustible
 * If this does not happen, your changes will NOT persist across weekly updates!
 */
public class DeBarkifyHeldItem extends GenericCommand {
	public static void register() {
		List<Argument> arguments = new ArrayList<>();

		arguments.add(new MultiLiteralArgument("Barking", "Debarking"));
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));
		new CommandAPICommand("debarkifyhelditem")
			.withPermission(CommandPermission.fromString("monumenta.command.debarkifyhelditem"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				CommandUtils.deEnchantifyHeldItem(sender, (Player)args[1], (String)args[0]);
			})
			.register();
	}
}
