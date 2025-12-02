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

public final class PercentDamageDealtMultiple extends PercentDamageDealt {
	public static final String effectID = "PercentDamageDealtMultiple";

	private int mTimesDamageDealt;
	private final int mMaxDamageInstances;
	private final boolean mMultiplicative;
	private final @Nullable Consumer<DamageEvent> mOnUse;

	public PercentDamageDealtMultiple(final int duration, final double amount, final int maxDamageInstances) {
		this(duration, amount, maxDamageInstances, 0);
	}

	public PercentDamageDealtMultiple(final int duration, final double amount, final int maxDamageInstances,
									  final int startingTimesDamageDealt) {
		this(duration, amount, maxDamageInstances, startingTimesDamageDealt, null, false);
	}

	public PercentDamageDealtMultiple(final int duration, final double amount, final int maxDamageInstances,
									  final int startingTimesDamageDealt, final @Nullable EnumSet<DamageType> affectedDamageTypes,
									  final boolean multiplicative) {
		this(duration, amount, maxDamageInstances, startingTimesDamageDealt, affectedDamageTypes, multiplicative, null);
	}

	public PercentDamageDealtMultiple(final int duration, final double amount, final int maxDamageInstances,
									  final int startingTimesDamageDealt, final @Nullable EnumSet<DamageType> affectedDamageTypes,
									  final boolean multiplicative, final @Nullable Consumer<DamageEvent> onUse) {
		super(duration, amount, affectedDamageTypes, effectID);
		mMaxDamageInstances = maxDamageInstances;
		mTimesDamageDealt = startingTimesDamageDealt;
		mMultiplicative = multiplicative;
		mOnUse = onUse;
	}

	private boolean isExhausted() {
		return mTimesDamageDealt >= mMaxDamageInstances;
	}

	private void registerDamageDealt() {
		if (isExhausted()) {
			return;
		}
		mTimesDamageDealt++;
	}

	@Override
	public double getMagnitude() {
		return (isExhausted() ? 0 : Math.abs(mAmount));
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		if (isExhausted()) {
			return;
		}
		if (event.getType() == DamageType.TRUE) {
			return;
		}
		if (mAffectedDamageTypes == null || mAffectedDamageTypes.contains(event.getType())
			|| (mAffectedDamageTypes.contains(DamageType.PROJECTILE_SKILL) && AbilityUtils.hasSpecialProjSkillScaling(event.getAbility()))) {
			registerDamageDealt();
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
		if (isExhausted()) {
			return null;
		}
		return super.getSpecificDisplay();
	}

	@Override
	public @Nullable String getDisplayedName() {
		if (isExhausted()) {
			return null;
		}
		return super.getDisplayedName() + " (%s/%s)".formatted(mTimesDamageDealt, mMaxDamageInstances);
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

		object.addProperty("maxDamageInstances", mMaxDamageInstances);
		object.addProperty("timesDamageDealt", mTimesDamageDealt);
		return object;
	}

	public static @Nullable PercentDamageDealtMultiple deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();
		int maxDamageInstances = object.get("maxDamageInstances").getAsInt();
		int timesDamageDealt = object.get("timesDamageDealt").getAsInt();

		if (timesDamageDealt >= maxDamageInstances) {
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
				return (PercentDamageDealtMultiple) new PercentDamageDealtMultiple(duration, amount, maxDamageInstances).damageTypes(damageTypeSet);
			} else {
				return new PercentDamageDealtMultiple(duration, amount, maxDamageInstances);
			}
		}
	}

	@Override
	public String toString() {
		return String.format("PercentDamageDealtSingle duration:%d amount:%f", this.getDuration(), mAmount);
	}
}
