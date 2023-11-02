package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public class SpellInspire extends Spell {

	private static final String PERCENT_SPEED_EFFECT_NAME = "InspirePercentSpeedEffect";
	private static final String PERCENT_DAMAGE_DEALT_EFFECT_NAME = "InspirePercentDamageDealtEffect";
	private static final String PERCENT_DAMAGE_RECEIVED_EFFECT_NAME = "InspirePercentDamageReceivedEffect";
	private static final double PERCENT_SPEED_EFFECT = 0.15;
	private static final double PERCENT_DAMAGE_DEALT_EFFECT = 0.15;
	private static final double PERCENT_DAMAGE_RECEIVED_EFFECT = -0.3;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mRange;

	public SpellInspire(Plugin plugin, LivingEntity boss, int range) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation().clone().add(0, 0.25, 0);
		new PPCircle(Particle.FIREWORKS_SPARK, loc, mRange)
			.ringMode(true)
			.count(30)
			.extra(0.01)
			.spawnAsEntityActive(mBoss);

		new PartialParticle(Particle.SPELL_INSTANT, mBoss.getEyeLocation(), mRange * mRange / 8, 0, 0, 0, 0.1).spawnAsEntityActive(mBoss);


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
