package com.playmonumenta.plugins.inventories;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.HumanEntity;
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

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ChestUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import net.md_5.bungee.api.ChatColor;

public class LootChestsInInventory implements Listener {
	private final Map<UUID, Integer> mLootMenu = new HashMap<>();

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
		tag.setString("id", "minecraft:chest");
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
		if (item.isSimilar(event.getCursor())) {
			return;
		}
		if (!event.getCursor().getType().equals(Material.AIR)) {
			player.sendMessage(ChatColor.DARK_RED + "You must have an empty cursor to open loot chests!");
			event.setCancelled(true);
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			return;
		}
		//Make an inventory and do some good ol roundabout population of the loot
		Inventory inventory = Bukkit.createInventory(null, 27, item.getItemMeta().displayName());
		Builder builder = new LootContext.Builder(player.getLocation());
		Collection<ItemStack> loot = table.populateLoot(FastUtils.RANDOM, builder.build());
		item.subtract();
		//I hate this, but its the only way for it to work :(
		ChestUtils.generateLootInventory(loot, inventory, player);

		addOrInitializePlayer(player);
		player.closeInventory(Reason.OPEN_NEW);
		player.openInventory(inventory);
		ItemStack emptyChest = new ItemStack(Material.CHEST);
		ItemMeta emptyChestMeta = emptyChest.getItemMeta();
		if (item2.hasItemMeta() && item2.getItemMeta().hasDisplayName()) {
			emptyChestMeta.displayName(item2.getItemMeta().displayName());
		}
		emptyChest.setItemMeta(emptyChestMeta);
		ItemUtils.setPlainTag(emptyChest);
		InventoryUtils.giveItem(player, emptyChest);
	}

	//Drop the items upon closing the inventory
	@EventHandler(priority = EventPriority.LOW)
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		if (event.getInventory().getHolder() == null && event.getView().getTopInventory().getType().equals(InventoryType.CHEST) && event.getView().getTopInventory().getSize() == 27) {
			/* Right type of inventory - check if the player is in the map */
			HumanEntity player = event.getPlayer();

			/* Check if the player had a loot table chest open, and if so, decrement the count by 1. If it decrements to 0, remove from the map */
			boolean hadLootInventoryOpen = decrementOrClearPlayer(player);
			if (hadLootInventoryOpen) {
				/* Player did have a virtual loot inventory open - drop everything from it */
				ItemStack[] items = event.getView().getTopInventory().getContents();
				for (ItemStack item : items) {
					if (item != null && !item.getType().isAir()) {
						Item droppedItem = player.getWorld().dropItem(player.getLocation(), item);
						droppedItem.setPickupDelay(0);
						droppedItem.setOwner(player.getUniqueId());
						droppedItem.setThrower(player.getUniqueId());

						// Allow other players to pick this up after 10s
						Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
							if (droppedItem.isValid() && !droppedItem.isDead()) {
								droppedItem.setOwner(null);
							}
						}, 200);
					}
				}
				/* Make sure the source container is cleared, since it won't be reachable anymore anyway */
				event.getView().getTopInventory().clear();
			}
		}
	}

	//Failsafes
	@EventHandler(priority = EventPriority.LOW)
	public void playerJoinEvent(PlayerJoinEvent event) {
		mLootMenu.remove(event.getPlayer().getUniqueId());
	}

	@EventHandler(priority = EventPriority.LOW)
	public void playerQuitEvent(PlayerQuitEvent event) {
		mLootMenu.remove(event.getPlayer().getUniqueId());
	}

	private void addOrInitializePlayer(HumanEntity player) {
		Integer value = mLootMenu.get(player.getUniqueId());
		if (value == null) {
			value = 1;
		} else {
			value += 1;
		}
		mLootMenu.put(player.getUniqueId(), value);
	}

	/* Returns whether or not the player was in the map to begin with */
	private boolean decrementOrClearPlayer(HumanEntity player) {
		Integer value = mLootMenu.get(player.getUniqueId());
		if (value == null) {
			return false;
		} else {
			if (value > 1) {
				value--;
				mLootMenu.put(player.getUniqueId(), value);
			} else {
				mLootMenu.remove(player.getUniqueId());
			}
			return true;
		}
	}
}
