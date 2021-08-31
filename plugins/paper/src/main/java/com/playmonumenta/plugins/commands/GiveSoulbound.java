package com.playmonumenta.plugins.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.ItemStackArgument;
import dev.jorel.commandapi.arguments.LootTableArgument;
import net.kyori.adventure.text.Component;

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

		new CommandAPICommand("givesoulbound")
			.withPermission(perms)
			.withArguments(new EntitySelectorArgument("players", EntitySelector.MANY_PLAYERS))
			.withArguments(new ItemStackArgument("item"))
			.executes((sender, args) -> {
				for (Player player : (Collection<Player>)args[0]) {
					give(player, (ItemStack)args[1]);
				}
			})
			.register();

		new CommandAPICommand("givesoulbound")
			.withPermission(perms)
			.withArguments(new EntitySelectorArgument("players", EntitySelector.MANY_PLAYERS))
			.withArguments(new LootTableArgument("item_loot_table"))
			.executes((sender, args) -> {
				for (Player player : (Collection<Player>)args[0]) {
					LootTable table = (LootTable)args[1];
					if (table != null) {
						Collection<ItemStack> loot = table.populateLoot(FastUtils.RANDOM, new LootContext.Builder(player.getLocation()).build());
						if (!loot.isEmpty()) {
							/* Give the player the first item in the collection */
							give(player, loot.iterator().next());
						}
					}
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
		ItemUtils.setPlainTag(stack);
		InventoryUtils.giveItem(player, stack);
	}
}
