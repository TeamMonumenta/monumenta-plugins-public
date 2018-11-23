package com.playmonumenta.bossfights.spells;

import java.util.LinkedHashSet;
import java.util.Set;

import org.bukkit.scheduler.BukkitRunnable;

public abstract class Spell {
	protected final Set<BukkitRunnable> mActiveRunnables = new LinkedHashSet<BukkitRunnable>();

	/*
	 * Used by some spells to indicate if they can be run
	 * now (true) or not (false)
	 */
	public boolean canRun() {
		return true;
	}
	public abstract void run();

	/*
	 * Cancels all currently running tasks (tasks in mActiveRunnables)
	 *
	 * To use this functionality, user needs to add every BukkitRunnable created to mActiveRunnables,
	 * and then remove them from mActiveRunnables when they are finished
	 */
	public final void cancel() {
		for (BukkitRunnable runnable : mActiveRunnables) {
			runnable.cancel();
		}
	}

	/* How long this spell takes to cast (in ticks) */
	public abstract int duration();
}
