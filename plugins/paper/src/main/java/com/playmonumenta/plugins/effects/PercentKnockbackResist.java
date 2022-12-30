package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class PercentKnockbackResist extends Effect {
	public static final String effectID = "PercentKnockbackResist";
	public static final String GENERIC_NAME = "PercentKnockbackResist";

	private final double mAmount;
	private final String mModifierName;

	public PercentKnockbackResist(int duration, double amount, String modifierName) {
		super(duration, effectID);
		mAmount = amount;
		mModifierName = modifierName;
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
	public boolean isBuff() {
		return mAmount > 0;
	}

	@Override
	public void entityGainEffect(Entity entity) {
		if (entity instanceof Attributable attributable) {
			EntityUtils.addAttribute(attributable, Attribute.GENERIC_KNOCKBACK_RESISTANCE, new AttributeModifier(mModifierName, mAmount, AttributeModifier.Operation.ADD_NUMBER));
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof Attributable attributable) {
			EntityUtils.removeAttribute(attributable, Attribute.GENERIC_KNOCKBACK_RESISTANCE, mModifierName);
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

	public static PercentKnockbackResist deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();
		String modName = object.get("modifierName").getAsString();

		return new PercentKnockbackResist(duration, amount, modName);
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return StringUtils.to2DP(mAmount * 10) + " Knockback Resistance";
	}

	@Override
	public String toString() {
		return String.format("PercentKnockbackResist duration:%d modifier:%s amount:%f", this.getDuration(), mModifierName, mAmount);
	}
}
