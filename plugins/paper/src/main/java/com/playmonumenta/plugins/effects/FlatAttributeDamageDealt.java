package com.playmonumenta.plugins.effects;

import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;

import com.playmonumenta.plugins.utils.EntityUtils;

public class FlatAttributeDamageDealt extends Effect {

	private final double mAmount;
	private final String mModifierName;

	public FlatAttributeDamageDealt(int duration, double amount, String modifierName) {
		super(duration);
		mAmount = amount;
		mModifierName = modifierName;
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof Attributable) {
			EntityUtils.addAttribute((Attributable) entity, Attribute.GENERIC_ATTACK_DAMAGE, new AttributeModifier(mModifierName, mAmount, AttributeModifier.Operation.ADD_NUMBER));
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof Attributable) {
			EntityUtils.removeAttribute((Attributable) entity, Attribute.GENERIC_ATTACK_DAMAGE, mModifierName);
		}
	}

	@Override
	public String toString() {
		return String.format("FlatAttributeDamage duration:%d modifier:%s amount:%f", this.getDuration(), mModifierName, mAmount);
	}
}
