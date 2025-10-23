package com.playmonumenta.plugins.seasonalevents.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RewardsView extends View {
	private static final int ROW_SIZE = 5;
	private static final int ROWS_SHOWN = 3;
	private static final int PAGE_DELTA = 3;

	int mStartRow = 0;

	public RewardsView(PassGui gui) {
		super(gui);
	}

	@Override
	public void setup(Player displayedPlayer) {
		int maxRow = getMaxRow();
		mStartRow = Math.max(0, Math.min(mStartRow, maxRow));

		// Arrows
		ItemStack item;
		ItemMeta meta;

		if (mStartRow > 0) {
			item = new ItemStack(Material.ARROW);
			meta = item.getItemMeta();
			meta.displayName(Component.text("Previous Rewards", NamedTextColor.WHITE, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(meta);
			mGui.setItem(5, 0, item).onClick((InventoryClickEvent event) -> prevPage());
		}

		if (mStartRow < maxRow) {
			item = new ItemStack(Material.ARROW);
			meta = item.getItemMeta();
			meta.displayName(Component.text("Next Rewards", NamedTextColor.WHITE, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(meta);
			mGui.setItem(5, 8, item).onClick((InventoryClickEvent event) -> nextPage());
		}

		// Rewards
		for (int pageRow = 0; pageRow < ROWS_SHOWN; pageRow++) {
			int y = 2 * pageRow;
			int absoluteRow = mStartRow + pageRow;
			int rowStartRewardIndex = absoluteRow * ROW_SIZE;
			for (int i = 0; i < ROW_SIZE; i++) {
				int x = i + 2;
				int rewardIndex = i + rowStartRewardIndex;
				mGui.addRewardIndicatorIcon(y, x, displayedPlayer, rewardIndex);
				mGui.addRewardItem(y + 1, x, displayedPlayer, rewardIndex);
			}
		}
	}

	private int getMaxRow() {
		return (mGui.mPass.mRewards.size() / ROW_SIZE) - (ROWS_SHOWN - 1);
	}

	public void prevPage() {
		mStartRow -= PAGE_DELTA;
		mGui.updateWithPageSound();
	}

	public void nextPage() {
		mStartRow += PAGE_DELTA;
		mGui.updateWithPageSound();
	}
}
