package com.playmonumenta.plugins.commands;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.bukkit.ChatColor;
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
public class DeBarkifyHeldItem extends GenericCommand {
	static final String COMMAND = "debarkifyhelditem";
	static final String PERMISSION = "monumenta.command.debarkifyhelditem";

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
		registerType("Debarking");
	}

	private static void run(CommandSender sender, Player player, String selection) throws WrapperCommandSyntaxException {
		ItemStack item = player.getInventory().getItemInMainHand();

		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			CommandAPI.fail("Player must have a " + selection + " item in their main hand!");
		}

		List<String> lore = meta.getLore();
		if (lore == null || lore.isEmpty()) {
			CommandAPI.fail("Player must have a " + selection + " item in their main hand!");
		}
		if (selection == "Barking") {
			List<String> newLore = new ArrayList<>();
			boolean hasBarking = false;
			for (String loreEntry : lore) {
				if (loreEntry.contains(ChatColor.GRAY + "Barking")) {
					hasBarking = true;
				} else {
					newLore.add(loreEntry);
				}
			}

			if (!hasBarking) {
				CommandAPI.fail("Player must have a Barking item in their main hand!");
			} else {
				meta.setLore(newLore);
				item.setItemMeta(meta);

				sender.sendMessage("Successfully removed Barking from the player's held item");
			}
		} else {
			List<String> newLore = new ArrayList<>();
			boolean hasDebarking = false;
			for (String loreEntry : lore) {
				if (loreEntry.contains(ChatColor.GRAY + "Debarking")) {
					hasDebarking = true;
				} else {
					newLore.add(loreEntry);
				}
			}

			if (!hasDebarking) {
				CommandAPI.fail("Player must have a Debarking item in their main hand!");
			} else {
				meta.setLore(newLore);
				item.setItemMeta(meta);

				sender.sendMessage("Successfully removed Debarking from the player's held item");
			}
		}
	}
}
