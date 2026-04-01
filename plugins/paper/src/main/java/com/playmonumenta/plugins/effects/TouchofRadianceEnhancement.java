package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.abilities.cleric.TouchofRadiance;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class TouchofRadianceEnhancement extends Effect {
	private final Plugin mPlugin;
	private final double mDamage;
	private final int mBlindDuration;
	private final int mFireDuration;
	private final Set<LivingEntity> mAffectedMobs = new HashSet<>();

	public TouchofRadianceEnhancement(Plugin plugin, double damage, int blindDuration, int fireDuration, int duration) {
		super(duration, TouchofRadiance.ENHANCEMENT_EFFECT_NAME);
		mPlugin = plugin;
		mDamage = damage;
		mBlindDuration = blindDuration;
		mFireDuration = fireDuration;
	}

	@Override
	public void entityGainEffect(Entity entity) {
		mAffectedMobs.clear();
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		if (Crusade.enemyTriggersAbilities(enemy) && (event.getType() == DamageEvent.DamageType.MELEE || event.getType() == DamageEvent.DamageType.PROJECTILE)) {
			if (mAffectedMobs.add(enemy)) {
				mPlugin.mEffectManager.addEffect(enemy, "TouchofRadianceEnhancementBlindness", new Blindness(mBlindDuration));
				EntityUtils.applyFire(mPlugin, mFireDuration, enemy, entity);
				DamageUtils.damage(entity, enemy, DamageEvent.DamageType.MAGIC, mDamage, ClassAbility.TOUCH_OF_RADIANCE, true);
			}
		}
	}

	@Override
	public String toString() {
		return String.format(TouchofRadiance.ENHANCEMENT_EFFECT_NAME + " duration:%d", this.getDuration());
	}
}
