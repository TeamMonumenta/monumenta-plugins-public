package com.playmonumenta.plugins.chunk;

import org.bukkit.Chunk;
import org.bukkit.event.HandlerList;
import org.bukkit.event.world.ChunkEvent;
import org.jetbrains.annotations.NotNull;

public class ChunkFullLoadEvent extends ChunkEvent {
	private static final HandlerList handlers = new HandlerList();

	public ChunkFullLoadEvent(@NotNull Chunk chunk) {
		super(chunk);
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
