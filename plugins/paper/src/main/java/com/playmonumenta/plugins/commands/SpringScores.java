package com.playmonumenta.plugins.commands;

import java.util.LinkedHashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.utils.SpringEventUtils;
import com.playmonumenta.plugins.utils.SpringEventUtils.City;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.LiteralArgument;
import io.github.jorelali.commandapi.api.exceptions.WrapperCommandSyntaxException;

public class SpringScores {
	public static void registerType(City city) {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put(city.getLabel(), new LiteralArgument(city.getLabel()));
		arguments.put("player", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));
		CommandAPI.getInstance().register("springscores", CommandPermission.fromString("monumenta.command.springscores.city"), arguments,
			(sender, args) -> {
				run(sender, (Player)args[0], city);
			});
	}

	public static void registerType() {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("player", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));
		CommandAPI.getInstance().register("springscores", CommandPermission.fromString("monumenta.command.springscores.player"), arguments,
			(sender, args) -> {
				run(sender, (Player)args[0]);
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
		registerType();
	}

	private static void run(CommandSender sender, Player player, City city) throws WrapperCommandSyntaxException {
		try {
			SpringEventUtils.displayCityStats(sender, player, city);
		} catch (WrapperCommandSyntaxException ex) {
			/* Let the player also know why it failed */
			player.sendMessage(ChatColor.GOLD + "[Spring Cleaner] " + ChatColor.RED + ex.getMessage());
			/* Continue to propagate the failure */
			throw ex;
		}
	}

	private static void run(CommandSender sender, Player player) throws WrapperCommandSyntaxException {
		try {
			SpringEventUtils.displayPlayerStats(sender, player);
		} catch (WrapperCommandSyntaxException ex) {
			/* Let the player also know why it failed */
			player.sendMessage(ChatColor.GOLD + "[Spring Cleaner] " + ChatColor.RED + ex.getMessage());
			/* Continue to propagate the failure */
			throw ex;
		}
	}
}
