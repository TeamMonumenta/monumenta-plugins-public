package com.playmonumenta.plugins.events;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.MagicType;

public class CustomDamageEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	private boolean mIsCancelled;
	private final Entity mDamager;
	private final LivingEntity mDamaged;
	private double mDamage;
	private final MagicType mMagicType;

	private final Spells mSpell;
	private final boolean mApplySpellshock;
	private final boolean mTriggerSpellshock;

	public CustomDamageEvent(Entity damager, LivingEntity damaged, double damage, MagicType magicType) {
		this(damager, damaged, damage, magicType, null, true, true);
	}

	public CustomDamageEvent(Entity damager, LivingEntity damaged, double damage, MagicType magicType, Spells spell, boolean applySpellshock, boolean triggerSpellshock) {
		mIsCancelled = false;
		mDamager = damager;
		mDamaged = damaged;
		mDamage = damage;
		if (magicType == null) {
			mMagicType = MagicType.NONE;
		} else {
			mMagicType = magicType;
		}
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
