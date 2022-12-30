package com.playmonumenta.plugins.effects;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class PercentDamageDealtSingle extends PercentDamageDealt {
	public static final String effectID = "PercentDamageDealtSingle";

	private boolean mHasDoneDamage;

	public PercentDamageDealtSingle(int duration, double amount) {
		super(duration, amount, effectID);
		mHasDoneDamage = false;
	}

	@Override
	public double getMagnitude() {
		return (mHasDoneDamage ? 0 : Math.abs(mAmount));
	}


	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}
		if (!mHasDoneDamage) {
			mHasDoneDamage = true;
			event.setDamage(event.getDamage() * Math.max(0, 1 + mAmount));
		}
	}

	@Override
	public boolean isBuff() {
		return mAmount > 0;
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("amount", mAmount);

		object.addProperty("hasDoneDamage", mHasDoneDamage);
		return object;
	}

	public static @Nullable PercentDamageDealtSingle deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();
		boolean hasDoneDamage = object.get("hasDoneDamage").getAsBoolean();

		if (hasDoneDamage) {
			return null;
		} else {
			return new PercentDamageDealtSingle(duration, amount);
		}
	}

	@Override public String toString() {
		return String.format("PercentDamageDealtSingle duration:%d amount:%f", this.getDuration(), mAmount);
	}
}
