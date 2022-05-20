package com.playmonumenta.plugins.inventories;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * This class allows one Shulker Box to be accessed without being placed.
 */
@SuppressWarnings("checkstyle:EmptyLineSeparator")
public class ShulkerInventory {
	private final Plugin mPlugin;
	private final Player mPlayer;
	private final ItemStack mShulkerItem;
	private final BlockStateMeta mShulkerMeta;
	private final ShulkerBox mShulkerState;
	private final Inventory mInventory;
	private final Inventory mParentInventory;
	private final int mParentSlot;
	private final int mDepositLimit;
	private final int mSlots;
	private int mDepositCount;

	public static final ItemStack FILLER = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);

	static {
		ItemMeta meta = FILLER.getItemMeta();
		meta.displayName(Component.empty());
		FILLER.setItemMeta(meta);
	}

	/**
	 * Create a new ShulkerInventory to allow players to access a Shulker Box without being placed.
	 *
	 * @param plugin          The Plugin to manage scheduling tasks.
	 * @param player          The Player who is opening this Shulker Box.
	 * @param parentInventory The inventory the Shulker Box item is in.
	 * @param parentSlot      The slot the Shulker Box is in.
	 * @see #ShulkerInventory(Plugin, Player, Inventory, int, int)
	 */
	ShulkerInventory(Plugin plugin, Player player, Inventory parentInventory, int parentSlot) throws Exception {
		this(plugin, player, parentInventory, parentSlot, 0);
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
	 * @param parentSlot The slot the Shulker Box is in.
	 * @param depositLimit The number of ItemStacks that will be deposited into this Shulker Box.
	 *                     Attempting to deposit more than this number will throw an error.
	 *                     The shulker will automatically be closed when this number of deposits is reached.
	 *                     If 0, the shulker is not being used for deposit, but to be opened by a player.
	 */
	ShulkerInventory(Plugin plugin, Player player, Inventory parentInventory, int parentSlot, int depositLimit) throws Exception {
		mPlugin = plugin;
		mPlayer = player;
		mParentInventory = parentInventory;
		mParentSlot = parentSlot;
		mShulkerItem = mParentInventory.getItem(mParentSlot);
		if (mShulkerItem == null || !ItemUtils.isShulkerBox(mShulkerItem.getType())) {
			throw new Exception("not a shulker box");
		}
		mDepositLimit = depositLimit;
		mShulkerMeta = (BlockStateMeta) mShulkerItem.getItemMeta();
		mShulkerState = (ShulkerBox) mShulkerMeta.getBlockState();
		mSlots = ItemStatUtils.getShulkerSlots(mShulkerItem);
		int size = Math.max(9, Math.min(((mSlots + 8) / 9) * 9, 27));
		Component name = mShulkerState.customName();
		if (name == null) {
			name = ItemUtils.getDisplayName(mShulkerItem);
		}
		if (size == 27) {
			mInventory = Bukkit.createInventory(mPlayer, InventoryType.SHULKER_BOX, name);
		} else {
			mInventory = Bukkit.createInventory(mPlayer, size, name);
		}
		if (mSlots == 27) {
			mInventory.setContents(mShulkerState.getInventory().getContents());
		} else {
			ItemStack[] contents = mShulkerState.getInventory().getContents();
			contents = Arrays.copyOfRange(contents, 0, size);
			for (int i = mSlots; i < size; i++) { // put filler items in blocked slots
				contents[i] = FILLER.clone();
			}
			mInventory.setContents(contents);
		}
	}

	/**
	 * Update the Shulker Box ItemStack in the parent inventory to match the current contents.
	 * This is done by looping through the entire parent inventory looking for the open Shulker Box.
	 * Note that the actual update does not occur until the following server tick. This is to account
	 * for InventoryClickEvents that don't actually update inventories until the end of the event.
	 *
	 * @return True if the Shulker Box still exists in the parent inventory.
	 * @see #updateShulker(boolean, boolean)
	 */
	boolean updateShulker() {
		return updateShulker(false, false);
	}

	/**
	 * Update the Shulker Box ItemStack in the parent inventory to match the current contents.
	 * This is done by looping through the entire parent inventory looking for the open Shulker Box.
	 * Note that the actual update does not occur until the following server tick. This is to account
	 * for InventoryClickEvents that don't actually update inventories until the end of the event.
	 *
	 * @param unlock True if the Shulker Box should be unlocked after this update.
	 * @param instant True if the Shulker Box should be saved instantly. For
	 * @return True if the Shulker Box still exists in the parent inventory.
	 */
	private boolean updateShulker(boolean unlock, boolean instant) {
		if (isShulkerValid()) {
			if (instant) {
				return updateShulkerInstant(unlock);
			} else {
				new BukkitRunnable() {
					@Override
					public void run() {
						updateShulkerInstant(unlock);
					}
				}.runTask(mPlugin);
				return true;
			}
		}
		return false;
	}

	private boolean updateShulkerInstant(boolean unlock) {
		if (isShulkerValid()) {
			if (unlock) {
				mShulkerState.setLock(null);
			}
			if (mSlots == 27) {
				mShulkerState.getInventory().setContents(mInventory.getContents());
			} else {
				for (int i = 0; i < mSlots; i++) {
					mShulkerState.getInventory().setItem(i, mInventory.getItem(i));
				}
			}
			mShulkerMeta.setBlockState(mShulkerState);
			mShulkerItem.setItemMeta(mShulkerMeta);
			mParentInventory.setItem(mParentSlot, mShulkerItem);
			return true;
		}
		return false;
	}

	private boolean isShulkerValid() {
		InventoryHolder holder = mParentInventory.getHolder();
		if (holder instanceof Entity && !((Entity) holder).isValid()) {
			return false;
		}
		return mShulkerItem.equals(mParentInventory.getItem(mParentSlot));
	}

	/**
	 * Update the Shulker Box ItemStack in the parent inventory to match the current contents.
	 * This is done by looping through the entire parent inventory looking for the open Shulker Box.
	 * Note that the actual update does not occur until the following server tick. This is to account
	 * for InventoryClickEvents that don't actually update inventories until the end of the event.
	 * This does not close the player's inventory automatically.
	 *
	 * @param instant If the Shulker Box should be saved on the same tick. Only used if the parent inventory will not
	 *                exist on the next tick. For example: When a player is logging out.
	 * @return True if the Shulker Box still exists in the parent inventory.
	 * @see #updateShulker(boolean, boolean)
	 */
	boolean closeShulker(boolean instant) {
		return updateShulker(true, instant);
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
	 * @see #ShulkerInventory(Plugin, Player, Inventory, int, int)
	 */
	int depositItem(ItemStack item) throws Exception {
		if (isDepositComplete()) {
			throw new Exception("Deposit Limit Exceeded");
		}
		mDepositCount++;
		HashMap<Integer, ItemStack> remaining = mInventory.addItem(item);
		updateShulker(isDepositComplete(), false);
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

	public Inventory getInventory() {
		return mInventory;
	}

	public int getSlots() {
		return mSlots;
	}

	public ItemStack getShulkerItem() {
		return mShulkerItem;
	}
}
