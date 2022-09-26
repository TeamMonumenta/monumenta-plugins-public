package com.playmonumenta.plugins.events;

import com.playmonumenta.plugins.effects.Effect;
import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class CustomEffectApplyEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	private boolean mIsCancelled;

	private Entity mEntity;
	private Effect mEffect;
	private String mSource;

	public CustomEffectApplyEvent(Entity entity, Effect effect, String source) {
		mEntity = entity;
		mEffect = effect;
		mSource = source;
	}

	public Entity getEntity() {
		return mEntity;
	}

	public Effect getEffect() {
		return mEffect;
	}

	public String getSource() {
		return mSource;
	}

	public void setEntity(Entity entity) {
		mEntity = entity;
	}

	public void setEffect(Effect effect) {
		mEffect = effect;
	}

	public void setSource(String source) {
		mSource = source;
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
