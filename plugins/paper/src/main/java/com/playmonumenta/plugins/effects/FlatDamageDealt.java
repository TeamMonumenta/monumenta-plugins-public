package com.playmonumenta.plugins.effects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.Nullable;

public class FlatDamageDealt extends Effect {
	public static final String effectID = "FlatDamageDealt";

	private final double mAmount;
	private final @Nullable EnumSet<DamageType> mAffectedDamageTypes;

	public FlatDamageDealt(int duration, double amount, @Nullable EnumSet<DamageType> affectedDamageTypes) {
		super(duration, effectID);
		mAmount = amount;
		mAffectedDamageTypes = affectedDamageTypes;
	}

	public FlatDamageDealt(int duration, double amount) {
		this(duration, amount, null);
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}
		if (mAffectedDamageTypes == null || mAffectedDamageTypes.contains(event.getType())) {
			event.setDamage(event.getDamage() + mAmount);
		}
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("amount", mAmount);

		if (mAffectedDamageTypes != null) {
			JsonArray jsonArray = new JsonArray();
			for (DamageType damageType : mAffectedDamageTypes) {
				jsonArray.add(damageType.name());
			}
			object.add("type", jsonArray);
		}

		return object;
	}

	public static FlatDamageDealt deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();

		if (object.has("type")) {
			JsonArray damageTypes = object.getAsJsonArray("type");
			List<DamageType> damageTypeList = new ArrayList<>();
			for (JsonElement element : damageTypes) {
				String string = element.getAsString();
				damageTypeList.add(DamageEvent.DamageType.valueOf(string));
			}

			EnumSet<DamageEvent.DamageType> damageTypeSet = EnumSet.copyOf(damageTypeList);
			return new FlatDamageDealt(duration, amount, damageTypeSet);
		} else {
			return new FlatDamageDealt(duration, amount);
		}
	}

	@Override
	public boolean isBuff() {
		return mAmount > 0;
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return "+" + StringUtils.to2DP(mAmount) + StringUtils.getDamageTypeString(mAffectedDamageTypes) + " Flat Damage Dealt";
	}

	@Override
	public String toString() {
		String types = "any";
		if (mAffectedDamageTypes != null) {
			types = "";
			for (DamageType type : mAffectedDamageTypes) {
				if (!types.isEmpty()) {
					types += ",";
				}
				types += type.name();
			}
		}
		return String.format("FlatDamageDealt duration:%d types:%s amount:%f", this.getDuration(), types, mAmount);
	}
}
