package com.playmonumenta.plugins.mail;

public class NoMailAccessException extends Exception {
	private boolean mCloseGui = true;

	public NoMailAccessException(String message) {
		super(message);
	}

	public void closeGui(boolean shouldClose) {
		mCloseGui = shouldClose;
	}

	public boolean closeGui() {
		return mCloseGui;
	}
}
