package com.playmonumenta.plugins.events;

import com.playmonumenta.plugins.managers.GlowingManager.GlowingInstance;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;

public class EntityGlowEvent extends EntityEvent implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	private boolean mIsCancelled = false;

	private final GlowingInstance mGlowingInstance;

	public EntityGlowEvent(Entity entity, GlowingInstance glowingInstance) {
		super(entity);
		mGlowingInstance = glowingInstance;
	}

	public GlowingInstance getGlowingInstance() {
		return mGlowingInstance;
	}

	public int getGlowingPriority() {
		return mGlowingInstance.mPriority();
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
