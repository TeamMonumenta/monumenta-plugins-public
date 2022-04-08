package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PercentAttackSpeed extends Effect {
	public static final String GENERIC_NAME = "PercentAttackSpeed";

	private final double mAmount;
	private final String mModifierName;

	public PercentAttackSpeed(int duration, double amount, String modifierName) {
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
		if (entity instanceof Attributable attributable) {
			EntityUtils.addAttribute(attributable, Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(mModifierName, mAmount, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof Attributable attributable) {
			EntityUtils.removeAttribute(attributable, Attribute.GENERIC_ATTACK_SPEED, mModifierName);
		}
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount) + " Attack Speed";
	}

	@Override
	public String toString() {
		return String.format("PercentAttackSpeed duration:%d modifier:%s amount:%f", this.getDuration(), mModifierName, mAmount);
	}
}
