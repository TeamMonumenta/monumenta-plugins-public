package com.playmonumenta.plugins.events;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DamageShieldedEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();

	private boolean mIsCancelled;

	private final Player mPlayer;
	private final @Nullable LivingEntity mSource;
	private final EntityDamageEvent.DamageCause mCause;

	public DamageShieldedEvent(Player player, @Nullable LivingEntity source, EntityDamageEvent.DamageCause cause) {
		mPlayer = player;
		mSource = source;
		mCause = cause;
	}

	public Player getPlayer() {
		return mPlayer;
	}

	public @Nullable LivingEntity getSource() {
		return mSource;
	}

	public EntityDamageEvent.DamageCause getCause() {
		return mCause;
	}

	@Override
	public boolean isCancelled() {
		return mIsCancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.mIsCancelled = cancelled;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
