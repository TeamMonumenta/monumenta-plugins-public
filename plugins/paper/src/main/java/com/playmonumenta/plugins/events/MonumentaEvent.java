package com.playmonumenta.plugins.events;

import java.time.Instant;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MonumentaEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	private boolean mIsCancelled;

	private final Player mPlayer;
	private final String mEvent;
	private final Instant mTimestamp;

	public MonumentaEvent(Player player, String event) {
		mPlayer = player;
		mEvent = event;
		mTimestamp = Instant.now();
	}

	public Player getPlayer() {
		return mPlayer;
	}

	public String getEvent() {
		return mEvent;
	}

	public Instant getTimestamp() {
		return mTimestamp;
	}

	@Override
	public boolean isCancelled() {
		return mIsCancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.mIsCancelled = cancelled;
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
