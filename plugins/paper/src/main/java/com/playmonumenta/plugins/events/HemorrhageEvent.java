package com.playmonumenta.plugins.events;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class HemorrhageEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	private final Player mCaster;
	private final LivingEntity mMob;

	public HemorrhageEvent(Player caster, LivingEntity mob) {
		mCaster = caster;
		mMob = mob;
	}

	public Player getCaster() {
		return mCaster;
	}

	public LivingEntity getMob() {
		return mMob;
	}

	// Mandatory Event Methods (If you remove these, I'm 99% sure the event will break)

	@Override
	@NotNull
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

}
