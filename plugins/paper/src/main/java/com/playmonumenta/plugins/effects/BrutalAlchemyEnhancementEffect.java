package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.alchemist.BrutalAlchemy;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.jetbrains.annotations.Nullable;

public class BrutalAlchemyEnhancementEffect extends CustomDamageOverTime {
	public BrutalAlchemyEnhancementEffect(int duration, double damage, int period, @Nullable Player player, @Nullable ClassAbility spell, Particle particle) {
		super(duration, damage, period, player, spell, particle);
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		for (LivingEntity mob : EntityUtils.getNearbyMobs(event.getEntity().getLocation(), BrutalAlchemy.BRUTAL_ALCHEMY_ENHANCEMENT_RANGE)) {
			if (!Plugin.getInstance().mEffectManager.hasEffect(mob, BrutalAlchemy.BRUTAL_ALCHEMY_DOT_EFFECT_NAME)) {
				Plugin.getInstance().mEffectManager.addEffect(mob, BrutalAlchemy.BRUTAL_ALCHEMY_DOT_EFFECT_NAME, new BrutalAlchemyEnhancementEffect(mDuration, mDamage, mPeriod, mPlayer, mSpell, mParticle));
			}
		}
	}
}
