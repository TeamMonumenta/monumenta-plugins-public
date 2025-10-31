package com.playmonumenta.plugins.effects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.AbilityUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public final class PercentDamageDealtSingle extends PercentDamageDealt {
	public static final String effectID = "PercentDamageDealtSingle";

	private boolean mHasDoneDamage;
	private final boolean mMultiplicative;
	private final @Nullable Consumer<DamageEvent> mOnUse;

	public PercentDamageDealtSingle(final int duration, final double amount) {
		this(duration, amount, null, false);
	}

	public PercentDamageDealtSingle(final int duration, final double amount,
	                                final @Nullable EnumSet<DamageType> affectedDamageTypes, final boolean multiplicative) {
		this(duration, amount, affectedDamageTypes, multiplicative, null);
	}

	public PercentDamageDealtSingle(final int duration, final double amount,
	                                final @Nullable EnumSet<DamageType> affectedDamageTypes, final boolean multiplicative,
	                                final @Nullable Consumer<DamageEvent> onUse) {
		super(duration, amount, affectedDamageTypes, effectID);
		mHasDoneDamage = false;
		mMultiplicative = multiplicative;
		mOnUse = onUse;
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
		if (event.getType() == DamageType.TRUE) {
			return;
		}
		if (mAffectedDamageTypes == null || mAffectedDamageTypes.contains(event.getType())
			|| (mAffectedDamageTypes.contains(DamageType.PROJECTILE_SKILL) && AbilityUtils.hasSpecialProjSkillScaling(event.getAbility()))) {
			mHasDoneDamage = true;
			if (mMultiplicative) {
				event.setFlatDamage(event.getFlatDamage() * (1 + mAmount));
			} else {
				event.updateDamageWithMultiplier(Math.max(0, 1 + mAmount));
			}

			if (mOnUse != null) {
				mOnUse.accept(event);
			}
		}
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
			for (DamageType damageType : mAffectedDamageTypes) {
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
				List<DamageType> damageTypeList = new ArrayList<>();
				for (JsonElement element : damageTypes) {
					String string = element.getAsString();
					damageTypeList.add(DamageType.valueOf(string));
				}

				EnumSet<DamageType> damageTypeSet = EnumSet.copyOf(damageTypeList);
				return (PercentDamageDealtSingle) new PercentDamageDealtSingle(duration, amount).damageTypes(damageTypeSet);
			} else {
				return new PercentDamageDealtSingle(duration, amount);
			}
		}
	}

	@Override
	public String toString() {
		return String.format("PercentDamageDealtSingle duration:%d amount:%f", this.getDuration(), mAmount);
	}
}
