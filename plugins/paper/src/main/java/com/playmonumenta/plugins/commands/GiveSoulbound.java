package com.playmonumenta.plugins.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.kyori.adventure.text.Component;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.ItemStackArgument;

/*
 * NOTICE!
 * If this enchantment gets changed, make sure someone updates the Python item replacement code to match!
 * Constants and new enchantments included!
 * This most likely means @NickNackGus or @Combustible
 * If this does not happen, your changes will NOT persist across weekly updates!
 */
public class GiveSoulbound extends GenericCommand {
	@SuppressWarnings("unchecked")
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.givesoulbound");

		List<Argument> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument("players", EntitySelector.MANY_PLAYERS));
		arguments.add(new ItemStackArgument("item"));
		new CommandAPICommand("givesoulbound")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				for (Player player : (Collection<Player>)args[0]) {
					give(player, (ItemStack)args[1]);
				}
			})
			.register();
	}

	private static void give(Player player, ItemStack stack) {
		ItemMeta meta = null;
		if (stack.hasItemMeta()) {
			meta = stack.getItemMeta();
		} else {
			meta = Bukkit.getServer().getItemFactory().getItemMeta(stack.getType());
		}

		List<Component> lore = null;
		if (meta.hasLore()) {
			lore = meta.lore();
		} else {
			lore = new ArrayList<>();
		}

		lore.add(Component.text("* Soulbound to " + player.getName() + " *"));
		meta.lore(lore);
		stack.setItemMeta(meta);
		stack = ItemUtils.setPlainLore(stack);
		InventoryUtils.giveItem(player, stack);
	}
}
