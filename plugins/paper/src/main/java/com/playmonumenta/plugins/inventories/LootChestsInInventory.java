package com.playmonumenta.plugins.inventories;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCloseEvent.Reason;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootContext.Builder;
import org.bukkit.loot.LootTable;

import com.playmonumenta.plugins.utils.InventoryUtils;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import net.md_5.bungee.api.ChatColor;

public class LootChestsInInventory implements Listener {
	private static List<Player> lootMen = new ArrayList<>();

	@EventHandler(priority = EventPriority.LOW)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (!event.getClick().equals(ClickType.RIGHT)) {
			return;
		}

		ItemStack item = event.getCurrentItem();
		//Make sure it is at the very least a chest
		if (item == null) {
			return;
		}

		if (!item.getType().equals(Material.CHEST)) {
			return;
		}

		Player player = (Player)event.getWhoClicked();

		//This is needed for it to work
		NBTItem nbti = new NBTItem(item);
		NBTCompound tag = nbti.getCompound("BlockEntityTag");
		if (tag == null) {
			return;
		}
		tag.setString("id","minecraft:chest");
		ItemStack item2 = nbti.getItem();

		//Classic turning an item into a blockstate
		BlockStateMeta meta = (BlockStateMeta)item2.getItemMeta();
		BlockState state = meta.getBlockState();
		Chest chest = (Chest)state;
		//Loot tables are fun. Make sure the loot table exists
		LootTable table = chest.getLootTable();
		if (table == null) {
			return;
		}
		if (!event.getCursor().getType().equals(Material.AIR)) {
			player.sendMessage(ChatColor.DARK_RED + "You must have an empty cursor to open loot chests!");
			event.setCancelled(true);
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			return;
		}
		//Make an inventory and do some good ol roundabout population of the loot
		Inventory inventory = Bukkit.createInventory(null, 27, item.getItemMeta().getDisplayName());
		Random random = new Random();
		Builder builder = new LootContext.Builder(player.getLocation());
		Collection<ItemStack> loot = table.populateLoot(random, builder.build());
		item.subtract();
		//I hate this, but its the only way for it to work :(
		for (ItemStack lootItem : loot) {
			if (lootItem != null) {
				inventory.addItem(lootItem);
			}
		}
		lootMen.add(player);
		player.closeInventory(Reason.OPEN_NEW);
		player.openInventory(inventory);
		ItemStack emptyChest = new ItemStack(Material.CHEST);
		ItemMeta emptyChestMeta = emptyChest.getItemMeta();
		if (item2.hasItemMeta() && item2.getItemMeta().hasDisplayName()) {
			emptyChestMeta.setDisplayName(item2.getItemMeta().getDisplayName());
		}
		emptyChest.setItemMeta(emptyChestMeta);
		InventoryUtils.giveItem(player, emptyChest);
	}

	//Drop the items upon closing the inventory
	@EventHandler(priority = EventPriority.LOW)
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		//Only drop the items if a bunch of things that hopefully only should be true if you are in a loot chest inventory. Otherwise you should just remove yourself from the list.
		if (lootMen.contains(event.getPlayer()) && event.getInventory().getHolder() == null && event.getView().getTopInventory().getType().equals(InventoryType.CHEST)
				&& event.getView().getTopInventory().getSize() == 27) {
			ItemStack[] items = event.getView().getTopInventory().getContents();
			for (ItemStack item : items) {
				if (item != null) {
					Item droppedItem = event.getPlayer().getWorld().dropItem(event.getPlayer().getLocation(), item);
					droppedItem.setPickupDelay(0);
				}
			}
			lootMen.remove(event.getPlayer());
		} else if (lootMen.contains(event.getPlayer()) && !event.getReason().equals(InventoryCloseEvent.Reason.OPEN_NEW)) {
			lootMen.remove(event.getPlayer());
		}
	}

	//Failsafes
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
