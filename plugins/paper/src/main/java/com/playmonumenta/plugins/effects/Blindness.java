package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Particle;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class Blindness extends ZeroArgumentEffect {
	public static final String effectID = "Blindness";
	public static final String attributeModifierName = "BlindnessFollowRange";

	public Blindness(int duration) {
		super(duration, effectID);
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof Attributable e && !EntityUtils.isCCImmuneMob(entity)) {
			EntityUtils.addAttribute(e, Attribute.GENERIC_FOLLOW_RANGE, new AttributeModifier(attributeModifierName, -1, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		} else {
			clearEffect();
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof Attributable e) {
			EntityUtils.removeAttribute(e, Attribute.GENERIC_FOLLOW_RANGE, attributeModifierName);
		}
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (entity instanceof LivingEntity le && !EntityUtils.isCCImmuneMob(le)) {
			new PartialParticle(Particle.FIREWORKS_SPARK, le.getEyeLocation()).count(4).delta(0.3).extra(0.04).spawnAsEnemyBuff();
		}
	}

	@Override
	public boolean isDebuff() {
		return true;
	}

	@Override
	public String toString() {
		return String.format("Blindness duration:%d", this.getDuration());
	}
}
