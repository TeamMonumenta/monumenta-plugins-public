package com.playmonumenta.plugins.market.gui;

import net.kyori.adventure.text.Component;

public class TabMainMenu implements MarketGuiTab {

	MarketGui mGui;

	static final Component TAB_TITLE = Component.text("Player Market");
	static final int TAB_SIZE = 6 * 9;

	public TabMainMenu(MarketGui marketGUI) {
		this.mGui = marketGUI;
	}

	@Override
	public void setup() {
		mGui.setItem(1, 4, MarketGuiIcons.BAZAAR_LISTING_BROWSER).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_BAZAAR_BROWSER));
		mGui.setItem(3, 3, MarketGuiIcons.PLAYER_LISTINGS).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_PLAYER_LISTINGS));
		mGui.setItem(3, 5, MarketGuiIcons.ADD_LISTING).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_ADD_LISTING));
		if (mGui.mIsOp) {
			mGui.setItem(5, 8, MarketGuiIcons.MODERATOR_MENU).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_MODERATOR_MENU));
		}
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
