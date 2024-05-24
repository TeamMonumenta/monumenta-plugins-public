package com.playmonumenta.plugins.market.gui;

import com.playmonumenta.plugins.market.MarketListingIndex;
import com.playmonumenta.plugins.market.MarketManager;
import com.playmonumenta.plugins.utils.GUIUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;

public class TabModeratorMenu implements MarketGuiTab {

	MarketGui mGui;

	static final Component TAB_TITLE = Component.text("Moderation");
	static final int TAB_SIZE = 9;

	public TabModeratorMenu(MarketGui marketGUI) {
		this.mGui = marketGUI;
	}

	@Override
	public void setup() {
		mGui.setItem(0, MarketGuiIcons.BACK_TO_MAIN_MENU).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_MAIN_MENU));
		mGui.setItem(1, GUIUtils.createBasicItem(Material.BOOKSHELF, "Browse ALL listings", NamedTextColor.GOLD, true)).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_MODERATOR_BROWSER));
		mGui.setItem(2, GUIUtils.createBasicItem(Material.BOOK, "Refresh all indexes", NamedTextColor.GOLD, true)).onClick((clickEvent) -> MarketListingIndex.resyncAllIndexes(mGui.mPlayer));
		mGui.setItem(6, GUIUtils.createBasicItem(Material.CHAIN, "Lock all trades", NamedTextColor.RED, true)).onClick((clickEvent) -> MarketManager.lockAllListings(mGui.mPlayer));
		mGui.setItem(7, GUIUtils.createBasicItem(Material.IRON_BARS, "Read-only mode", NamedTextColor.RED, true)).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_NOT_IMPLEMENTED));
		mGui.setItem(8, GUIUtils.createBasicItem(Material.BARRIER, "Close market", NamedTextColor.RED, true)).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_NOT_IMPLEMENTED));
	}

	@Override
	public void onSwitch() {
		mGui.setTitle(TAB_TITLE);
		mGui.setSize(TAB_SIZE);
	}

	@Override
	public void onLeave() {

	}
}
