package com.playmonumenta.plugins.classes.magic;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.playmonumenta.plugins.classes.Spells;

public class CustomDamageEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	public boolean isCancelled;
	public Entity damager;
	public LivingEntity damaged;
	public double damage;
	public MagicType magicType;

	public Spells spell = null;

	public CustomDamageEvent(Entity damager, LivingEntity damaged, double damage, MagicType magicType) {
		this.isCancelled = false;
		this.damager = damager;
		this.damaged = damaged;
		this.damage = damage;
		this.magicType = magicType;
	}

	public CustomDamageEvent(Entity damager, LivingEntity damaged, double damage) {
		this.isCancelled = false;
		this.damager = damager;
		this.damaged = damaged;
		this.damage = damage;
		this.magicType = MagicType.NONE;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean arg0) {
		this.isCancelled = arg0;
	}

	public void setSpell(Spells spell) {
		this.spell = spell;
	}

	public Spells getSpell() {
		return spell;
	}

	public MagicType getMagicType() {
		return magicType;
	}

	public Entity getDamager() {
		return damager;
	}

	public LivingEntity getDamaged() {
		return damaged;
	}

	public double getDamage() {
		return damage;
	}

	public void setDamage(double damage) {
		this.damage = damage;
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
