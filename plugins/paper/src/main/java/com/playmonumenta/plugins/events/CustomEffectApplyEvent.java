package com.playmonumenta.plugins.events;

import com.playmonumenta.plugins.effects.Effect;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CustomEffectApplyEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	private boolean mIsCancelled;

	private final Entity mEntity;
	private final Entity mApplier;
	private final Effect mEffect;

	public CustomEffectApplyEvent(Entity entity, Entity applier, Effect effect) {
		mEntity = entity;
		mApplier = applier;
		mEffect = effect;
	}

	public Entity getEntity() {
		return mEntity;
	}

	public Entity getApplier() {
		return mApplier;
	}

	public Effect getEffect() {
		return mEffect;
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
