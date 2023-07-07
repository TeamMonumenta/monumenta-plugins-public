package com.playmonumenta.plugins.events;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class EntityGainAbsorptionEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	private boolean mIsCancelled;

	private final LivingEntity mEntity;
	private final int mDuration;

	private double mAmount;
	private double mMaxAmount;

	public EntityGainAbsorptionEvent(LivingEntity entity, double amount, double maxAmount, int duration) {
		mEntity = entity;
		mAmount = amount;
		mMaxAmount = maxAmount;
		mDuration = duration;
	}

	public LivingEntity getEntity() {
		return mEntity;
	}

	public double getAmount() {
		return mAmount;
	}

	public void setAmount(double amount) {
		mAmount = amount;
	}

	public double getMaxAmount() {
		return mMaxAmount;
	}

	public void setMaxAmount(double maxAmount) {
		mMaxAmount = maxAmount;
	}

	public int getDuration() {
		return mDuration;
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
