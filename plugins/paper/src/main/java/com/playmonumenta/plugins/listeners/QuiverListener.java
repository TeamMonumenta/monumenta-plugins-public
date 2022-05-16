package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.inventories.ShulkerInventoryManager;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import io.papermc.paper.event.entity.EntityLoadCrossbowEvent;
import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;

/**
 * Handles quivers, which are shulker boxes for arrows.
 * Arrows are taken from them to shoot, and arrows being picked up are put in there before the inventory.
 */
public class QuiverListener implements Listener {

	// Refill if the used arrow stack has less than this many arrows left.
	private static final int REFILL_LOWER_THAN = 16;

	// Refill arrows up to this amount. This is less than max stack size to prevent using an infinity crossbow (or multiple in a row) starting a new stack.
	private static final int REFILL_UP_TO = 48;

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityShootBowEvent(EntityShootBowEvent event) {
		if (event.getEntity() instanceof Player player) {
			refillInventoryDelayed(player);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityLoadCrossbowEvent(EntityLoadCrossbowEvent event) {
		if (event.getEntity() instanceof Player player) {
			refillInventoryDelayed(player);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void playerInteractEvent(PlayerInteractEvent event) {
		// When right-clicking with a bow or crossbow while having no arrows in the inventory, take some out of a quiver is available
		Player player = event.getPlayer();
		if ((ItemUtils.isSomeBow(player.getInventory().getItemInMainHand()) || ItemUtils.isSomeBow(player.getInventory().getItemInOffHand()))
			    && Arrays.stream(player.getInventory().getContents()).noneMatch(ItemUtils::isArrow)) {
			refillInventoryImmediately(player);
		}
	}

	// Refill delayed to execute after the event for bow shot/crossbow load (top execute after the arrow has been used)
	private void refillInventoryDelayed(Player player) {
		if (player.getGameMode() == GameMode.CREATIVE) {
			return;
		}
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			refillInventoryImmediately(player);
		});
	}

	// Refill immediately if the event allows
	private void refillInventoryImmediately(Player player) {
		if (player.getGameMode() == GameMode.CREATIVE) {
			return;
		}
		PlayerInventory inventory = player.getInventory();
		for (ItemStack arrow : inventory) {
			// Look for the first arrow stack in the player's inventory
			if (!ItemUtils.isArrow(arrow)) {
				continue;
			}
			// If that stack still has enough arrows, stop
			if (arrow.getAmount() >= REFILL_LOWER_THAN) {
				return;
			}
			// Search for quivers in the inventory and use them to restock that stack
			for (ItemStack quiver : inventory) {
				if (!ItemStatUtils.isQuiver(quiver)
					    || !(quiver.getItemMeta() instanceof BlockStateMeta blockStateMeta)
					    || !(blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox)) {
					continue;
				}
				boolean modified = false;
				// Move matching arrows until the stack to refill is almost full
				for (ItemStack quiverArrow : shulkerBox.getInventory()) {
					if (arrow.isSimilar(quiverArrow)) {
						int transferred = Math.min(REFILL_UP_TO - arrow.getAmount(), quiverArrow.getAmount());
						quiverArrow.subtract(transferred);
						arrow.add(transferred);
						modified = true;
						if (arrow.getAmount() >= REFILL_UP_TO) {
							break;
						}
					}
				}
				if (modified) {
					blockStateMeta.setBlockState(shulkerBox);
					quiver.setItemMeta(blockStateMeta);
				}
				if (arrow.getAmount() >= REFILL_UP_TO) {
					return;
				}
			}
			// no quiver found, or not enough arrows for a full stack - stop here.
			return;
		}

		// No arrows found in the inventory - the last arrow of its type was used up.
		// Search for a quiver and take out the first stack of arrows.
		if (!InventoryUtils.isFull(inventory)) {
			for (ItemStack quiver : inventory) {
				if (!ItemStatUtils.isQuiver(quiver)
					    || !(quiver.getItemMeta() instanceof BlockStateMeta blockStateMeta)
					    || !(blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox)) {
					continue;
				}
				for (ItemStack arrow : shulkerBox.getInventory()) {
					if (!ItemUtils.isArrow(arrow)) {
						continue;
					}
					ItemStack moved = arrow.clone();
					moved.setAmount(Math.min(arrow.getAmount(), REFILL_UP_TO));
					inventory.addItem(moved);
					arrow.subtract(moved.getAmount());
					blockStateMeta.setBlockState(shulkerBox);
					quiver.setItemMeta(blockStateMeta);
					return; // can directly return from here
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerAttemptPickupItemEvent(PlayerAttemptPickupItemEvent event) {
		handlePickupEvent(event, event.getItem(), event.getPlayer());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerPickupArrowEvent(PlayerPickupArrowEvent event) {
		handlePickupEvent(event, event.getItem(), event.getPlayer());
	}

	private void handlePickupEvent(Cancellable event, Item item, Player player) {
		if (player.getGameMode() == GameMode.CREATIVE || !item.isValid()) {
			return;
		}
		// If an arrow is picked up, put it into a quiver if space is available
		ItemStack itemStack = item.getItemStack();
		if (!ItemUtils.isArrow(itemStack)) {
			return;
		}
		for (ItemStack quiver : player.getInventory()) {
			if (!ItemStatUtils.isQuiver(quiver)
				    || !(quiver.getItemMeta() instanceof BlockStateMeta blockStateMeta)
				    || !(blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox)
				    || ShulkerInventoryManager.isShulkerInUse(quiver)) {
				continue;
			}
			int oldAmount = itemStack.getAmount();
			InventoryUtils.insertItemIntoLimitedInventory(shulkerBox.getInventory(), ItemStatUtils.getShulkerSlots(quiver), itemStack);
			if (oldAmount != itemStack.getAmount()) {
				blockStateMeta.setBlockState(shulkerBox);
				quiver.setItemMeta(blockStateMeta);
				if (itemStack.getAmount() == 0) {
					event.setCancelled(true);
					player.playPickupItemAnimation(item);
					item.remove();
					return;
				} else {
					item.setItemStack(itemStack);
				}
			}
		}
	}

}
