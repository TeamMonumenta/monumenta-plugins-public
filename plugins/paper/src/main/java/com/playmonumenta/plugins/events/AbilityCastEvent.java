package com.playmonumenta.plugins.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.playmonumenta.plugins.classes.ClassAbility;

public class AbilityCastEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	private boolean mIsCancelled;

	private final Player mCaster;
	private final ClassAbility mSpell;

	public AbilityCastEvent(Player caster, ClassAbility spell) {
		mCaster = caster;
		mSpell = spell;
	}

	public Player getCaster() {
		return mCaster;
	}

	public ClassAbility getAbility() {
		return mSpell;
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
