package com.playmonumenta.plugins.custominventories;

import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

public class GuiItem {
	private final int mPage;
	private final int mSlot;
	private final ItemStack mShowedItem;
	private final @Nullable Map<ItemStack, Integer> mCost;
	private final @Nullable DoubleParameterFunction<Player, Inventory, Boolean> mAfterClickFunction;
	private final @Nullable DoubleParameterFunction<Player, Inventory, Boolean> mCondition;

	public GuiItem(int page, int slot, ItemStack showedItem, @Nullable Map<ItemStack, Integer> cost, @Nullable DoubleParameterFunction<Player, Inventory, Boolean> cond, @Nullable DoubleParameterFunction<Player, Inventory, Boolean> afterClick) {
		this.mPage = page;
		this.mSlot = slot;
		this.mShowedItem = showedItem;
		this.mCost = cost;
		this.mCondition = cond;
		this.mAfterClickFunction = afterClick;
	}

	public GuiItem(int page, int slot, ItemStack showedItem, @Nullable Map<ItemStack, Integer> cost, @Nullable DoubleParameterFunction<Player, Inventory, Boolean> cond) {
		this(page, slot, showedItem, cost, cond, null);
	}

	public GuiItem(int page, int slot, ItemStack showedItem, @Nullable DoubleParameterFunction<Player, Inventory, Boolean> cond, @Nullable DoubleParameterFunction<Player, Inventory, Boolean> afterClick) {
		this(page, slot, showedItem, null, cond, afterClick);
	}

	public GuiItem(int page, int slot, ItemStack showedItem, @Nullable Map<ItemStack, Integer> cost) {
		this(page, slot, showedItem, cost, null);
	}

	public GuiItem(int slot, ItemStack showedItem, @Nullable Map<ItemStack, Integer> cost) {
		this(0, slot, showedItem, cost);
	}

	public GuiItem(int slot, ItemStack showedItemStack) {
		this(0, slot, showedItemStack, (Map<ItemStack, Integer>) null);
	}

	public GuiItem(int page, int slot, ItemStack showedItemStack, @Nullable DoubleParameterFunction<Player, Inventory, Boolean> cond) {
		this(page, slot, showedItemStack, (Map<ItemStack, Integer>) null, cond);
	}

	public int getPage() {
		return mPage;
	}

	public int getSlot() {
		return mSlot;
	}

	public ItemStack getShowedItem() {
		return mShowedItem;
	}

	public boolean isVisible(Player player, Inventory inventory) {
		return mCondition == null || mCondition.apply(player, inventory);
	}

	public boolean afterClick(Player player, Inventory inventory) {
		if (mAfterClickFunction != null) {
			return mAfterClickFunction.apply(player, inventory);
		}
		return true;
	}

	public boolean doesSomethingOnClick() {
		return mAfterClickFunction != null;
	}

	public boolean canPurchase(Player player) {
		if (player.getGameMode() == GameMode.CREATIVE) {
			return true;
		}
		//a player in creative don't have to pay

		if (mCost == null || isFree()) {
			return true;
		}
		//if the item is free return

		for (Map.Entry<ItemStack, Integer> entry : mCost.entrySet()) {
			ItemStack itemStack = entry.getKey();
			int amount = entry.getValue();
			if (!player.getInventory().containsAtLeast(itemStack, amount)) {
				return false;
			}
		}

		return true;
	}

	public boolean purchase(Player player) {
		if (player.getGameMode() == GameMode.CREATIVE) {
			return true;
		}
		//a player in creative don't have to pay

		if (mCost == null || isFree()) {
			return true;
		}
		//if the item is free return

		for (Map.Entry<ItemStack, Integer> entry : mCost.entrySet()) {
			ItemStack itemStack = entry.getKey();
			int amount = entry.getValue();
			while (amount > 64) {
				itemStack.setAmount(64);
				player.getInventory().removeItem(itemStack);
				amount -= 64;
			}
			itemStack.setAmount(amount);
			player.getInventory().removeItem(itemStack);
			itemStack.setAmount(1);
		}

		return true;
	}

	//we consider that an item with no cost is free
	public boolean isFree() {
		return mCost == null || mCost.isEmpty();
	}

	/**
	 * @return a copy of this object
	 */
	public GuiItem copy() {
		return new GuiItem(mPage, mSlot, mShowedItem, mCost, mCondition, mAfterClickFunction);
	}

	@FunctionalInterface
	public interface DoubleParameterFunction<P, I, B> {
		B apply(P p, I i);
	}
}
