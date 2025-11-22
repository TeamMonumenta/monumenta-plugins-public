package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import net.kyori.adventure.text.Component;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class PercentSpeed extends Effect {
	public static final String effectID = "PercentSpeed";
	public static final String GENERIC_NAME = "PercentSpeed";

	private double mAmount;
	private final String mModifierName;

	private boolean mWasInNoMobilityZone = false;

	public PercentSpeed(final int duration, final double amount, final String modifierName) {
		super(duration, effectID);
		mAmount = Math.max(-1, amount);
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
	public boolean isBuff() {
		return mAmount > 0;
	}

	@Override
	public void entityGainEffect(final Entity entity) {
		if (entity instanceof final Attributable attributable && (!(entity instanceof Player) ||
			!ZoneUtils.hasZoneProperty(entity, ZoneProperty.NO_MOBILITY_ABILITIES))) {
			EntityUtils.replaceAttribute(attributable, Attribute.GENERIC_MOVEMENT_SPEED,
				new AttributeModifier(mModifierName, mAmount, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
	}

	@Override
	public void entityLoseEffect(final Entity entity) {
		if (entity instanceof final Attributable attributable) {
			EntityUtils.removeAttribute(attributable, Attribute.GENERIC_MOVEMENT_SPEED, mModifierName);
		}
	}

	@Override
	public void entityTickEffect(final Entity entity, final boolean fourHertz, final boolean twoHertz,
	                             final boolean oneHertz) {
		if (entity instanceof Player) {
			final boolean isInNoMobilityZone = ZoneUtils.hasZoneProperty(entity, ZoneProperty.NO_MOBILITY_ABILITIES);

			if (mWasInNoMobilityZone && !isInNoMobilityZone) {
				entityGainEffect(entity);
			} else if (!mWasInNoMobilityZone && isInNoMobilityZone) {
				entityLoseEffect(entity);
			}

			mWasInNoMobilityZone = isInNoMobilityZone;
		}
	}

	public void setAmount(final double speed, final Entity entity) {
		mAmount = speed;
		if (mUsed) {
			entityGainEffect(entity);
		}
	}

	@Override
	public JsonObject serialize() {
		final JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("amount", mAmount);
		object.addProperty("modifierName", mModifierName);

		return object;
	}

	public static PercentSpeed deserialize(final JsonObject object, final Plugin plugin) {
		final int duration = object.get("duration").getAsInt();
		final double amount = object.get("amount").getAsDouble();
		final String modifierName = object.get("modifierName").getAsString();

		return new PercentSpeed(duration, amount, modifierName);
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount)
			.append(Component.text(" " + getDisplayedName()));
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Speed";
	}

	@Override
	public String toString() {
		return String.format("PercentSpeed duration:%d modifier:%s amount:%f", this.getDuration(), mModifierName, mAmount);
	}
}
