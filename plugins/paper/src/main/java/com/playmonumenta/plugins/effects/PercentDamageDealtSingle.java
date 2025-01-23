package com.playmonumenta.plugins.effects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.AbilityUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class PercentDamageDealtSingle extends PercentDamageDealt {
	public static final String effectID = "PercentDamageDealtSingle";

	private boolean mHasDoneDamage;
	private final boolean mMultiplicative;

	public PercentDamageDealtSingle(int duration, double amount) {
		this(duration, amount, null, false);
	}

	public PercentDamageDealtSingle(int duration, double amount, @Nullable EnumSet<DamageEvent.DamageType> affectedDamageTypes) {
		this(duration, amount, affectedDamageTypes, false);
	}

	public PercentDamageDealtSingle(int duration, double amount, @Nullable EnumSet<DamageEvent.DamageType> affectedDamageTypes, boolean multiplicative) {
		super(duration, amount, affectedDamageTypes, effectID);
		mHasDoneDamage = false;
		mMultiplicative = multiplicative;
	}

	@Override
	public double getMagnitude() {
		return (mHasDoneDamage ? 0 : Math.abs(mAmount));
	}


	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		if (mHasDoneDamage) {
			return;
		}
		if (event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}
		if (mAffectedDamageTypes == null || mAffectedDamageTypes.contains(event.getType())
			|| (mAffectedDamageTypes.contains(DamageEvent.DamageType.PROJECTILE_SKILL) && AbilityUtils.hasSpecialProjSkillScaling(event.getAbility()))) {
			mHasDoneDamage = true;
			if (mMultiplicative) {
				event.setFlatDamage(event.getFlatDamage() * (1 + mAmount));
			} else {
				event.updateDamageWithMultiplier(Math.max(0, 1 + mAmount));
			}
		}
	}

	@Override
	public boolean isBuff() {
		return mAmount > 0;
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		if (mHasDoneDamage) {
			return null;
		}
		return super.getSpecificDisplay();
	}

	@Override
	public @Nullable String getDisplayedName() {
		if (mHasDoneDamage) {
			return null;
		}
		return super.getDisplayedName();
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

	@Override
	public String toString() {
		return String.format("PercentDamageDealtSingle duration:%d amount:%f", this.getDuration(), mAmount);
	}
}
