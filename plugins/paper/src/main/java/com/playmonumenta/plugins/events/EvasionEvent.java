package com.playmonumenta.plugins.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.utils.EntityUtils;

public class EvasionEvent extends Event {

	private static final HandlerList handlers = new HandlerList();

	private final Player mPlayer;
	private final double mDamage;
	private final EntityDamageByEntityEvent mDamageEvent;

	public EvasionEvent(Player player, double damage, EntityDamageByEntityEvent damageEvent) {
		mPlayer = player;
		mDamage = damage;
		mDamageEvent = damageEvent;
	}

	public Player getPlayer() {
		return mPlayer;
	}

	public double getFinalDamage() {
		// Bugged case where it was ability damage and we don't have proper calculations
		if (mDamageEvent == null) {
			return mDamage;
		} else {
			return EntityUtils.getRealFinalDamage(mDamageEvent);
		}
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
