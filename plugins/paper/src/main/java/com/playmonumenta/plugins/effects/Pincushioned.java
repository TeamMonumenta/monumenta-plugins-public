package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Constants;

public class Pincushioned extends Effect {
	public static final String effectID = "PincushionedStacks";
	public static final String PINCUSHIONED_EFFECT_NAME = "PincushionedEffect";
	// pincushion duration is the duration of the debuff as a whole that enables stack gain/loss
	private static final int DURATION = 25 * Constants.TICKS_PER_SECOND;
	private static final int EXPLOSION_STACKS = 3;
	private int mStacks;

	public Pincushioned() {
	    super(DURATION, effectID);
	    incrementStacks();
	}

	/**
	 * Increases the stacks of Pincushion on the mob
	 * @return <code>true</code> if the threshold for Pincushion is hit
	 */
	public boolean incrementStacks() {
	    return incrementStacks(1);
	}

	public boolean incrementStacks(int stacks) {
	    setDuration(DURATION);
	    mStacks += stacks;

	    return mStacks >= EXPLOSION_STACKS;
	}

	public void clearStacks() {
	    mStacks = 0;
	}

	@Override
	public boolean isDebuff() {
	    return true;
	}

	@Override
	public String toString() {
	    return String.format(
	            "%s | duration:%s stacks:%s",
	            this.getClass().getName(),
	            getDuration(),
	            getMagnitude()
	    );
	}
}
