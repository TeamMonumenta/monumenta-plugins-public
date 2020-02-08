package com.playmonumenta.plugins.inventories;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;

/**
 * This class allows one Shulker Box to be accessed without being placed.
 */
public class ShulkerInventory {
	private final Plugin mPlugin;
	private final Player mPlayer;
	private final ItemStack mShulkerItem;
	private final Inventory mInventory;
	private final Inventory mParentInventory;
	private final int mDepositLimit;
	private final String mLock;
	private int mDepositCount;

	/**
	 * Create a new ShulkerInventory to allow players to access a Shulker Box without being placed.
	 *
	 * @param plugin The Plugin to manage scheduling tasks.
	 * @param player The Player who is opening this Shulker Box.
	 * @param parentInventory The inventory the Shulker Box item is in.
	 * @param shulkerItem The Shulker Box in the form of an ItemStack.
	 * @see #ShulkerInventory(Plugin, Player, Inventory, ItemStack, int)
	 */
	ShulkerInventory(Plugin plugin, Player player, Inventory parentInventory, ItemStack shulkerItem) {
		this(plugin, player, parentInventory, shulkerItem, 0);
	}

	/**
	 * Create a new ShulkerInventory to allow players to access a Shulker Box without being placed.
	 * If forDeposit is true, this Shulker Box will be marked as For Deposit Only. In this state,
	 * the Shulker Box inventory cannot be opened by a player. This is useful for depositing items
	 * into the shulker without opening it.
	 *
	 * @param plugin The Plugin to manage scheduling tasks.
	 * @param player The Player who is opening this Shulker Box.
	 * @param parentInventory The inventory the Shulker Box item is in.
	 * @param shulkerItem The Shulker Box in the form of an ItemStack.
	 * @param depositLimit The number of ItemStacks that will be deposited into this Shulker Box.
	 *                     Attempting to deposit more than this number will throw an error.
	 *                     The shulker will automatically be closed when this number of deposits is reached.
	 *                     If 0, the shulker is not being used for deposit, but to be opened by a player.
	 */
	ShulkerInventory(Plugin plugin, Player player, Inventory parentInventory, ItemStack shulkerItem, int depositLimit) {
		mPlugin = plugin;
		mPlayer = player;
		mParentInventory = parentInventory;
		mShulkerItem = shulkerItem;
		mDepositLimit = depositLimit;
		mLock = (mDepositLimit > 0 ? "ShulkerDeposit:" : "ShulkerShortcut:") + mPlayer.getUniqueId();
		BlockStateMeta shulkerMeta = (BlockStateMeta)mShulkerItem.getItemMeta();
		ShulkerBox shulkerBox = (ShulkerBox)shulkerMeta.getBlockState();
		if (shulkerBox.getCustomName() != null) {
			mInventory = Bukkit.createInventory(mPlayer, InventoryType.SHULKER_BOX, shulkerBox.getCustomName());
		} else {
			mInventory = Bukkit.createInventory(mPlayer, InventoryType.SHULKER_BOX);
		}
		mInventory.setContents(shulkerBox.getInventory().getContents());
	}

	/**
	 * Update the Shulker Box ItemStack in the parent inventory to match the current contents.
	 * This is done by looping through the entire parent inventory looking for the open Shulker Box.
	 * Note that the actual update does not occur until the following server tick. This is to account
	 * for InventoryClickEvents that don't actually update inventories until the end of the event.
	 *
	 * @return True if the Shulker Box still exists in the parent inventory.
	 * @see #updateShulker(boolean)
	 */
	boolean updateShulker() {
		return updateShulker(false);
	}

	/**
	 * Update the Shulker Box ItemStack in the parent inventory to match the current contents.
	 * This is done by looping through the entire parent inventory looking for the open Shulker Box.
	 * Note that the actual update does not occur until the following server tick. This is to account
	 * for InventoryClickEvents that don't actually update inventories until the end of the event.
	 *
	 * @param unlock True if the Shulker Box should be unlocked after this update.
	 * @return True if the Shulker Box still exists in the parent inventory.
	 */
	private boolean updateShulker(boolean unlock) {
		for (ItemStack item : mParentInventory) {
			if (item != null && ItemUtils.isShulkerBox(item.getType())) {
				BlockStateMeta shulkerMeta = (BlockStateMeta) item.getItemMeta();
				ShulkerBox shulkerBox = (ShulkerBox) shulkerMeta.getBlockState();
				if (shulkerBox.getLock().equals(mLock)) {
					new BukkitRunnable() {
						@Override
						public void run() {
							if (unlock) {
								shulkerBox.setLock(null);
							}
							shulkerBox.getInventory().setContents(mInventory.getContents());
							shulkerMeta.setBlockState(shulkerBox);
							mShulkerItem.setItemMeta(shulkerMeta);
							item.setItemMeta(shulkerMeta);
						}
					}.runTask(mPlugin);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Update the Shulker Box ItemStack in the parent inventory to match the current contents.
	 * This is done by looping through the entire parent inventory looking for the open Shulker Box.
	 * Note that the actual update does not occur until the following server tick. This is to account
	 * for InventoryClickEvents that don't actually update inventories until the end of the event.
	 * This does not close the player's inventory automatically.
	 *
	 * @return True if the Shulker Box still exists in the parent inventory.
	 * @see #updateShulker(boolean)
	 */
	boolean closeShulker() {
		return updateShulker(true);
	}

	/**
	 * Forces the player this Shulker Box belongs to to open the inventory.
	 * This does not force the player to close their current inventory.
	 * This will fail if the player has another inventory open already.
	 * If this Shulker Inventory is intended to be opened, it is recommended to
	 * close the player's current inventory before creating this object.
	 * This will refuse to work and log a warning if the constructor specified forDeposit = true
	 */
	void openShulker() {
		if (mDepositLimit > 0) {
			mPlugin.getLogger().warning("ShulkerInventory attempted to open a Shulker Box marked as For Deposit Only");
		} else {
			mPlayer.openInventory(mInventory);
		}
	}

	/**
	 * Attempt to deposit one ItemStack into the open Shulker Box.
	 * If the deposit limit has been reached, the Shulker Box will be closed after the item is deposited.
	 * If the limit is exceeded, an exception will be thrown.
	 *
	 * @param item The ItemStack to be deposited.
	 * @return The amount of the original stack that could not be deposited.
	 * @see #ShulkerInventory(Plugin, Player, Inventory, ItemStack, int)
	 */
	int depositItem(ItemStack item) throws Exception {
		if (isDepositComplete()) {
			throw new Exception("Deposit Limit Exceeded");
		}
		mDepositCount++;
		HashMap<Integer, ItemStack> remaining = mInventory.addItem(item);
		updateShulker(isDepositComplete());
		if (remaining.isEmpty()) {
			return 0;
		}
		return remaining.get(0).getAmount();
	}

	/**
	 * Get a list of users viewing the opened inventory.
	 * This list should never exceed a size of 1, and if it has a size of 0,
	 * the Shulker Box should be closed, as nobody has access to this inventory.
	 *
	 * @return a List of HumanEntity objects representing the current viewers.
	 */
	List<HumanEntity> getViewers() {
		return mInventory.getViewers();
	}

	public boolean isDepositComplete() {
		return mDepositCount >= mDepositLimit;
	}
}
