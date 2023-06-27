package com.playmonumenta.plugins.seasonalevents.gui;

import org.bukkit.entity.Player;

public abstract class View {
	protected final PassGui mGui;

	public View(PassGui gui) {
		mGui = gui;
	}

	public abstract void setup(Player displayedPlayer);
}
