package com.playmonumenta.plugins.chunk;

import org.bukkit.Chunk;
import org.bukkit.event.HandlerList;
import org.bukkit.event.world.ChunkEvent;
import org.jetbrains.annotations.NotNull;

public class ChunkPartialUnloadEvent extends ChunkEvent {
	private static final HandlerList handlers = new HandlerList();
	private boolean mNeedsSave;
	private boolean mUnloadingWorld;

	public ChunkPartialUnloadEvent(@NotNull Chunk chunk, boolean needsSave) {
		this(chunk, needsSave, false);
	}

	public ChunkPartialUnloadEvent(@NotNull Chunk chunk, boolean needsSave, boolean unloadingWorld) {
		super(chunk);
		mNeedsSave = needsSave;
		mUnloadingWorld = unloadingWorld;
	}

	public boolean isNeedsSave() {
		return mNeedsSave;
	}

	public void setSaveChunk(boolean saveChunk) {
		mNeedsSave = saveChunk;
	}

	public boolean isUnloadingWorld() {
		return mUnloadingWorld;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
