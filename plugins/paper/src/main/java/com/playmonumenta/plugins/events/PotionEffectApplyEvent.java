package com.playmonumenta.plugins.events;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.potion.PotionEffect;

public class PotionEffectApplyEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	private boolean isCancelled;
	private Entity applier;
	private LivingEntity applied;
	private PotionEffect effect;

	public PotionEffectApplyEvent(Entity applier, LivingEntity applied, PotionEffect effect) {
		this.applier = applier;
		this.applied = applied;
		this.effect = effect;
	}

	public PotionEffect getEffect() {
		return effect;
	}

	public LivingEntity getApplied() {
		return applied;
	}

	public Entity getApplier() {
		return applier;
	}

	public void setEffect(PotionEffect effect) {
		this.effect = effect;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean arg0) {
		this.isCancelled = arg0;
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
