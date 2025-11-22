package com.playmonumenta.plugins.effects;

/**
 * When a mob is hit by a projectile, give them an Iframe effect with the magnitude equivalent to its projectile damage.
 * If mob is hit by a projectile but the damage is lower than an existing iframe, negate that attack.
 * If mob is hit by a projectile and incoming damage is higher than existing iframe, add a new effect with equal duration
 * and hurt the mob with damage (Damage - Magnitude), and the new effect has the magnitude of total incoming damage.
 * Most of the logic is done in DamageListener.
 */
public class ProjectileIframe extends Effect {
	public static final String effectID = "ProjectileIframe";
	public static final String SOURCE = "ProjectileIframe";
	public static final int IFRAME_DURATION = 10;
	private final double mAmount;

	public ProjectileIframe(final int duration, final double amount) {
		super(duration, effectID);
		mAmount = amount;
	}

	public ProjectileIframe cleanCopy() {
		ProjectileIframe copy = new ProjectileIframe(mDuration, mAmount);
		copy.mUsed = false;
		return copy;
	}

	@Override
	public double getMagnitude() {
		return mAmount;
	}

	@Override
	public String toString() {
		return String.format("ProjectileIframe duration:%d amount:%f", getDuration(), mAmount);
	}
}
