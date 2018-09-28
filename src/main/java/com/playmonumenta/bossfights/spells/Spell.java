package com.playmonumenta.bossfights.spells;

public interface Spell {
	/*
	 * Used by some spells to indicate if they can be run
	 * now (true) or not (false)
	 */
	default boolean canRun() {
		return true;
	}
	void run();

	/* How long this spell takes to cast (in ticks) */
	int duration();
}
