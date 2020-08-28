package com.playmonumenta.plugins.commands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.LiteralArgument;
import io.github.jorelali.commandapi.api.exceptions.WrapperCommandSyntaxException;

/*
 * NOTICE!
 * If this enchantment gets changed, make sure someone updates the Python item replacement code to match!
 * Constants and new enchantments included!
 * This most likely means @NickNackGus or @Combustible
 * If this does not happen, your changes will NOT persist across weekly updates!
 */
public class BarkifyHeldItem extends GenericCommand {
	static final String COMMAND = "barkifyhelditem";
	static final String PERMISSION = "monumenta.command.barkifyhelditem";

	private static void registerType(String selection) {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put(selection, new LiteralArgument(selection));
		CommandAPI.getInstance().register(COMMAND, CommandPermission.fromString(PERMISSION), arguments,
			(sender, args) -> {
			});

		arguments.clear();
		arguments.put(selection, new LiteralArgument(selection));
		arguments.put("player", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));
		CommandAPI.getInstance().register(COMMAND, CommandPermission.fromString(PERMISSION), arguments,
			(sender, args) -> {
				run(sender, (Player)args[0], selection);
			});
	}

	public static void register() {
		registerType("Barking");
		registerType("Barking2");
		registerType("Debarking");
	}

	private static void run(CommandSender sender, Player player, String selection) throws WrapperCommandSyntaxException {
		List<String> newLore = new ArrayList<>();
		ItemStack item = player.getEquipment().getItemInMainHand();
		ItemMeta meta = item.getItemMeta();
		List<String> lore = meta.getLore();
		boolean enchantmentFound = false;
		if (selection == "Barking2") {
			selection = "Barking II";
		}

		for (String loreEntry : lore) {
			if (loreEntry.contains(ChatColor.GRAY + selection)) {
				enchantmentFound = true;
			}

			String loreStripped = ChatColor.stripColor(loreEntry).trim();
			if (!enchantmentFound && (loreStripped.contains("King's Valley :") ||
			                          loreStripped.contains("Celsian Isles :") ||
			                          loreStripped.contains("Monumenta :") ||
			                          loreStripped.contains("Armor") ||
			                          loreStripped.contains("Magic Wand") ||
			                          loreStripped.isEmpty())) {
				newLore.add(ChatColor.GRAY + selection);
				enchantmentFound = true;
			}
				newLore.add(loreEntry);
		}
		meta.setLore(newLore);
		item.setItemMeta(meta);

		if (selection == "Barking") {
			player.playSound(player.getLocation(), Sound.ENTITY_WOLF_AMBIENT, 1, 1f);
		} else if (selection == "Barking II") {
			player.playSound(player.getLocation(), Sound.ENTITY_WOLF_WHINE, 1, 1f);
		} else {
			player.playSound(player.getLocation(), Sound.ENTITY_WOLF_GROWL, 1, 1f);
		}

		sender.sendMessage("Succesfully added " + selection + " to player's held item");
	}
}
