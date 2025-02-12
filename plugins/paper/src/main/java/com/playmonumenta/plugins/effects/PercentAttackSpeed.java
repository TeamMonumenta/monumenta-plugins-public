package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public class PercentAttackSpeed extends Effect {
	public static final String GENERIC_NAME = "PercentAttackSpeed";
	public static final String effectID = "PercentAttackSpeed";

	private final double mAmount;
	private final String mModifierName;

	public PercentAttackSpeed(final int duration, final double amount, final String modifierName) {
		super(duration, effectID);
		mAmount = amount;
		mModifierName = modifierName;
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public void entityGainEffect(final Entity entity) {
		if (entity instanceof Attributable attributable) {
			EntityUtils.replaceAttribute(attributable, Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(mModifierName, mAmount, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
	}

	@Override
	public void entityLoseEffect(final Entity entity) {
		if (entity instanceof Attributable attributable) {
			EntityUtils.removeAttribute(attributable, Attribute.GENERIC_ATTACK_SPEED, mModifierName);
		}
	}

	@Override
	public boolean isBuff() {
		return mAmount > 0;
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount).append(Component.text(" " + getDisplayedName()));
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Attack Speed";
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

	public static PercentAttackSpeed deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();
		String modName = object.get("modifierName").getAsString();

		return new PercentAttackSpeed(duration, amount, modName);
	}

	@Override
	public String toString() {
		return String.format("PercentAttackSpeed duration:%d modifier:%s amount:%f", this.getDuration(), mModifierName, mAmount);
	}
}
