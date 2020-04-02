package com.playmonumenta.plugins.commands;

import java.util.LinkedHashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.playmonumenta.plugins.utils.SpringEventUtils;
import com.playmonumenta.plugins.utils.SpringEventUtils.City;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.LiteralArgument;

public class SpringCleanItems extends GenericCommand {
	@SuppressWarnings("unchecked")
	public static void registerType(City city) {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put(city.getLabel(), new LiteralArgument(city.getLabel()));
		arguments.put("player", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));
		arguments.put("items", new EntitySelectorArgument(EntitySelector.MANY_ENTITIES));
		CommandAPI.getInstance().register("springcleanitems", CommandPermission.fromString("monumenta.command.springcleanitems"), arguments,
			(sender, args) -> {
				run(sender, (Player)args[0], (List<Entity>)args[1], city);
			});

	}

	public static void register() {
		registerType(City.SIERHAVEN);
		registerType(City.NYR);
		registerType(City.FARR);
		registerType(City.LOWTIDE);
		registerType(City.TAELDIM);
		registerType(City.HIGHWATCH);
		registerType(City.MISTPORT);
		registerType(City.ALNERA);
		registerType(City.RAHKERI);
		registerType(City.MOLTA);
		registerType(City.FROSTGATE);
		registerType(City.NIGHTROOST);
		registerType(City.WISPERVALE);
		registerType(City.STEELMELD);
	}

	@SuppressWarnings("unchecked")
	private static void run(CommandSender sender, Player player, List<? extends Entity> items, City city) throws CommandSyntaxException {
		for (Entity entity : items) {
			if (!(entity instanceof Item)) {
				CommandAPI.fail("Got entity '" + entity.getType().toString() + "' that was not an item.");
			}
		}

		try {
			SpringEventUtils.doClean(sender, player, (List<Item>)items, city);
		} catch (CommandSyntaxException ex) {
			/* Let the player also know why it failed */
			player.sendMessage(ChatColor.GOLD + "[Spring Cleaner] " + ChatColor.RED + ex.getMessage());
			/* Continue to propagate the failure */
			throw ex;
		}
	}
}
