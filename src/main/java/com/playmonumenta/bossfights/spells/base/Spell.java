package com.playmonumenta.bossfights.spells;

public interface Spell
{
	/*
	 * Used by some spells to indicate if they can be run
	 * now (true) or not (false)
	 */
	default public boolean canRun() {
		return true;
	}
	public void run();
}
