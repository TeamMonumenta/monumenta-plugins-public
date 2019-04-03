package com.playmonumenta.plugins.classes.magic;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.playmonumenta.plugins.classes.Spells;

public class CustomDamageEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	public boolean mIsCancelled;
	public Entity mDamager;
	public LivingEntity mDamaged;
	public double mDamage;
	public MagicType mMagicType;

	public Spells mSpell = null;

	public CustomDamageEvent(Entity damager, LivingEntity damaged, double damage, MagicType magicType) {
		mIsCancelled = false;
		mDamager = damager;
		mDamaged = damaged;
		mDamage = damage;
		if (magicType == null) {
			mMagicType = MagicType.NONE;
		} else {
			mMagicType = magicType;
		}
	}

	@Override
	public boolean isCancelled() {
		return mIsCancelled;
	}

	@Override
	public void setCancelled(boolean arg0) {
		mIsCancelled = arg0;
	}

	public void setSpell(Spells spell) {
		mSpell = spell;
	}

	public Spells getSpell() {
		return mSpell;
	}

	public MagicType getMagicType() {
		return mMagicType;
	}

	public Entity getDamager() {
		return mDamager;
	}

	public LivingEntity getDamaged() {
		return mDamaged;
	}

	public double getDamage() {
		return mDamage;
	}

	public void setDamage(double damage) {
		mDamage = damage;
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
