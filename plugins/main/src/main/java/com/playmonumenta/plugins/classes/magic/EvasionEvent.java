package com.playmonumenta.plugins.classes.magic;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class EvasionEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	private final Player player;

	public EvasionEvent(Player player) {
		this.player = player;
	}

	public Player getPlayer() {
		return player;
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
