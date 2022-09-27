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
import org.checkerframework.checker.nullness.qual.Nullable;

// Reduce incoming damage based on charges
public class CourageEffect extends Effect {
	public static final String effectID = "CourageEffect";

	private final @Nullable EnumSet<DamageEvent.DamageType> mAffectedDamageTypes;

	private final double mAmount;
	private int mCharges;
	private int mTickWhenHit = 0;

	public CourageEffect(int duration, double amount, int charges, @Nullable EnumSet<DamageEvent.DamageType> affectedDamageTypes) {
		super(duration, effectID);
		mAmount = amount;
		mAffectedDamageTypes = affectedDamageTypes;
		mCharges = charges;
	}

	public CourageEffect(int duration, double amount, int charges) {
		this(duration, amount, charges, null);
	}

	@Override
	public double getMagnitude() {
		return mAmount;
	}

	@Override
	public void onHurt(LivingEntity entity, DamageEvent event) {
		if (mAffectedDamageTypes == null || mAffectedDamageTypes.contains(event.getType())) {
			double amount = mAmount;

			int currentTick = entity.getTicksLived();

			// Check if the last hit was larger than 0.5 seconds - meaning we need to expend a charge for this hit.
			if (currentTick - mTickWhenHit > 10) {
				mCharges -= 1;

				event.setDamage(event.getDamage() * (1 - amount));
				mTickWhenHit = currentTick;

				if (mCharges == 0) {
					mDuration = 10; // We need the effect to last 0.5 seconds for the last charge.
				}
			} else {
				// Therefore this should mean that the last hit is within the 0.5 seconds, so mitigate it
				// without spending charges
				event.setDamage(event.getDamage() * (1 - amount));
			}
		}
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("amount", mAmount);
		object.addProperty("charges", mCharges);

		if (mAffectedDamageTypes != null) {
			JsonArray damageTypes = new JsonArray();
			for (DamageEvent.DamageType type : mAffectedDamageTypes) {
				damageTypes.add(type.name());
			}
			object.add("type", damageTypes);
		}

		return object;
	}

	public static CourageEffect deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();
		int charges = object.get("charges").getAsInt();

		JsonArray damageTypes = object.getAsJsonArray("type");

		if (damageTypes != null) {
			List<DamageEvent.DamageType> damageTypeList = new ArrayList<>();
			for (JsonElement element : damageTypes) {
				String string = element.getAsString();
				damageTypeList.add(DamageEvent.DamageType.valueOf(string));
			}

			EnumSet<DamageEvent.DamageType> damageTypeSet = EnumSet.copyOf(damageTypeList);
			return new CourageEffect(duration, amount, charges, damageTypeSet);
		} else {
			return new CourageEffect(duration, amount, charges);
		}
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public String toString() {
		String types = "any";
		if (mAffectedDamageTypes != null) {
			types = "";
			for (DamageEvent.DamageType type : mAffectedDamageTypes) {
				if (!types.isEmpty()) {
					types += ",";
				}
				types += type.name();
			}
		}
		return String.format("LiquidCourageEffect duration:%d types:%s charges:%d amount:%f", this.getDuration(), types, mCharges, mAmount);
	}
}
