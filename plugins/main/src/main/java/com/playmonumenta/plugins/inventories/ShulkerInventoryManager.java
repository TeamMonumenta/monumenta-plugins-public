package com.playmonumenta.plugins.inventories;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.util.HashMap;
import java.util.UUID;

/**
 * This class allows Shulker Boxes to be opened directly from inventories,
 * without needing to place them on the ground first.
 *
 * @see com.playmonumenta.plugins.inventories.ShulkerInventory
 */
public class ShulkerInventoryManager {
	private final static String ERROR_SHULKER_LOCKED = String.format("%s%sThis shulker is locked", ChatColor.DARK_RED, ChatColor.BOLD);
	private final static String ERROR_SHULKER_ALREADY_OPEN = String.format("%s%sThis shulker is already open", ChatColor.DARK_RED, ChatColor.BOLD);
	private final Plugin mPlugin;
	private final HashMap<UUID, ShulkerInventory> mInventories = new HashMap<>();

	public ShulkerInventoryManager(Plugin plugin) {
		mPlugin = plugin;
	}

	/**
	 * Opens a shulker box directly from an inventory and gives the player access.
	 *
	 * @param player The Player who is trying to open the Shulker Box.
	 * @param parentInventory The inventory the Shulker Box item is in.
	 * @param shulkerItem The Shulker Box in the form of an ItemStack.
	 * @return True if the Shulker Box was successfully opened.
	 */
	public boolean openShulker(Player player, Inventory parentInventory, ItemStack shulkerItem) {
		if (shulkerItem != null && ItemUtils.isShulkerBox(shulkerItem.getType())) {
			// Get metadata from shulker box. If it doesn't have metadata, this will generate blank data.
			BlockStateMeta shulkerMeta = (BlockStateMeta)shulkerItem.getItemMeta();
			ShulkerBox shulkerBox = (ShulkerBox)shulkerMeta.getBlockState();
			// If any of the metadata was missing and needed to be generated, update the item with the generated data.
			if (!shulkerItem.hasItemMeta() || !shulkerMeta.hasBlockState()) {
				shulkerMeta.setBlockState(shulkerBox);
				shulkerItem.setItemMeta(shulkerMeta);
			}
			// Check if the shulker box can be opened
			if (shulkerBox.isLocked()) {
				String lock = shulkerBox.getLock();
				if (lock.startsWith("ShulkerShortcut:")) {
					UUID lockUUID = UUID.fromString(lock.substring(16));
					if (mInventories.containsKey(lockUUID)) {
						if (mInventories.get(lockUUID).getViewers().size() != 0) {
							// Someone has this shulker open already
							player.sendMessage(ERROR_SHULKER_ALREADY_OPEN);
							return false;
						}
						// This shulker is stuck in an open state but no players have access
						closeShulker(lockUUID);
						shulkerBox.setLock(null);
					}
				} else {
					player.sendMessage(ERROR_SHULKER_LOCKED);
					return false;
				}
			}
			player.closeInventory(InventoryCloseEvent.Reason.OPEN_NEW);
			shulkerBox.setLock("ShulkerShortcut:"+player.getUniqueId());
			shulkerMeta.setBlockState(shulkerBox);
			shulkerItem.setItemMeta(shulkerMeta);
			ShulkerInventory shulkerInventory = new ShulkerInventory(mPlugin, player, parentInventory, shulkerItem);
			mInventories.put(player.getUniqueId(), shulkerInventory);
			shulkerInventory.openShulker();
			return true;
		}
		return false;
	}

