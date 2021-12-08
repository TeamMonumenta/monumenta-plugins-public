package com.playmonumenta.plugins.listeners;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;

import de.tr7zw.nbtapi.NBTEntity;



public class DeathItemListener implements Listener {
	private static final String DEATH_SORT_TAG = "DeathSortTag";

	//Snapshot of the player's inventory at death
	private final Map<UUID, List<ItemStack>> mBeforeDeathItems = new HashMap<>();
	private final Map<UUID, BukkitRunnable> mTimers30Sec = new HashMap<>();
	private final Map<UUID, BukkitRunnable> mTimers15Min = new HashMap<>();
	private final Plugin mPlugin;

	public DeathItemListener(Plugin plugin) {
		mPlugin = plugin;
	}

	//Records player's items into a map that gives slot of a hashmap
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void recordPlayerDeathEvent(PlayerDeathEvent event) {
		if (
			//Player contains PEB tag
			event.getEntity().getScoreboardTags().contains(DEATH_SORT_TAG)
				//In case a problem happens and the peb option can be turned off
				&& event.getEntity().hasPermission("monumenta.deathitemsort")
				//Player is in an area where they drop their items
				&& !event.getKeepInventory()) {

			ItemStack[] contents = event.getEntity().getInventory().getContents();
			List<ItemStack> beforeDeathItems = Arrays.asList(contents);

			//Do not put in the death map of a player with no items
			for (ItemStack item : beforeDeathItems) {
				if (item != null && !item.getType().isAir() && item.hasItemMeta() && !item.getType().equals(Material.COMPASS)) {
					//Found at least one non-air non-compass item with meta
					//Save the player's death inventory
					//Update inventory includes the 15 minute timer that starts to run on death (synced up to hoped items despawn timer)
					mBeforeDeathItems.put(event.getEntity().getUniqueId(), beforeDeathItems);
					cancelTimers(event.getEntity().getUniqueId());
					//Clears the saving of the players inventory after 15 minutes
					runClearMapDelay(event.getEntity().getUniqueId(), 20 * 60 * 15, mTimers15Min);
					break;
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void checkItemEvent(EntityPickupItemEvent event) {
		if (!(event.getEntity() instanceof Player) || !event.getEntity().getScoreboardTags().contains(DEATH_SORT_TAG)) {
			return; // not a player, or they don't have death sort enabled
		}

		//Get the item picked up
		Player player = (Player) event.getEntity();
		PlayerInventory inventory = player.getInventory();
		ItemStack item = event.getItem().getItemStack();

		//Get the player's inventory before they died
		List<ItemStack> beforeDeathItems = mBeforeDeathItems.get(player.getUniqueId());
		if (beforeDeathItems == null) {
			return; //No data
		}

		//Combine item with the same item if it can be stacked into it (do nothing)
		for (int slot = 0; slot < beforeDeathItems.size(); slot++) {
			ItemStack invItem = inventory.getItem(slot);
			if (invItem != null && !invItem.getType().isAir() && invItem.getAmount() < invItem.getMaxStackSize() && invItem.isSimilar(item)) {
				//Item picked up will stack with one already in inventory - don't need to do anything
				return;
			}
		}

		//Attempt to place this in an empty inventory slot where it previously was
		for (int slot = 0; slot < beforeDeathItems.size(); slot++) {
			ItemStack originalItem = beforeDeathItems.get(slot);
			ItemStack invItem = inventory.getItem(slot);
			if (originalItem != null && (invItem == null || invItem.getType().isAir()) && originalItem.isSimilar(item)) {
				//Put the item back in this slot, unless it was an armor slot and the item was curse of binding
				if (slot < 36 || !item.containsEnchantment(Enchantment.BINDING_CURSE)) {
					if (ItemUtils.isArmor(item) && slot >= 36 && slot <= 39) {
						player.getWorld().playSound(player.getLocation(), ItemUtils.getArmorEquipSound(item.getType()), 0.75f, 1);
					}

					//Simulate item pickup
					inventory.setItem(slot, event.getItem().getItemStack());
					InventoryUtils.scheduleDelayedEquipmentSlotCheck(mPlugin, player, slot);
					simulatePickup(event.getItem(), player);
					event.setCancelled(true);

					//Delete the inventory slots data after 30 seconds once the player successfully picks up one item from their death pile
					runClearMapDelay(player.getUniqueId(), 20 * 30, mTimers30Sec);

					return; //Nothing left to do, this item is handled
				}
			}
		}

		//Attempt to place this in an empty inventory slot that was empty before death
		for (int slot = 0; slot < 36; slot++) {
			ItemStack originalItem = beforeDeathItems.get(slot);
			ItemStack invItem = inventory.getItem(slot);
			if ((originalItem == null || originalItem.getType().isAir()) && (invItem == null || invItem.getType().isAir())) {
				//Found an empty slot that was empty before death

				//Simulate item pickup
				inventory.setItem(slot, event.getItem().getItemStack());
				InventoryUtils.scheduleDelayedEquipmentSlotCheck(mPlugin, player, slot);
				simulatePickup(event.getItem(), player);
				event.setCancelled(true);

				return; //Nothing left to do, this item is handled
			}
		}

		//Failed to put it on an existing stack, or where it was before, or in an empty slot before death.
		//Just let normal item pickup do its thing
	}

	//Clears the saving of the players inventory after some delay
	private void runClearMapDelay(UUID id, int delay, Map<UUID, BukkitRunnable> trackingMap) {
		if (!trackingMap.containsKey(id)) {
			BukkitRunnable runnable = new BukkitRunnable() {
				@Override
				public void run() {
					mBeforeDeathItems.remove(id);
					cancelTimers(id);
				}
			};
			runnable.runTaskLater(mPlugin, delay);
			trackingMap.put(id, runnable);
		}
	}

	//Cancels and removes the map wipe timers
	private void cancelTimers(UUID id) {
		BukkitRunnable timer = mTimers15Min.remove(id);
		if (timer != null) {
			timer.cancel();
		}
		timer = mTimers30Sec.remove(id);
		if (timer != null) {
			timer.cancel();
		}
	}

	//Makes the item seem like it was picked up by the player animation wise, and then deletes it
	private static void simulatePickup(Item item, Player player) {

		//Prevent the item from being picked up normally
		item.setCanMobPickup(false);
		item.setCanPlayerPickup(false);

		//Make the item despawn in the next tick
		NBTEntity nbte = new NBTEntity(item);
		nbte.setShort("Age", (short) 11999);

		//Play pickup animation and sound
		player.playPickupItemAnimation(item);
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.25f, (float) FastUtils.randomDoubleInRange(1, 2));
	}

	public int pickupItem(Player player, Item entity) {
		PlayerInventory inventory = player.getInventory();
		ItemStack item = entity.getItemStack();
		if (player.getScoreboardTags().contains(DEATH_SORT_TAG) && mBeforeDeathItems.containsKey(player.getUniqueId())) {
			List<ItemStack> beforeDeathItems = mBeforeDeathItems.get(player.getUniqueId());

			//Combine item with the same item if it can be stacked into it
			for (int slot = 0; slot < beforeDeathItems.size(); slot++) {
				ItemStack invItem = inventory.getItem(slot);
				if (invItem != null && !invItem.getType().isAir() && invItem.getAmount() < invItem.getMaxStackSize() && invItem.isSimilar(item)) {
					if (invItem.getAmount() + item.getAmount() <= invItem.getMaxStackSize()) {
						// Full amount can be combined
						invItem.add(item.getAmount());
						simulatePickup(entity, player);
						return 0;
					} else {
						// Partial amount can be combined
						item.setAmount(invItem.getAmount() + item.getAmount() - invItem.getMaxStackSize());
						invItem.setAmount(invItem.getMaxStackSize());
						entity.setItemStack(item);
					}
				}
			}

			//Attempt to place this in an empty inventory slot where it previously was
			for (int slot = 0; slot < beforeDeathItems.size(); slot++) {
				ItemStack originalItem = beforeDeathItems.get(slot);
				ItemStack invItem = inventory.getItem(slot);
				if (originalItem != null && (invItem == null || invItem.getType().isAir()) && originalItem.isSimilar(item)) {
					//Put the item back in this slot, unless it was an armor slot and the item was curse of binding
					if (slot < 36 || (!item.containsEnchantment(Enchantment.BINDING_CURSE) && !ItemUtils.isShatteredWearable(item))) {
						if (ItemUtils.isArmor(item) && slot >= 36 && slot <= 39) {
							player.getWorld().playSound(player.getLocation(), ItemUtils.getArmorEquipSound(item.getType()), 0.75f, 1);
						}

						//Simulate item pickup
						inventory.setItem(slot, item);
						InventoryUtils.scheduleDelayedEquipmentSlotCheck(mPlugin, player, slot);
						simulatePickup(entity, player);

						//Delete the inventory slots data after 30 seconds once the player successfully picks up one item from their death pile
						runClearMapDelay(player.getUniqueId(), 20 * 30, mTimers30Sec);

						return 0; //Nothing left to do, this item is handled
					}
				}
			}

			//Attempt to place this in an empty inventory slot that was empty before death
			for (int slot = 0; slot < 36; slot++) {
				ItemStack originalItem = beforeDeathItems.get(slot);
				ItemStack invItem = inventory.getItem(slot);
				if ((originalItem == null || originalItem.getType().isAir()) && (invItem == null || invItem.getType().isAir())) {
					//Found an empty slot that was empty before death

					//Simulate item pickup
					inventory.setItem(slot, item);
					InventoryUtils.scheduleDelayedEquipmentSlotCheck(mPlugin, player, slot);
					simulatePickup(entity, player);

					return 0; //Nothing left to do, this item is handled
				}
			}
		}

		//Attempt to place this in any empty slot
		int slot = inventory.firstEmpty();
		if (slot != -1) {
			inventory.setItem(slot, item);
			simulatePickup(entity, player);
			return 0;
		}

		// Couldn't place the item, return how many are left
		return item.getAmount();
	}
}
