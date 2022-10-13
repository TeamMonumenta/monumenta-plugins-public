package com.playmonumenta.plugins.custominventories;

import java.util.Map;
import java.util.function.BiPredicate;
import javax.annotation.Nullable;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ParrotGuiItem {
	private final int mPage;
	private final int mSlot;
	private final ItemStack mShowedItem;
	private final @Nullable Map<ItemStack, Integer> mCost;
	private final @Nullable BiPredicate<Player, Inventory> mAfterClickFunction;
	private final @Nullable BiPredicate<Player, Inventory> mCondition;

	public ParrotGuiItem(int page, int slot, ItemStack showedItem, @Nullable Map<ItemStack, Integer> cost, @Nullable BiPredicate<Player, Inventory> cond, @Nullable BiPredicate<Player, Inventory> afterClick) {
		this.mPage = page;
		this.mSlot = slot;
		this.mShowedItem = showedItem;
		this.mCost = cost;
		this.mCondition = cond;
		this.mAfterClickFunction = afterClick;
	}

	public ParrotGuiItem(int page, int slot, ItemStack showedItem, @Nullable Map<ItemStack, Integer> cost, @Nullable BiPredicate<Player, Inventory> cond) {
		this(page, slot, showedItem, cost, cond, null);
	}

	public ParrotGuiItem(int page, int slot, ItemStack showedItem, @Nullable BiPredicate<Player, Inventory> cond, @Nullable BiPredicate<Player, Inventory> afterClick) {
		this(page, slot, showedItem, null, cond, afterClick);
	}

	public ParrotGuiItem(int page, int slot, ItemStack showedItem, @Nullable Map<ItemStack, Integer> cost) {
		this(page, slot, showedItem, cost, null);
	}

	public ParrotGuiItem(int slot, ItemStack showedItem, @Nullable Map<ItemStack, Integer> cost) {
		this(0, slot, showedItem, cost);
	}

	public ParrotGuiItem(int slot, ItemStack showedItemStack) {
		this(0, slot, showedItemStack, (Map<ItemStack, Integer>) null);
	}

	public ParrotGuiItem(int page, int slot, ItemStack showedItemStack, @Nullable BiPredicate<Player, Inventory> cond) {
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
		return mCondition == null || mCondition.test(player, inventory);
	}

	public boolean afterClick(Player player, Inventory inventory) {
		if (mAfterClickFunction != null) {
			return mAfterClickFunction.test(player, inventory);
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

	public ParrotGuiItem copy() {
		return new ParrotGuiItem(mPage, mSlot, mShowedItem, mCost, mCondition, mAfterClickFunction);
	}

}
