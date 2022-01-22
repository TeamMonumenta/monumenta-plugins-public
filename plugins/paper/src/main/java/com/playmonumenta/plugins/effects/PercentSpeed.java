package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class PercentSpeed extends Effect {

	private final double mAmount;
	private final String mModifierName;

	private boolean mWasInNoMobilityZone = false;

	public PercentSpeed(int duration, double amount, String modifierName) {
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
		if (entity instanceof Attributable && (!(entity instanceof Player) || !ZoneUtils.hasZoneProperty(entity, ZoneProperty.NO_MOBILITY_ABILITIES))) {
			EntityUtils.addAttribute((Attributable) entity, Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier(mModifierName, mAmount, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof Attributable) {
			EntityUtils.removeAttribute((Attributable) entity, Attribute.GENERIC_MOVEMENT_SPEED, mModifierName);
		}
	}

	@Override
	public void entityTickEffect(Entity entity, boolean fourHertz, boolean twoHertz, boolean oneHertz) {
		if (entity instanceof Player) {
			boolean isInNoMobilityZone = ZoneUtils.hasZoneProperty(entity, ZoneProperty.NO_MOBILITY_ABILITIES);

			if (mWasInNoMobilityZone && !isInNoMobilityZone) {
				entityGainEffect(entity);
			} else if (!mWasInNoMobilityZone && isInNoMobilityZone) {
				entityLoseEffect(entity);
			}

			mWasInNoMobilityZone = isInNoMobilityZone;
		}
	}

	@Override
	public String toString() {
		return String.format("PercentSpeed duration:%d modifier:%s amount:%f", this.getDuration(), mModifierName, mAmount);
	}
}
