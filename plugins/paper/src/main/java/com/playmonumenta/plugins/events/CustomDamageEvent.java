package com.playmonumenta.plugins.events;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.magic.MagicType;

public class CustomDamageEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	private boolean mIsCancelled;
	private final Entity mDamager;
	private final LivingEntity mDamaged;
	private double mDamage;
	private final @Nullable MagicType mMagicType;

	/*
	 * We want this event to trigger on all custom damage,
	 * but this flag determines if abilities should be
	 * able to trigger off this event.
	 */
	private final boolean mRegistered;

	private final @Nullable ClassAbility mSpell;
	private final boolean mApplySpellshock;
	private final boolean mTriggerSpellshock;

	public CustomDamageEvent(Entity damager, LivingEntity damaged, double damage, @Nullable MagicType magicType) {
		this(damager, damaged, damage, magicType, true, null, true, true);
	}

	public CustomDamageEvent(Entity damager, LivingEntity damaged, double damage, @Nullable MagicType magicType, boolean registered,
	                         @Nullable ClassAbility spell, boolean applySpellshock, boolean triggerSpellshock) {
		mIsCancelled = false;
		mDamager = damager;
		mDamaged = damaged;
		mDamage = damage;
		if (magicType == null) {
			mMagicType = MagicType.NONE;
		} else {
			mMagicType = magicType;
		}
		mRegistered = registered;
		mSpell = spell;
		mApplySpellshock = applySpellshock;
		mTriggerSpellshock = triggerSpellshock;
	}

	@Override
	public boolean isCancelled() {
		return mIsCancelled;
	}

	@Override
	public void setCancelled(boolean arg0) {
		mIsCancelled = arg0;
	}

	public @Nullable ClassAbility getSpell() {
		return mSpell;
	}

	public @Nullable MagicType getMagicType() {
		return mMagicType;
	}

	public boolean getRegistered() {
		return mRegistered;
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

	public boolean appliesSpellshock() {
		return mApplySpellshock;
	}

	public boolean triggersSpellshock() {
		return mTriggerSpellshock;
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
