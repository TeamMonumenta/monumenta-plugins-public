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
import java.util.function.BiPredicate;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class PercentDamageDealt extends Effect {
	public static final String GENERIC_NAME = "PercentDamageDealt";
	public static final String effectID = "PercentDamageDealt";

	protected final double mAmount;
	protected final @Nullable EnumSet<DamageType> mAffectedDamageTypes;
	protected final int mPriority;
	private @Nullable BiPredicate<LivingEntity, LivingEntity> mPredicate = null;

	private PercentDamageDealt(int duration, double amount, @Nullable EnumSet<DamageType> affectedDamageTypes, int priority, @Nullable BiPredicate<LivingEntity, LivingEntity> predicate, String id) {
		super(duration, id);
		mAmount = amount;
		mAffectedDamageTypes = affectedDamageTypes;
		mPriority = priority;
		mPredicate = predicate;
	}

	public PercentDamageDealt(int duration, double amount, @Nullable EnumSet<DamageType> affectedDamageTypes, int priority, @Nullable BiPredicate<LivingEntity, LivingEntity> predicate) {
		this(duration, amount, affectedDamageTypes, priority, predicate, effectID);
	}

	public PercentDamageDealt(int duration, double amount) {
		this(duration, amount, null, 0, null);
	}

	public PercentDamageDealt(int duration, double amount, @Nullable EnumSet<DamageType> affectedDamageTypes) {
		this(duration, amount, affectedDamageTypes, 0, null);
	}

	// Only call this in PercentDamageDealtSingle
	protected PercentDamageDealt(int duration, double amount, @Nullable EnumSet<DamageEvent.DamageType> affectedDamageTypes, String effectIdentifier) {
		this(duration, amount, affectedDamageTypes, 0, null, effectIdentifier);
	}

	// This needs to trigger before any flat damage
	@Override
	public EffectPriority getPriority() {
		if (mPriority == 1) {
			return EffectPriority.NORMAL;
		} else if (mPriority == 2) {
			return EffectPriority.LATE;
		} else {
			return EffectPriority.EARLY;
		}
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

	public @Nullable EnumSet<DamageType> getAffectedDamageTypes() {
		return mAffectedDamageTypes;
	}

	@Override
	public void onDamage(LivingEntity entity, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}
		if (mPredicate != null && !mPredicate.test(entity, enemy)) {
			return;
		}
		if (mAffectedDamageTypes == null || mAffectedDamageTypes.contains(event.getType())) {
			event.setDamage(event.getDamage() * Math.max(0, 1 + mAmount));
		}
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		return StringUtils.doubleToColoredAndSignedPercentage(mAmount) + StringUtils.getDamageTypeString(mAffectedDamageTypes) + " Damage Dealt";
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

		object.addProperty("priority", mPriority);
		object.addProperty("hasPredicate", mPredicate != null);

		return object;
	}

	public static @Nullable PercentDamageDealt deserialize(JsonObject object, Plugin plugin) {
		if (object.has("hasPredicate") && object.get("hasPredicate").getAsBoolean()) {
			// Noper nope nope not dealing with this
			return null;
		}

		int duration = object.get("duration").getAsInt();
		double amount = object.get("amount").getAsDouble();
		int priority = object.get("priority").getAsInt();

		if (object.has("type")) {
			JsonArray damageTypes = object.getAsJsonArray("type");
			List<DamageType> damageTypeList = new ArrayList<>();
			for (JsonElement element : damageTypes) {
				String string = element.getAsString();
				damageTypeList.add(DamageEvent.DamageType.valueOf(string));
			}

			EnumSet<DamageEvent.DamageType> damageTypeSet = EnumSet.copyOf(damageTypeList);
			return new PercentDamageDealt(duration, amount, damageTypeSet, priority, null);
		} else {
			return new PercentDamageDealt(duration, amount, null, priority, null);
		}
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
		return String.format("PercentDamageDealt duration:%d types:%s amount:%f", this.getDuration(), types, mAmount);
	}
}
