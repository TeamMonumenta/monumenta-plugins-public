package com.playmonumenta.plugins.guis;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

/**
 * A helper class to make simple Minecraft item GUIs.
 * Extend this class and define all items in the overridden {@link #setup()} method, then just {@link #open()} it to show it to a player.
 */
public abstract class Gui {

	private static final Map<UUID, Gui> LAST_OPENED_INVENTORY = new HashMap<>();

	protected final Plugin mPlugin;
	public final Player mPlayer;
	protected int mSize;
	private Component mTitle;
	private boolean mTitleDirty = false;
	private GuiCustomInventory mCustomInventory;

	private final List<GuiItem> mItems;

	private @Nullable ItemStack mFiller = null;

	public Gui(Player player, int size, Component title) {
		mPlugin = Plugin.getInstance();
		mPlayer = player;
		mSize = size;
		mTitle = title;
		mCustomInventory = new GuiCustomInventory(size, title);
		mItems = new ArrayList<>(size);
	}

	/**
	 * Updates the GUI - will clear all items and call {@link #setup()} again to fill it anew.
	 * Will also update the title and GUI size if they have been changed.
	 */
	public void update() {
		mCustomInventory.getInventory().clear();
		mItems.clear();

		setup();

		List<HumanEntity> oldViewers = Collections.emptyList();
		if (mSize != mCustomInventory.getInventory().getSize()
			    || mTitleDirty
			    || mCustomInventory.mDiscarded) {
			oldViewers = new ArrayList<>(mCustomInventory.getInventory().getViewers());
			mCustomInventory.discard();
			mCustomInventory = new GuiCustomInventory(mSize, mTitle);
			mTitleDirty = false;
		}
		Inventory inventory = mCustomInventory.getInventory();
		for (int i = 0; i < mSize; i++) {
			GuiItem guiItem = i < mItems.size() ? mItems.get(i) : null;
			if (guiItem != null) {
				inventory.setItem(i, guiItem.mItem);
			} else {
				inventory.setItem(i, mFiller);
			}
		}
		for (HumanEntity human : oldViewers) {
			if (human instanceof Player player) {
				mCustomInventory.openInventory(player, mPlugin);
			}
		}
	}

	/**
	 * Shows this GUI to the player. Can safely be called multiple times.
	 */
	public void open() {
		update();
		LAST_OPENED_INVENTORY.put(mPlayer.getUniqueId(), this);
		mCustomInventory.openInventory(mPlayer, mPlugin);
	}

	/**
	 * Define GUI items in this method using the various {@link #setItem(int, int, ItemStack) setItem} methods.
	 */
	protected abstract void setup();

	/**
	 * Closes this GUI. Note that this will call {@link #onClose(InventoryCloseEvent) onClose}.
	 */
	public void close() {
		mCustomInventory.close();
	}

	/**
	 * Sets a filler item to fill empty slots with. The name of the filler item will be set to the empty string.
	 * <p>
	 * Can be called at any time from within {@link #setup()} or in the constructor. If called from anywhere else, needs an {@link #update()} call to become effective.
	 */
	public void setFiller(@Nullable Material fillerMaterial) {
		if (fillerMaterial == null) {
			mFiller = null;
			return;
		}
		ItemStack filler = new ItemStack(fillerMaterial, 1);
		ItemMeta meta = filler.getItemMeta();
		meta.displayName(Component.text(""));
		filler.setItemMeta(meta);
		mFiller = filler;
	}

	/**
	 * Sets a filler item to fill empty slots with.
	 */
	public void setFiller(@Nullable ItemStack filler) {
		mFiller = filler;
	}

	/**
	 * Changes the size of this GUI. Can be called at any time from within {@link #setup()}. If called from anywhere else, needs an {@link #update()} call to become effective.
	 */
	public void setSize(int newSize) {
		if (newSize < 9 || newSize > 6 * 9 || newSize % 9 != 0) {
			throw new IllegalArgumentException("Invalid GUI size " + newSize);
		}
		mSize = newSize;
		if (mItems.size() > mSize) {
			MMLog.warning("Resizing a GUI to be smaller than its contents! (num items=" + mItems.size() + ", new GUI size=" + mSize + ", GUI class=" + getClass() + ")");
		}
	}

