package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CustomAbsorption extends Effect {
	public static final String effectID = "CustomAbsorption";
	public static final String GENERIC_NAME = "CustomAbsorption";

	private final double mAmount;
	private final String mModifierName;

	private double mCurrent = 0;

	public CustomAbsorption(int duration, double amount, String modifierName) {
		super(duration, effectID);
		mAmount = amount;
		mModifierName = modifierName;
		mDuration = duration;
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
		if (entity instanceof Player player) {
			AbsorptionUtils.addAbsorption(player, player.getMaxHealth() * mAmount, player.getMaxHealth() * mAmount, mDuration);
			mCurrent = player.getMaxHealth() * mAmount;
		}
	}

	@Override
	public void entityLoseEffect(Entity entity) {
		if (entity instanceof Player player) {
			AbsorptionUtils.subtractAbsorption(player, mCurrent);
		}
	}

	@Override
	public void onHurt(LivingEntity entity, DamageEvent event) {
		mCurrent = Math.max(mCurrent - event.getFinalDamage(true), 0);
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

	public static CustomAbsorption deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();
		String modName = object.get("modifierName").getAsString();

		return new CustomAbsorption(duration, amount, modName);
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return StringUtils.to2DP(mAmount * 100) + "% Absorption Health";
	}

	@Override
	public String toString() {
		return String.format("CustomAbsorption duration:%d modifier:%s amount:%f", this.getDuration(), mModifierName, mAmount);
	}
}
