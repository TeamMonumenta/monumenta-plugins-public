package com.playmonumenta.plugins.guis.lib;

import com.google.common.base.Preconditions;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An abstract base class representing a customizable GUI inventory for a player.
 * <p>
 * Subclasses must implement rendering logic and inventory interaction handling.
 * This class manages inventory creation, updates, and basic state management.
 * </p>
 */
public abstract class Gui implements InventoryHolder {
	/**
	 * The player interacting with this GUI instance.
	 */
	protected final Player mPlayer;

	/**
	 * Flag indicating whether the GUI needs to be re-rendered.
	 */
	private boolean mIsDirty = true;

	/**
	 * ItemStack used to fill empty slots in the inventory.
	 */
	private final ItemStack mFiller;

	/**
	 * List of GuiItems in the inventory, with null entries representing filler items.
	 */
	final List<@Nullable GuiItem> mItems = new ArrayList<>();

	/**
	 * The Bukkit inventory instance managed by this GUI.
	 */
	@Nullable
	private Inventory mInventory;

	/**
	 * The current title displayed in the inventory window.
	 */
	private Component mTitle;

	/**
	 * Flag indicating whether the GUI is currently being rendered.
	 */
	private boolean mIsRendering;

	/**
	 * Constructs a new GUI instance.
	 *
	 * @param player The player who will interact with this GUI
	 * @param filler The item used to fill empty inventory slots
	 * @param title  The initial title of the inventory
	 * @param size   The initial size of the inventory (must be multiple of 9)
	 */
	protected Gui(Player player, ItemStack filler, Component title, int size) {
		Preconditions.checkState(Bukkit.isPrimaryThread(), "off-main gui creation is not allowed");
		mFiller = filler;
		mPlayer = player;
		recreateInventory(size, title);
	}

	/**
	 * Constructs a new GUI instance.
	 *
	 * @param player The player who will interact with this GUI
	 * @param filler The item used to fill empty inventory slots
	 * @param title  The initial title of the inventory
	 * @param size   The initial size of the inventory (must be multiple of 9)
	 */
	protected Gui(Player player, ItemStack filler, String title, int size) {
		this(player, filler, MessagingUtils.MINIMESSAGE_ALL.deserialize(title), size);
	}

	/**
	 * Recreates the inventory with specified size and title.
	 * <p>
	 * Resets all items and fills the inventory with the filler item.
	 * </p>
	 *
	 * @param size  The new size of the inventory
	 * @param title The new title for the inventory
	 */
	private void recreateInventory(int size, Component title) {
		mTitle = title;
		mItems.clear();
		mInventory = Bukkit.createInventory(this, size, title);

		for (int i = 0; i < size; i++) {
			mItems.add(null);
			mInventory.setItem(i, mFiller);
		}
	}

	/**
	 * Updates the GUI if marked as dirty.
	 * <p>
	 * Recreates the inventory if size or title changed, triggers rendering,
	 * and opens the updated inventory for the player if necessary.
	 * </p>
	 */
	void update() {
		if (mInventory == null || !mIsDirty) {
			return;
		}

		final var size = getSize();
		final var title = getTitle();

		boolean recreateInventory = size != mItems.size() || !Objects.equals(mTitle, title);

		if (recreateInventory) {
			recreateInventory(size, title);
		}

		for (int i = 0; i < mInventory.getSize(); i++) {
			mInventory.setItem(i, mFiller.clone());
			mItems.set(i, null);
		}

		mIsRendering = true;

		// RAII guard
		try {
			render();
		} finally {
			mIsRendering = false;
		}

		if (recreateInventory) {
			// it's never null here...
			mPlayer.openInventory(Objects.requireNonNull(mInventory));
		} else {
			mPlayer.updateInventory();
		}

		mIsDirty = false;
	}

