package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentSpeed;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;

public class SpellDistanceCloser extends Spell {

	private static final String EFFECT_NAME = "DistanceCloserPercentSpeedEffect";
	private static final int DURATION = 10;

	private final Plugin mPlugin = Plugin.getInstance();
	private final double mDistanceSquared;
	private final double mSpeed;

	private Mob mBoss;

	public SpellDistanceCloser(Mob boss, double distance, double speed) {
		mDistanceSquared = distance * distance;
		mSpeed = speed;
		mBoss = boss;
	}

	@Override
	public void run() {
		LivingEntity target = mBoss.getTarget();
		if (target != null && target.getLocation().distanceSquared(mBoss.getLocation()) > mDistanceSquared) {
			mPlugin.mEffectManager.addEffect(mBoss, EFFECT_NAME, new PercentSpeed(DURATION, mSpeed, EFFECT_NAME));
		}
	}

	@Override
	public int cooldownTicks() {
		return DURATION / 2;
	}

}
