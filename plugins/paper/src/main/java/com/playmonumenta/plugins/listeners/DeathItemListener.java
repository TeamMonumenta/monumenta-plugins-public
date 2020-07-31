package com.playmonumenta.plugins.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Location;
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
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;

import de.tr7zw.nbtapi.NBTEntity;



public class DeathItemListener implements Listener {
	private static final String DEATH_SORT_TAG = "DeathSortTag";

	private final Map<UUID, Map<ItemStack, List<Integer>>> mInventories = new HashMap<>();
	private final Map<UUID, List<ItemStack>> mItemSlots = new HashMap<>();
	private Plugin mPlugin;

	public DeathItemListener(Plugin plugin) {
		mPlugin = plugin;
	}

	//Records player's items into a map that gives slot of a hashmap
	@EventHandler(priority = EventPriority.HIGHEST)
	public void recordPlayerDeathEvent(PlayerDeathEvent event) {
		if (!event.isCancelled()
				&& event.getEntity() != null
				//Player contains PEB tag
				&& event.getEntity().getScoreboardTags().contains(DEATH_SORT_TAG)
				//In case a problem happens and the peb option can be turned off
				&& event.getEntity().hasPermission("monumenta.deathitemsort")
				//Player has an inventory
				&& event.getEntity().getInventory() != null
				//Player is in an area where they drop their items
				&& !event.getKeepInventory()) {

			Map<ItemStack, List<Integer>> items = new HashMap<>();
			ItemStack[] contents = event.getEntity().getInventory().getContents();
			List<ItemStack> itemSlots = Arrays.asList(contents);

			//Put inventory in hashmap, prioritize armor and offhand first
			//For armor and offhand, it will not save curse of binding or curse of vanishing gear
			for (int i = 36; i < contents.length; i++) {
				if (contents[i] != null && contents[i].getType() != Material.AIR
						&& !contents[i].containsEnchantment(Enchantment.VANISHING_CURSE) && !contents[i].containsEnchantment(Enchantment.BINDING_CURSE)) {
					if (items.get(contents[i]) == null || items.get(contents[i]).isEmpty()) {
						items.put(contents[i], new ArrayList<Integer>());
					}
					items.get(contents[i]).add(i);
				}
			}
			//Rest of inventory saved, curse of vanishing gear isn't, as the item changes to shattered on death
			for (int i = 0; i < 36; i++) {
				if (contents[i] != null && contents[i].getType() != Material.AIR
						&& !contents[i].containsEnchantment(Enchantment.VANISHING_CURSE)) {
					if (items.get(contents[i]) == null || items.get(contents[i]).isEmpty()) {
						items.put(contents[i], new ArrayList<Integer>());
					}
					items.get(contents[i]).add(i);
				}
			}

			//Do not put in the death map of a player with no items
			if (items.size() > 0) {
				mInventories.put(event.getEntity().getUniqueId(), items);
				mItemSlots.put(event.getEntity().getUniqueId(), itemSlots);
			}
		}
	}

