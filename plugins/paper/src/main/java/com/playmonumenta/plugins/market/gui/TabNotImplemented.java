package com.playmonumenta.plugins.market.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;

public class TabNotImplemented implements MarketGuiTab {

	MarketGui mGui;

	public TabNotImplemented(MarketGui marketGUI) {
		this.mGui = marketGUI;
	}

	@Override
	public void setup() {
		mGui.mPlayer.sendMessage(Component.text("The selected tab is not yet implemented", NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, true));
		mGui.mPlayer.playSound(mGui.mPlayer.getLocation(), Sound.ENTITY_SHULKER_HURT, 1.0f, 0.7f);
		mGui.switchToTab(mGui.TAB_MAIN_MENU);
	}

	@Override
	public void onSwitch() {

	}
}
