package com.playmonumenta.plugins.guis;

import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * An item in a {@link Gui}.
 */
public class GuiItem {

	final ItemStack mItem;
	private final List<Consumer<InventoryClickEvent>> mClickListeners = new ArrayList<>(0);

	public GuiItem(ItemStack item) {
		this(item, true);
	}

	public GuiItem(ItemStack item, boolean setPlainTag) {
		if (setPlainTag) {
			ItemUtils.setPlainTag(item);
		}
		mItem = item;
	}

	/**
	 * Defines an action to be executed when this item is left-clicked.
	 *
	 * @return This {@link GuiItem} (for method chaining)
	 */
	public GuiItem onLeftClick(Runnable onClick) {
		return onClick(event -> {
			if (event.getClick() == ClickType.LEFT) {
				onClick.run();
			}
		});
	}

	/**
	 * Defines an action to be executed when this item is right-clicked.
	 *
	 * @return This {@link GuiItem} (for method chaining)
	 */
	public GuiItem onRightClick(Runnable onClick) {
		return onClick(event -> {
			if (event.getClick() == ClickType.RIGHT) {
				onClick.run();
			}
		});
	}

	/**
	 * Defines an action to be executed when this item is clicked. Will handle all click types - <b>make sure to check for click type</b>,
	 * as some are most likely unwanted (e.g. drop or swap with hotbar).
	 *
	 * @return This {@link GuiItem} (for method chaining)
	 */
	public GuiItem onClick(Consumer<InventoryClickEvent> onClick) {
		mClickListeners.add(onClick);
		return this;
	}

	void clicked(InventoryClickEvent event) {
		for (Consumer<InventoryClickEvent> clickListener : mClickListeners) {
			clickListener.accept(event);
		}
	}

}
