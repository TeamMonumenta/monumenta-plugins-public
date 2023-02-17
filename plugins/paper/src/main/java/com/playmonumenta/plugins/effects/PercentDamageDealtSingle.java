package com.playmonumenta.plugins.effects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class PercentDamageDealtSingle extends PercentDamageDealt {
	public static final String effectID = "PercentDamageDealtSingle";

	private boolean mHasDoneDamage;

	public PercentDamageDealtSingle(int duration, double amount) {
		this(duration, amount, null);
	}

	public PercentDamageDealtSingle(int duration, double amount, @Nullable EnumSet<DamageEvent.DamageType> affectedDamageTypes) {
		super(duration, amount, affectedDamageTypes, effectID);
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
		if (!mHasDoneDamage && (mAffectedDamageTypes == null || mAffectedDamageTypes.contains(event.getType()))) {
			mHasDoneDamage = true;
			event.setDamage(event.getDamage() * Math.max(0, 1 + mAmount));
		}
	}

	@Override
	public boolean isBuff() {
		return mAmount > 0;
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		if (mHasDoneDamage) {
			return null;
		}
		return super.getSpecificDisplay();
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("amount", mAmount);

		if (mAffectedDamageTypes != null) {
			JsonArray jsonArray = new JsonArray();
			for (DamageEvent.DamageType damageType : mAffectedDamageTypes) {
				jsonArray.add(damageType.name());
			}
			object.add("type", jsonArray);
		}

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
			if (object.has("type")) {
				JsonArray damageTypes = object.getAsJsonArray("type");
				List<DamageEvent.DamageType> damageTypeList = new ArrayList<>();
				for (JsonElement element : damageTypes) {
					String string = element.getAsString();
					damageTypeList.add(DamageEvent.DamageType.valueOf(string));
				}

				EnumSet<DamageEvent.DamageType> damageTypeSet = EnumSet.copyOf(damageTypeList);
				return new PercentDamageDealtSingle(duration, amount, damageTypeSet);
			} else {
				return new PercentDamageDealtSingle(duration, amount, null);
			}
		}
	}

	@Override public String toString() {
		return String.format("PercentDamageDealtSingle duration:%d amount:%f", this.getDuration(), mAmount);
	}
}