	/**
	 * Temporarily opens a Shulker Box without giving access to a player.
	 * This open shulker will have one ItemStack inserted, then be closed.
	 *
	 * @param player Player who is depositing the item.
	 * @param parentInventory The inventory the shulker is in.
	 * @param shulkerItem The Shulker Box in the form of an ItemStack.
	 * @param item The item(s) to be inserted in the form of an ItemStack.
	 * @return The amount of items from the stack that were not able to fit in the Shulker Box.
	 */
	public int addItemToShulker(Player player, Inventory parentInventory, ItemStack shulkerItem, ItemStack item) {
		if (shulkerItem != null && ItemUtils.isShulkerBox(shulkerItem.getType())) {
			// Get metadata from shulker box. If it doesn't have metadata, this will generate blank data.
			BlockStateMeta shulkerMeta = (BlockStateMeta) shulkerItem.getItemMeta();
			ShulkerBox shulkerBox = (ShulkerBox) shulkerMeta.getBlockState();
			// If any of the metadata was missing and needed to be generated, update the item with the generated data.
			if (!shulkerItem.hasItemMeta() || !shulkerMeta.hasBlockState()) {
				shulkerMeta.setBlockState(shulkerBox);
				shulkerItem.setItemMeta(shulkerMeta);
			}
			if (shulkerBox.isLocked()) {
				String lock = shulkerBox.getLock();
				if (lock.startsWith("ShulkerShortcut:")) {
					UUID lockUUID = UUID.fromString(lock.substring(16));
					if (mInventories.containsKey(lockUUID)) {
						if (mInventories.get(lockUUID).getViewers().size() != 0) {
							// Someone has this shulker open already
							player.sendMessage(ERROR_SHULKER_ALREADY_OPEN);
							return -1;
						}
						// This shulker is stuck in an open state but no players have access
						closeShulker(lockUUID);
						shulkerBox.setLock(null);
					}
				} else {
					player.sendMessage(ERROR_SHULKER_LOCKED);
					player.sendMessage("[DEBUG] Shulker Lock: "+lock);
					return -2;
				}
			}
			shulkerBox.setLock("ShulkerDeposit:"+player.getUniqueId());
			shulkerMeta.setBlockState(shulkerBox);
			shulkerItem.setItemMeta(shulkerMeta);
			ShulkerInventory shulker = new ShulkerInventory(mPlugin, player, parentInventory, shulkerItem, true);
			return shulker.depositItem(item);
		}
		return -3;
	}

	/**
	 * Update the Shulker Box currently open by a player.
	 * This does not force the player to close their inventory if the Shulker Box is invalid.
	 *
	 * @param player Player whose Shulker Box should be updated.
	 * @return True if the player has a valid Shulker open.
	 */
	public boolean updateShulker(Player player) {
		if (mInventories.containsKey(player.getUniqueId())) {
			ShulkerInventory inv = mInventories.get(player.getUniqueId());
			if (inv.getViewers().size() == 0) {
				// Shulker is no longer actually open, close it
				return closeShulker(player);
			}
			return inv.updateShulker();
		}
		return false;
	}

	/**
	 * Closes a Shulker Box opened via a player.
	 * This does not force the player to close their inventory.
	 *
	 * @param player Player whose Shulker Box should be closed.
	 * @return True if the player had a valid open Shulker Box.
	 */
	public boolean closeShulker(Player player) {
		return closeShulker(player.getUniqueId());
	}

	/**
	 * Closes a Shulker Box opened via a player.
	 * This does not force the player to close their inventory.
	 *
	 * @param uuid UUID of the player whose Shulker Box should be closed.
	 * @return True if the player had a valid open Shulker Box
	 */
	private boolean closeShulker(UUID uuid) {
		if (mInventories.containsKey(uuid)) {
			ShulkerInventory inv = mInventories.remove(uuid);
			return inv.closeShulker();
		}
		return false;
	}

	/**
	 * Check if a shulker box is currently opened via shortcut,
	 * as well as if the shulker is currently being used by a player.
	 * If it is open but not being used, the lock is released automatically.
	 *
	 * @param shulkerItem Shulker Box in ItemStack form.
	 * @return True if shulker is open and in use.
	 */
	public boolean isShulkerInUse(ItemStack shulkerItem) {
		if (shulkerItem != null && ItemUtils.isShulkerBox(shulkerItem.getType())) {
			BlockStateMeta shulkerMeta = (BlockStateMeta) shulkerItem.getItemMeta();
			ShulkerBox shulkerBox = (ShulkerBox) shulkerMeta.getBlockState();
			if (shulkerBox.getLock().startsWith("ShulkerShortcut:")) {
				// Shulker Box was opened via shortcut
				UUID lockUUID = UUID.fromString(shulkerBox.getLock().substring(16));
				if (mInventories.containsKey(lockUUID)) {
					if (mInventories.get(lockUUID).getViewers().size() > 0) {
						// Someone is currently using the shulker.
						return true;
					}
					// The shulker was opened via shortcut but nobody is using it now. Release the lock.
					shulkerBox.setLock(null);
					shulkerMeta.setBlockState(shulkerBox);
					shulkerItem.setItemMeta(shulkerMeta);
					closeShulker(lockUUID);
				}
			}
		}
		// Item is either not a shulker or is not being used.
		return false;
	}

	/**
	 * Check if a player is currently using a shulker box via shortcut.
	 *
	 * @param player The player to be tested
	 * @return True if the player has a shulker open.
	 */
	public boolean playerHasShulkerOpen(Player player) {
		return mInventories.containsKey(player.getUniqueId());
	}
}