	/**
	 * Changes the title of this GUI. Can be called at any time from within {@link #setup()}. If called from anywhere else, needs an {@link #update()} call to become effective.
	 */
	public void setTitle(Component newTitle) {
		if (mTitle.equals(newTitle)) {
			return;
		}
		mTitle = newTitle;
		mTitleDirty = true;
	}

	public GuiItem setItem(int row, int column, GuiItem item) {
		return setItem(row * 9 + column, item);
	}

	public GuiItem setItem(int index, GuiItem item) {
		if (index < 0 || index >= 6 * 9) {
			throw new IllegalArgumentException("Invalid item index " + index);
		}
		if (index >= mSize) {
			MMLog.warning("Invalid item index " + index + " for inventory of size " + mSize + " (GUI class=" + getClass() + ")");
			return item;
		}
		while (mItems.size() <= index) {
			mItems.add(null);
		}
		mItems.set(index, item);
		return item;
	}

	public GuiItem setItem(int row, int column, ItemStack item) {
		return setItem(row, column, new GuiItem(item));
	}

	public GuiItem setItem(int index, ItemStack item) {
		return setItem(index, new GuiItem(item));
	}

	/**
	 * Called when the player clicks in the GUI area. Only use this if {@link GuiItem#onClick(Consumer)} is not sufficient for your use case.
	 * If this returns false, no item's onClick handler will be called.
	 */
	protected boolean onGuiClick(InventoryClickEvent event) {
		return true;
	}

	/**
	 * Called when the player clicks in the player inventory area. Useful to perform actions on player items.
	 */
	protected void onPlayerInventoryClick(InventoryClickEvent event) {

	}

	protected void onOutsideInventoryClick(InventoryClickEvent event) {

	}

	protected void onInventoryDrag(InventoryDragEvent event) {

	}

	/**
	 * Called when this GUI is closed, whether by the player, {@link #close()}, or another inventory or GUI being shown to the player,
	 * or other reasons (check the {@link InventoryCloseEvent#getReason() event reason} for details).
	 */
	protected void onClose(InventoryCloseEvent event) {

	}

	private class GuiCustomInventory extends CustomInventory {
		private boolean mDiscarded = false;

		public GuiCustomInventory(int size, Component title) {
			super(mPlayer, size, title);
		}

		@Override
		protected void inventoryClick(InventoryClickEvent event) {
			event.setCancelled(true);
			if (mDiscarded) {
				MMLog.warning("GuiCustomInventory received click event after being discarded (GUI class=" + Gui.this.getClass() + ")");
				return;
			}
			if (event.getClickedInventory() == mInventory) {
				if (!onGuiClick(event)) {
					return;
				}
				if (event.getSlot() < mItems.size()) {
					GuiItem item = mItems.get(event.getSlot());
					if (item != null) {
						item.clicked(event);
					}
				}
			} else if (event.getClickedInventory() != null) {
				onPlayerInventoryClick(event);
			} else {
				onOutsideInventoryClick(event);
			}
		}

		@Override
		protected void inventoryDrag(InventoryDragEvent event) {
			event.setCancelled(true);
			onInventoryDrag(event);
		}

		@Override
		protected void inventoryClose(InventoryCloseEvent event) {
			if (!mDiscarded) {
				onClose(event);
				mDiscarded = true;
			}
		}

		public void discard() {
			mDiscarded = true;
		}
	}

	public static @Nullable Gui getOpenGui(Player player) {
		Gui lastOpenedGui = LAST_OPENED_INVENTORY.get(player.getUniqueId());
		if (lastOpenedGui != null && lastOpenedGui.mCustomInventory.getInventory().equals(player.getOpenInventory().getTopInventory())) {
			return lastOpenedGui;
		}
		return null;
	}

	public static void playerQuit(Player player) {
		LAST_OPENED_INVENTORY.remove(player.getUniqueId());
	}

}
