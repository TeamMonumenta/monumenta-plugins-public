package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;

/**
 * Effect to change an entity's movement speed, without it counting as a buff or debuff.
 */
public class BaseMovementSpeedModifyEffect extends Effect {
	public static final String effectID = "BaseMovementSpeedModifyEffect";
	public static final String GENERIC_NAME = "BaseMovementSpeedModifyEffect";
	public static final String MODIFIER_NAME = "BaseMovementSpeedModifyEffect";

	private final double mAmount;


	public BaseMovementSpeedModifyEffect(int duration, double amount) {
		super(duration, effectID);
		mAmount = Math.max(-1, amount);
	}

	public boolean isSlow() {
		return mAmount < 0;
	}

	@Override
	public double getMagnitude() {
		return mAmount;
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof Attributable attributable) {
			EntityUtils.replaceAttribute(attributable, Attribute.GENERIC_MOVEMENT_SPEED, new AttributeModifier(MODIFIER_NAME, mAmount, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof Attributable attributable) {
			EntityUtils.removeAttribute(attributable, Attribute.GENERIC_MOVEMENT_SPEED, MODIFIER_NAME);
		}
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("amount", mAmount);

		return object;
	}

	public static BaseMovementSpeedModifyEffect deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();

		return new BaseMovementSpeedModifyEffect(duration, amount);
	}

	@Override
	public String toString() {
		return String.format("BaseMovementSpeedModifyEffect duration:%d amount:%f", this.getDuration(), mAmount);
	}

}
