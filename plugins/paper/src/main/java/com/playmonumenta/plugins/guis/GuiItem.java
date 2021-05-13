package com.playmonumenta.plugins.guis;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GuiItem {
	int mPage;
	int mSlot;
	ItemStack mShowedItem;
	Map<ItemStack, Integer> mCost;
	DoubleParameterFunction<Player, Inventory, Boolean> mAfterClickFunction;
	DoubleParameterFunction<Player, Inventory, Boolean> mCondition;

	public GuiItem(int page, int slot, ItemStack showedItem, Map<ItemStack, Integer> cost, DoubleParameterFunction<Player, Inventory, Boolean> cond, DoubleParameterFunction<Player, Inventory, Boolean> afterClick) {
		this.mPage = page;
		this.mSlot = slot;
		this.mShowedItem = showedItem;
		this.mCost = cost;
		this.mCondition = cond;
		this.mAfterClickFunction = afterClick;
	}

	public GuiItem(int page, int slot, ItemStack showedItem, Map<ItemStack, Integer> cost, DoubleParameterFunction<Player, Inventory, Boolean> cond) {
		this(page, slot, showedItem, cost, cond, (i, s) -> {
			return true;
		});
	}

	public GuiItem(int page, int slot, ItemStack showedItem, DoubleParameterFunction<Player, Inventory, Boolean> cond, DoubleParameterFunction<Player, Inventory, Boolean> afterClick) {
		this(page, slot, showedItem, new HashMap<>(), cond, afterClick);
	}

	public GuiItem(int page, int slot, ItemStack showedItem, Map<ItemStack, Integer> cost) {
		this (page, slot, showedItem, cost, (i, s) -> {
			return true;
		});
	}

	public GuiItem(int slot, ItemStack showedItem, Map<ItemStack, Integer> cost) {
		this (0, slot, showedItem, cost);
	}

	public GuiItem(int slot, ItemStack showedItemStack) {
		this (0, slot, showedItemStack, new HashMap<>());
	}

	public GuiItem(int slot, ItemStack showedItemStack, DoubleParameterFunction<Player, Inventory, Boolean> cond) {
		this (0, slot, showedItemStack, new HashMap<>(), cond);
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
		return mCondition.applay(player, inventory);
	}

	public boolean afterClick(Player player, Inventory inventory) {
		return mAfterClickFunction.applay(player, inventory);
	}

	public boolean canPurcase(Player player) {
		if (player.getGameMode() == GameMode.CREATIVE) {
			return true;
		}
		//a player in creative don't have to pay

		if (isFree()) {
			return true;
		}
		//if the item is free return

		boolean result = true;
		for (ItemStack itemStack : mCost.keySet()) {
			int amount = mCost.get(itemStack);
			int times = 0;

			while (amount > 64) {
				itemStack.setAmount(64);
				if (!player.getInventory().containsAtLeast(itemStack, 64)) {
					result = false;
					break;
				}
				player.getInventory().removeItem(itemStack);
				amount -= 64;
				times++;
			}
			if (!player.getInventory().containsAtLeast(itemStack, amount)) {
				result = false;
			}

			//refound the currency
			while (times > 0) {
				itemStack.setAmount(64);
				player.getInventory().addItem(itemStack);
				times--;
			}

			itemStack.setAmount(1);
		}

		return result;
	}

	public boolean purcase(Player player) {
		if (player.getGameMode() == GameMode.CREATIVE) {
			return true;
		}
		//a player in creative don't have to pay

		if (isFree()) {
			return true;
		}
		//if the item is free return

		for (ItemStack itemStack : mCost.keySet()) {
			int amount = mCost.get(itemStack);
			while (amount > 64) {
				itemStack.setAmount(64);
				player.getInventory().removeItem(itemStack);
				amount -= 64;
			}
			itemStack.setAmount(amount);
			player.getInventory().removeItem(itemStack);
		}

		return true;
	}

	//we consider that an item with no cost is free
	public boolean isFree() {
		return mCost.isEmpty();
	}

	/**
	 * @return a copy of this object
	 */
	public GuiItem copy() {
		return new GuiItem(mPage, mSlot, mShowedItem, mCost, mCondition, mAfterClickFunction);

	}

	@FunctionalInterface
	public interface DoubleParameterFunction<P, I, B> {
		B applay(P p, I i);
	}
}
