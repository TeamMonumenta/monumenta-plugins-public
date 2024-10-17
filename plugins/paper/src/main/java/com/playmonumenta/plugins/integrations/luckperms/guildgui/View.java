package com.playmonumenta.plugins.integrations.luckperms.guildgui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public abstract class View {
	protected final GuildGui mGui;
	protected int mPage = 0;

	public View(GuildGui gui) {
		mGui = gui;
	}

	public abstract void setup();

	/*
	 * Reloads any data that may have changed, then runs mGui.update()
	 * If async code is required, mGui.syncUpdate() may be used as a shortcut
	 */
	public void refresh() {
	}

	protected int entriesPerPage() {
		return GuildGui.PAGE_HEIGHT;
	}

	protected void setPageArrows(int totalRows) {
		int maxPage = Math.floorDiv(Math.max(0, totalRows - 1), entriesPerPage());
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
			mGui.setItem(GuildGui.HEADER_Y, 0, item).onClick((InventoryClickEvent event) -> clickPrev());
		}

		if (mPage < maxPage) {
			item = new ItemStack(Material.ARROW);
			meta = item.getItemMeta();
			meta.displayName(Component.text("Next Page", NamedTextColor.WHITE, TextDecoration.BOLD)
				.decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(meta);
			mGui.setItem(GuildGui.HEADER_Y, 8, item).onClick((InventoryClickEvent event) -> clickNext());
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
}
