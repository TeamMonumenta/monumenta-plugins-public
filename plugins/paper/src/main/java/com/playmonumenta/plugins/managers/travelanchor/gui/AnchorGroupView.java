package com.playmonumenta.plugins.managers.travelanchor.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public abstract class AnchorGroupView {
	public static final int HEADER_Y = 0;

	protected final AnchorGroupGui mGui;
	protected int mPage = 0;

	public AnchorGroupView(AnchorGroupGui gui) {
		mGui = gui;
	}

	public abstract void setup();

	protected int rowsPerPage() {
		return 5;
	}

	protected void setPageArrows(int totalRows) {
		int maxPage = Math.floorDiv(Math.max(0, totalRows - 1), rowsPerPage());
		mPage = Math.max(0, Math.min(mPage, maxPage));

		ItemStack item;
		ItemMeta meta;

		// Prev/Next page buttons
		if (mPage > 0) {
			item = new ItemStack(Material.ARROW);
			meta = item.getItemMeta();
			meta.displayName(Component.text("Previous Page", NamedTextColor.WHITE, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(meta);
			mGui.setItem(HEADER_Y, 0, item).onClick((InventoryClickEvent event) -> clickPrev());
		}

		if (mPage < maxPage) {
			item = new ItemStack(Material.ARROW);
			meta = item.getItemMeta();
			meta.displayName(Component.text("Next Page", NamedTextColor.WHITE, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(meta);
			mGui.setItem(HEADER_Y, 8, item).onClick((InventoryClickEvent event) -> clickNext());
		}
	}

	private void clickPrev() {
		mPage--;
		mGui.mPlayer.playSound(mGui.mPlayer, Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0f, 1.0f);
		mGui.update();
	}

	private void clickNext() {
		mPage++;
		mGui.mPlayer.playSound(mGui.mPlayer, Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0f, 1.0f);
		mGui.update();
	}

	protected void handleShiftClickedFromInventory(ItemStack item) {
	}
}
