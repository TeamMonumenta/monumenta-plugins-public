package com.playmonumenta.plugins.commands;

import java.util.LinkedHashMap;

import com.playmonumenta.plugins.utils.CommandUtils;

import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

/*
 * NOTICE!
 * If this enchantment gets changed, make sure someone updates the Python item replacement code to match!
 * Constants and new enchantments included!
 * This most likely means @NickNackGus or @Combustible
 * If this does not happen, your changes will NOT persist across weekly updates!
 */
public class BarkifyHeldItem extends GenericCommand {
	static final String COMMAND = "barkifyhelditem";

	private static void registerType(String selection) {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.barkifyhelditem");
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put(selection, new LiteralArgument(selection));
		arguments.put("player", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				run(sender, (Player)args[0], selection);
			})
			.register();
	}

	public static void register() {
		registerType("Barking");
		registerType("Barking2");
		registerType("Debarking");
	}

	private static void run(CommandSender sender, Player player, String selection) throws WrapperCommandSyntaxException {
		if (selection == "Barking2") {
			selection = "Barking II";
		}

		CommandUtils.enchantify(sender, player, selection);

		if (selection == "Barking") {
			player.playSound(player.getLocation(), Sound.ENTITY_WOLF_AMBIENT, 1, 1f);
		} else if (selection == "Barking II") {
			player.playSound(player.getLocation(), Sound.ENTITY_WOLF_WHINE, 1, 1f);
		} else {
			player.playSound(player.getLocation(), Sound.ENTITY_WOLF_GROWL, 1, 1f);
		}
	}
}