	//If item is in the hashmap specified, tries to set it to the slot it originally was in if not filled
	@EventHandler(priority = EventPriority.HIGHEST)
	public void checkItemEvent(EntityPickupItemEvent event) {
		if (!event.isCancelled() && event.getEntity() instanceof Player && event.getEntity().getScoreboardTags().contains(DEATH_SORT_TAG)) {

			//Get the item picked up
			Player player = (Player) event.getEntity();
			PlayerInventory inventory = player.getInventory();
			ItemStack item = event.getItem().getItemStack();

			Map<ItemStack, List<Integer>> itemToSlots = mInventories.get(player.getUniqueId());
			if (itemToSlots != null) {

				//Check if picked up item is in hashmap from death inventory locations
				if (itemToSlots.containsKey(item) && itemToSlots.get(item) != null && !itemToSlots.get(item).isEmpty()) {
					for (int slot : itemToSlots.get(item)) {
						//For armor equipments, do not place in armor slot if the item has curse of binding
						if (slot >= 36 && slot <= 39) {
							if (item.containsEnchantment(Enchantment.BINDING_CURSE)) {
								List<ItemStack> itemSlots = mItemSlots.get(player.getUniqueId());

								checkForOpenSlot(itemSlots, item, inventory, player, mPlugin, event);

								return;
							}
						}
						//If slot isn't filled, place the item there
						if (inventory.getItem(slot) == null || inventory.getItem(slot).getType() == Material.AIR) {

							//Set item and remove from array. If array is empty, remove it for this itemstack
							inventory.setItem(slot, item);
							itemToSlots.get(item).remove(Integer.valueOf(slot));
							if (itemToSlots.get(item).isEmpty()) {
								itemToSlots.remove(item);
							}

							//Play armor equip sound if it was an armor piece that was auto equipped
							if (ItemUtils.isArmorItem(item.getType()) && slot >= 36 && slot <= 39) {
								player.getWorld().playSound(player.getLocation(), ItemUtils.getArmorEquipSound(item.getType()), 0.75f, 1);
							}

							InventoryUtils.scheduleDelayedEquipmentSlotCheck(mPlugin, player, slot);
							simulatePickup(event.getItem(), player, mPlugin);
							event.setCancelled(true);

							//Remove Map after it has been exhausted
							if (itemToSlots == null || itemToSlots.isEmpty()) {
								mInventories.remove(player.getUniqueId());
								mItemSlots.remove(player.getUniqueId());
							}

							return;
						} else {
							//Remove from Map if filled, remove List too
							itemToSlots.get(item).remove(Integer.valueOf(slot));
							if (itemToSlots.get(item).isEmpty()) {
								itemToSlots.remove(item);
							}
						}
					}
				}

				//Place item in next open spot not filled from before death and current inventory
				//If neither works, run event as completely normal
				List<ItemStack> itemSlots = mItemSlots.get(player.getUniqueId());

				checkForOpenSlot(itemSlots, item, inventory, player, mPlugin, event);

				//Remove Map after it has been exhausted, remove List too
				if (itemToSlots == null || itemToSlots.isEmpty()) {
					mInventories.remove(player.getUniqueId());
					mItemSlots.remove(player.getUniqueId());
				}

			}
		}
	}

	//Goes through inventory, top to bottom, left to right
	//0-8 represents hotbar
	//Puts it in an unoccupied spot from before death, combines it with a similar one, or doesn't do anything
	private static void checkForOpenSlot(List<ItemStack> itemSlots, ItemStack item, PlayerInventory inventory, Player player, Plugin plugin, EntityPickupItemEvent event) {
		//Iterates through inventory
		for (int i = 0; i < 36; i++) {
			//Set item at next available spot in both the inventory currently and before death
			if ((itemSlots.get(i) == null || itemSlots.get(i).getType() == Material.AIR)
					&& (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR)) {
				inventory.setItem(i, event.getItem().getItemStack());

				InventoryUtils.scheduleDelayedEquipmentSlotCheck(plugin, player, i);
				simulatePickup(event.getItem(), player, plugin);
				event.setCancelled(true);

				break;
			//Combine item with the same item if it can be stacked into it (do nothing)
			} else if (inventory.getItem(i) != null && inventory.getItem(i).getType() != Material.AIR && inventory.getItem(i).getAmount() < inventory.getItem(i).getMaxStackSize() && inventory.getItem(i).isSimilar(item)) {
				break;
			//If there was an itemstack before of the same item, set/add it there.
			} else if (itemSlots.get(i) != null && itemSlots.get(i).getAmount() < itemSlots.get(i).getMaxStackSize() && itemSlots.get(i).isSimilar(item)) {
				//If slot is empty, place it there, otherwise, do nothing, they will combine normally through the event
				if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
					inventory.setItem(i, event.getItem().getItemStack());
				} else {
					break;
				}
				InventoryUtils.scheduleDelayedEquipmentSlotCheck(plugin, player, i);
				simulatePickup(event.getItem(), player, plugin);
				event.setCancelled(true);

				break;
			}
		}
	}

	//Makes the item seem like it was picked up by the player animation wise, and then deletes it
	private static void simulatePickup(Item item, Player player, Plugin plugin) {

		NBTEntity nbte = new NBTEntity(item);
		nbte.setShort("PickupDelay", new Short((short)32767)); //Makes item unpickupable
		nbte.setShort("Age", new Short((short)5990)); //Makes item despawn soon if this code misses removing it somehow

		//Deletes item after 2 ticks
		new BukkitRunnable() {
			@Override
			public void run() {
				item.remove();
			}
		}.runTaskLater(plugin, 2);

		//Set velocity of item towards player to simulate pickup animation
		Location itemLoc = item.getLocation();
		Location playerLoc = player.getLocation();
		Vector dir = new Vector(playerLoc.getX() - itemLoc.getX(), playerLoc.getY() - itemLoc.getY() + 0.5, playerLoc.getZ() - itemLoc.getZ());
		dir = dir.normalize().multiply(0.5);
		item.setVelocity(dir);

		//Play pickup sound with random pitch
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.25f, (float) FastUtils.randomDoubleInRange(1, 2));
	}
}
