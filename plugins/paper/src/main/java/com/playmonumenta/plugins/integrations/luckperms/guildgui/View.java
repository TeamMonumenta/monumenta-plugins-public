package com.playmonumenta.plugins.integrations.luckperms.guildgui;

public abstract class View {
	protected final GuildGui mGui;

	public View(GuildGui gui) {
		mGui = gui;
		mGui.mPage = 0;
	}

	public abstract void setup();

	/*
	 * Reloads any data that may have changed, then runs mGui.update()
	 * If async code is required, mGui.syncUpdate() may be used as a shortcut
	 */
	public void refresh() {
	}
}
