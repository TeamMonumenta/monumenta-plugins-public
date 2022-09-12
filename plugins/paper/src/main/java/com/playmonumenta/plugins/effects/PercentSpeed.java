package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PercentSpeed extends Effect {
	public static final String effectID = "PercentSpeed";
	public static final String GENERIC_NAME = "PercentSpeed";

	private final double mAmount;
	private final String mModifierName;

	private boolean mWasInNoMobilityZone = false;

	public PercentSpeed(int duration, double amount, String modifierName) {
		super(duration, effectID);
		mAmount = amount;
		mModifierName = modifierName;
	}

	public boolean isSlow() {
		return mAmount < 0;
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public boolean isDebuff() {
		return mAmount < 0;
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof Attributable attributable && (!(entity instanceof Player) || !ZoneUtils.hasZoneProperty(entity, ZoneProperty.NO_MOBILITY_ABILITIES))) {
			EntityUtils.addAttribute(attributable, Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier(mModifierName, mAmount, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof Attributable attributable) {
			EntityUtils.removeAttribute(attributable, Attribute.GENERIC_MOVEMENT_SPEED, mModifierName);
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
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("amount", mAmount);
		object.addProperty("modifierName", mModifierName);

		return object;
	}

	public static PercentSpeed deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();
		String modifierName = object.get("modifierName").getAsString();

		return new PercentSpeed(duration, amount, modifierName);
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount) + " Speed";
	}

	@Override
	public String toString() {
		return String.format("PercentSpeed duration:%d modifier:%s amount:%f", this.getDuration(), mModifierName, mAmount);
	}
}
