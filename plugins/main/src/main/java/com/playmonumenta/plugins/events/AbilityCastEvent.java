package com.playmonumenta.plugins.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.playmonumenta.plugins.classes.Spells;

public class AbilityCastEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	public boolean isCancelled;

	private Player caster;
	private Spells spell;
	public AbilityCastEvent(Player caster, Spells spell) {
		this.caster = caster;
		this.spell = spell;
	}

	public Player getCaster() {
		return caster;
	}

	public Spells getAbility() {
		return spell;
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
