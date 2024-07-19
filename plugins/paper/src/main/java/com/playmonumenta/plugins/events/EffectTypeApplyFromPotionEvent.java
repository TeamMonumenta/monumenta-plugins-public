package com.playmonumenta.plugins.events;

import com.playmonumenta.plugins.itemstats.EffectType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

public class EffectTypeApplyFromPotionEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	private final LivingEntity mEntity;
	private final EffectType mEffectType;
	private double mStrength;
	private int mDuration;
	private final ItemStack mItem;
	private boolean mIsCancelled;

	public EffectTypeApplyFromPotionEvent(LivingEntity applied, EffectType effectType, double strength, int duration, ItemStack item) {
		mEntity = applied;
		mEffectType = effectType;
		mStrength = strength;
		mDuration = duration;
		mItem = item;
	}

	public EffectType getEffectType() {
		return mEffectType;
	}

	public LivingEntity getEntity() {
		return mEntity;
	}

	public double getStrength() {
		return mStrength;
	}

	public int getDuration() {
		return mDuration;
	}

	public ItemStack getItem() {
		return mItem;
	}

	public void setStrength(double strength) {
		mStrength = strength;
	}

	public void setDuration(int duration) {
		mDuration = duration;
	}

	@Override
	public boolean isCancelled() {
		return mIsCancelled;
	}

	@Override
	public void setCancelled(boolean arg0) {
		this.mIsCancelled = arg0;
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
