package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class PercentHealthBoost extends Effect {
	public static final String effectID = "PercentHealthBoost";
	public static final String GENERIC_NAME = "PercentHealthBoost";

	private final double mAmount;
	private final String mModifierName;

	public PercentHealthBoost(int duration, double amount, String modifierName) {
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
		if (entity instanceof LivingEntity le) {
			if (isDebuff() && le.getHealth() != 0) {
				double maxHealth = EntityUtils.getMaxHealth(le);
				double newHealth = maxHealth + (maxHealth * mAmount);
				newHealth = Math.min(newHealth, le.getHealth());
				le.setHealth(newHealth);
			}
			EntityUtils.replaceAttribute(le, Attribute.GENERIC_MAX_HEALTH, new AttributeModifier(mModifierName, mAmount, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof LivingEntity le) {
			EntityUtils.removeAttribute(le, Attribute.GENERIC_MAX_HEALTH, mModifierName);
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

	public static PercentHealthBoost deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();
		String modName = object.get("modifierName").getAsString();

		return new PercentHealthBoost(duration, amount, modName);
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount).append(Component.text(" " + getDisplayedName()));
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Max Health";
	}

	@Override
	public String toString() {
		return String.format("PercentHealthBoost duration:%d modifier:%s amount:%f", this.getDuration(), mModifierName, mAmount);
	}
}
