package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.utils.EntityUtils;

public class SpellInspire extends Spell {

	private static final String PERCENT_SPEED_EFFECT_NAME = "InspirePercentSpeedEffect";
	private static final String PERCENT_DAMAGE_DEALT_EFFECT_NAME = "InspirePercentDamageDealtEffect";
	private static final String PERCENT_DAMAGE_RECEIVED_EFFECT_NAME = "InspirePercentDamageReceivedEffect";
	private static final double PERCENT_SPEED_EFFECT = 0.15;
	private static final double PERCENT_DAMAGE_DEALT_EFFECT = 0.15;
	private static final double PERCENT_DAMAGE_RECEIVED_EFFECT = -0.3;

	private final Plugin mPlugin;
	private final World mWorld;
	private final LivingEntity mBoss;
	private final int mRange;

	public SpellInspire(Plugin plugin, LivingEntity boss, int range) {
		mPlugin = plugin;
		mWorld = boss.getWorld();
		mBoss = boss;
		mRange = range;
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation();
		mWorld.spawnParticle(Particle.FIREWORKS_SPARK, loc, mRange * mRange / 8, mRange / 1.5, 0, mRange / 1.5, 0.05);

		for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, mRange)) {
			mPlugin.mEffectManager.addEffect(mob, PERCENT_SPEED_EFFECT_NAME,
					new PercentSpeed(20, PERCENT_SPEED_EFFECT, PERCENT_SPEED_EFFECT_NAME));
			mPlugin.mEffectManager.addEffect(mob, PERCENT_DAMAGE_DEALT_EFFECT_NAME,
					new PercentDamageDealt(20, PERCENT_DAMAGE_DEALT_EFFECT));
			mPlugin.mEffectManager.addEffect(mob, PERCENT_DAMAGE_RECEIVED_EFFECT_NAME,
					new PercentDamageReceived(20, PERCENT_DAMAGE_RECEIVED_EFFECT));
		}
	}

	@Override
	public int cooldownTicks() {
		// This is the period of run()
		return 20;
	}
}
