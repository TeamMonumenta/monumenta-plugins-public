package com.playmonumenta.plugins.inventories;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootContext.Builder;
import org.bukkit.loot.LootTable;

import com.playmonumenta.plugins.utils.InventoryUtils;

public class LootChestsInInventory implements Listener {
	private static List<Player> lootMen = new ArrayList<>();

	@EventHandler(priority = EventPriority.LOW)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (!event.getClick().equals(ClickType.RIGHT)) {
			return;
		}

		ItemStack item = event.getCurrentItem();

		if (!item.getType().equals(Material.CHEST)) {
			return;
		}

		BlockStateMeta chestMeta = (BlockStateMeta)item.getItemMeta();

		if (chestMeta == null) {
			return;
		}

		BlockState state = chestMeta.getBlockState();

		if (state == null) {
			return;
		}

		Chest chest = (Chest)state;
		LootTable table = chest.getLootTable();

		if (table == null) {
			return;
		}

		Inventory inventory = Bukkit.createInventory(null, 27, item.getI18NDisplayName());
		Random random = new Random();
		Builder builder = new LootContext.Builder(event.getWhoClicked().getLocation());
		Collection<ItemStack> loot =  table.populateLoot(random, builder.build());

		for (ItemStack lootItem : loot) {
			inventory.addItem(lootItem);
		}

		item.subtract();
		lootMen.add((Player)event.getWhoClicked());
		event.getWhoClicked().openInventory(inventory);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		if (lootMen.contains(event.getPlayer())) {
			ItemStack[] items = event.getView().getTopInventory().getContents();
			for (ItemStack item : items) {
				if (item != null) {
					InventoryUtils.giveItem((Player)event.getPlayer(), item);
				}
			}
			lootMen.remove(event.getPlayer());
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void playerJoinEvent(PlayerJoinEvent event) {
		if (lootMen.contains(event.getPlayer())) {
			lootMen.remove(event.getPlayer());
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void playerQuitEvent(PlayerQuitEvent event) {
		if (lootMen.contains(event.getPlayer())) {
			lootMen.remove(event.getPlayer());
		}
	}
}
