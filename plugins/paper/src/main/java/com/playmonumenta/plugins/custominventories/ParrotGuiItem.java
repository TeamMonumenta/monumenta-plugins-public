package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.guis.GuiItem;
import java.util.Map;
import java.util.function.BiPredicate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class ParrotGuiItem extends GuiItem {
	private final ParrotCustomInventory mGUI;
	private final int mPage;
	private final int mSlot;
	private final @Nullable Map<ItemStack, Integer> mCost;
	private final @Nullable BiPredicate<Player, Inventory> mAfterClickFunction;
	private final @Nullable BiPredicate<Player, Inventory> mCondition;

	public ParrotGuiItem(ParrotCustomInventory gui, int page, int slot, ItemStack showedItem, @Nullable Map<ItemStack, Integer> cost, @Nullable BiPredicate<Player, Inventory> cond, @Nullable BiPredicate<Player, Inventory> afterClick) {
		super(showedItem);
		this.mGUI = gui;
		this.mPage = page;
		this.mSlot = slot;
		this.mCost = cost;
		this.mCondition = cond;
		this.mAfterClickFunction = afterClick;
		onClick(event -> {
			if (event.isShiftClick()) {
				return;
			}

			HumanEntity human = event.getWhoClicked();
			if (!(human instanceof Player player)) {
				return;
			}

			if (doesSomethingOnClick()) {
				if (canPurchase(player)) {
					if (purchase(player)) {
						player.playSound(player.getLocation(), Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundCategory.NEUTRAL, 10f, 1.3f);
						afterClick(player, event.getInventory());
						gui.refreshItems();
					} else {
						player.sendMessage(Component.text("[SYSTEM]", NamedTextColor.RED).decoration(TextDecoration.BOLD, true)
							.append(Component.text(" Error when purchasing the item! Please contact a moderator.", NamedTextColor.RED).decoration(TextDecoration.BOLD, false)));
					}
				} else {
					player.sendMessage(Component.text("You don't have enough currency to pay for this item.", NamedTextColor.RED));
				}
			}
		});
	}

	public ParrotGuiItem(ParrotCustomInventory gui, int page, int slot, ItemStack showedItem, @Nullable Map<ItemStack, Integer> cost, @Nullable BiPredicate<Player, Inventory> cond) {
		this(gui, page, slot, showedItem, cost, cond, null);
	}

	public ParrotGuiItem(ParrotCustomInventory gui, int page, int slot, ItemStack showedItem, @Nullable BiPredicate<Player, Inventory> cond, @Nullable BiPredicate<Player, Inventory> afterClick) {
		this(gui, page, slot, showedItem, null, cond, afterClick);
	}

	public ParrotGuiItem(ParrotCustomInventory gui, int page, int slot, ItemStack showedItemStack, @Nullable BiPredicate<Player, Inventory> cond) {
		this(gui, page, slot, showedItemStack, (Map<ItemStack, Integer>) null, cond);
	}

	public int getPage() {
		return mPage;
	}

	public int getSlot() {
		return mSlot;
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
		return new ParrotGuiItem(mGUI, mPage, mSlot, getItem(), mCost, mCondition, mAfterClickFunction);
	}

}
