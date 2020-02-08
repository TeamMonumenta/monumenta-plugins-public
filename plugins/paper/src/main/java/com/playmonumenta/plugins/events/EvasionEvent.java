package com.playmonumenta.plugins.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class EvasionEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	private final Player mPlayer;

	public EvasionEvent(Player player) {
		mPlayer = player;
	}

	public Player getPlayer() {
		return mPlayer;
	}

	// Mandatory Event Methods (If you remove these, I'm 99% sure the event will break)

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}
