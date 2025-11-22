package com.playmonumenta.plugins.chunk;

import java.util.Collections;
import java.util.List;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.event.HandlerList;
import org.bukkit.event.world.ChunkEvent;
import org.jetbrains.annotations.NotNull;

public class ChunkPartialUnloadEvent extends ChunkEvent {
	private static final HandlerList handlers = new HandlerList();
	private boolean mNeedsSave;
	private final boolean mUnloadingWorld;
	private final List<Entity> mEntityList;

	public ChunkPartialUnloadEvent(@NotNull Chunk chunk, boolean needsSave) {
		this(chunk, needsSave, false, Collections.emptyList());
	}

	public ChunkPartialUnloadEvent(@NotNull Chunk chunk, boolean needsSave, List<Entity> entityList) {
		this(chunk, needsSave, false, entityList);
	}

	public ChunkPartialUnloadEvent(@NotNull Chunk chunk, boolean needsSave, boolean unloadingWorld) {
		this(chunk, needsSave, unloadingWorld, Collections.emptyList());
	}

	public ChunkPartialUnloadEvent(@NotNull Chunk chunk, boolean needsSave, boolean unloadingWorld, List<Entity> entityList) {
		super(chunk);
		mNeedsSave = needsSave;
		mUnloadingWorld = unloadingWorld;
		mEntityList = entityList;
	}

	public boolean isNeedsSave() {
		return mNeedsSave;
	}

	public List<Entity> getEntities() {
		return mEntityList;
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
