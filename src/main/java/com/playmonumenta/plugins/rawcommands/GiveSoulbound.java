package com.playmonumenta.plugins.rawcommands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.playmonumenta.plugins.utils.InventoryUtils;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.ItemStackArgument;

public class GiveSoulbound extends GenericCommand {
	@SuppressWarnings("unchecked")
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.givesoulbound");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("players", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS));
		arguments.put("item", new ItemStackArgument());
		CommandAPI.getInstance().register("givesoulbound",
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
		                                      for (Player player : (Collection<Player>)args[0]) {
												  give(sender, player, (ItemStack)args[1]);
		                                      }
		                                  }
		);
	}

	private static void give(CommandSender sender, Player player, ItemStack stack) {
		ItemMeta meta = null;
		if (stack.hasItemMeta()) {
			meta = stack.getItemMeta();
		} else {
			meta = Bukkit.getServer().getItemFactory().getItemMeta(stack.getType());
		}

		List<String> lore = null;
		if (meta.hasLore()) {
			lore = meta.getLore();
		} else {
			lore = new ArrayList<String>();
		}

		lore.add("* Soulbound to " + player.getName() + " *");
		meta.setLore(lore);
		stack.setItemMeta(meta);
		InventoryUtils.giveItem(player, stack);
	}
}
