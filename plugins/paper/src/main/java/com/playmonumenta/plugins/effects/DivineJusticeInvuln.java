package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Constants;

/**
 * Special effect for Divine Justice that helps implements custom invulnerability ticks for mobs.<br>
 * If a mob gets hit by Divine Justice within <code>DURATION</code> ticks, apply additional damage only if
 * the new event applies more damage than the old event
 */
public class DivineJusticeInvuln extends Effect {
	public static final String effectID = "DivineJusticeInvuln";
	public static final String SOURCE = "DivineJusticeInvuln";
	public static final int DURATION = Constants.HALF_TICKS_PER_SECOND;
	private final double mAmount;

	public DivineJusticeInvuln(final int duration, final double amount) {
		super(duration, effectID);
		mAmount = amount;
	}

	@Override
	public double getMagnitude() {
		return mAmount;
	}

	@Override public String toString() {
		return String.format("DivineJusticeInvuln duration:%d amount:%f", getDuration(), mAmount);
	}
}