	/**
	 * Sets an item in the specified slot during rendering.
	 *
	 * @param i    The slot index to update
	 * @param item The GUI item to place in the slot
	 * @throws IllegalStateException If called after inventory disposal or outside render()
	 */
	public final void setItem(int i, GuiItem item) {
		Preconditions.checkState(Bukkit.isPrimaryThread(), "off-main gui operation is not allowed");
		Preconditions.checkState(mIsRendering, "setItem() called outside of render()");
		Preconditions.checkState(mInventory != null, "setItem called after inventory was disposed");
		mInventory.setItem(i, item.getItem());
		mItems.set(i, item);
	}

	public final void setItem(int row, int col, GuiItem item) {
		setItem(row * 9 + col, item);
	}

	/**
	 * Marks the GUI as needing re-rendering.
	 * <p>
	 * The update will be performed on the next call to {@link #update()}.
	 * </p>
	 */
	public final void markDirty() {
		Preconditions.checkState(Bukkit.isPrimaryThread(), "off-main gui operation is not allowed");
		this.mIsDirty = true;
	}

	/**
	 * Opens the GUI for the player.
	 *
	 * @throws IllegalStateException If called after inventory disposal
	 */
	public void open() {
		Preconditions.checkState(Bukkit.isPrimaryThread(), "off-main gui operation is not allowed");
		if (mInventory == null) {
			return;
		}

		update();
		mPlayer.openInventory(mInventory);
	}

	/**
	 * Closes the GUI and disposes the inventory.
	 *
	 * @throws IllegalStateException If called after inventory disposal
	 */
	public void close() {
		Preconditions.checkState(Bukkit.isPrimaryThread(), "off-main gui operation is not allowed");
		if (mInventory == null) {
			return;
		}

		mInventory.close();
		mInventory = null;
	}

	@Override
	public @NotNull Inventory getInventory() {
		Preconditions.checkState(mInventory != null, "getInventory() called after inventory was disposed");
		return mInventory;
	}

	/**
	 * Called when the GUI needs to be rendered.
	 * <p>
	 * Implementations should use {@link #setItem(int, GuiItem)} to populate the inventory.
	 * </p>
	 */
	@ApiStatus.OverrideOnly
	protected abstract void render();

	/**
	 * Called when a click occurs within the GUI inventory.
	 *
	 * @param event The inventory click event
	 * @return true if the event should continue to {@link GuiItem} processing, false otherwise
	 */
	@ApiStatus.OverrideOnly
	protected boolean onGuiClick(InventoryClickEvent event) {
		return true;
	}

	/**
	 * Called when a click occurs outside the GUI inventory.
	 *
	 * @param event The inventory click event
	 */
	@ApiStatus.OverrideOnly
	protected void onOutsideInventoryClick(InventoryClickEvent event) {
	}

	/**
	 * Called when a click occurs in the player's inventory while the GUI is open.
	 *
	 * @param event The inventory click event
	 */
	@ApiStatus.OverrideOnly
	protected void onPlayerInventoryClick(InventoryClickEvent event) {
	}

	/**
	 * Handles inventory drag events across multiple slots.
	 *
	 * @param event The InventoryDragEvent triggered by the player
	 */
	@ApiStatus.OverrideOnly
	protected void onInventoryDrag(InventoryDragEvent event) {
	}

	/**
	 * Called when the inventory is closed by the player or server.
	 *
	 * @param event The InventoryCloseEvent containing closure details
	 */
	@ApiStatus.OverrideOnly
	protected void onClose(InventoryCloseEvent event) {
	}

	/**
	 * Gets the current number of slots in the inventory.
	 *
	 * @return The size of the inventory, always a multiple of 9
	 */
	@ApiStatus.OverrideOnly
	protected int getSize() {
		return mItems.size();
	}

	/**
	 * Gets the current title displayed in the inventory window.
	 *
	 * @return The Component used as the inventory title, may be null
	 */
	@ApiStatus.OverrideOnly
	protected Component getTitle() {
		return mTitle;
	}
}
