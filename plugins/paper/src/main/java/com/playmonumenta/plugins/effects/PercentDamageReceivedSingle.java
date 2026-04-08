package com.playmonumenta.plugins.effects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class PercentDamageReceivedSingle extends Effect {
	public static final String effectID = "PercentDamageReceivedSingle";
	public static final String GENERIC_NAME = "PercentDamageReceivedSingle";

	protected final double mAmount;
	protected final @Nullable EnumSet<DamageType> mAffectedDamageTypes;
	private boolean mCleared = false;

	public PercentDamageReceivedSingle(final int duration, final double amount, final @Nullable EnumSet<DamageType> affectedDamageTypes) {
		super(duration, effectID);
		mAmount = amount;
		mAffectedDamageTypes = affectedDamageTypes;
	}

	public PercentDamageReceivedSingle(final int duration, final double amount) {
		this(duration, amount, null);
	}

	@Override
	public double getMagnitude() {
		return Math.abs(mAmount);
	}

	@Override
	public boolean isDebuff() {
		return mAmount > 0;
	}

	@Override
	public boolean isBuff() {
		return mAmount < 0;
	}

	public @Nullable EnumSet<DamageType> getAffectedDamageTypes() {
		return mAffectedDamageTypes;
	}

	@Override
	public void onHurt(final LivingEntity entity, final DamageEvent event) {
		if (event.getType() != DamageType.TRUE && (mAffectedDamageTypes == null || mAffectedDamageTypes.contains(event.getType()))) {
			double amount = mAmount;
			if (EntityUtils.isBoss(entity) && isDebuff()) {
				amount /= 2;
			}
			event.updateDamageWithMultiplier(1 + amount);
			mCleared = true;
			clearEffect();
		}
	}

	@Override
	public JsonObject serialize() {
		final JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("amount", mAmount);

		if (mAffectedDamageTypes != null) {
			final JsonArray jsonArray = new JsonArray();
			for (DamageType damageType : mAffectedDamageTypes) {
				jsonArray.add(damageType.name());
			}
			object.add("type", jsonArray);
		}

		return object;
	}

	public static @Nullable PercentDamageReceivedSingle deserialize(JsonObject object, Plugin plugin) {
		boolean hasTakenDamage = object.get("hasTakenDamage").getAsBoolean();
		if (hasTakenDamage) {
			return null;
		}

		final int duration = object.get("duration").getAsInt();
		final double amount = object.get("amount").getAsDouble();

		if (object.has("type")) {
			final JsonArray damageTypes = object.getAsJsonArray("type");
			final List<DamageType> damageTypeList = new ArrayList<>();
			for (final JsonElement element : damageTypes) {
				damageTypeList.add(DamageType.valueOf(element.getAsString()));
			}

			final EnumSet<DamageType> damageTypeSet = EnumSet.copyOf(damageTypeList);
			return new PercentDamageReceivedSingle(duration, amount, damageTypeSet);
		} else {
			return new PercentDamageReceivedSingle(duration, amount);
		}
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		if (mCleared) {
			return null;
		}

		return StringUtils.doubleToColoredAndSignedPercentage(-mAmount).append(
			getDisplayedName() != null ? Component.text(getDisplayedName()) : Component.empty()
		);
	}

	@Override
	public @Nullable String getDisplayedName() {
		if (mCleared) {
			return null;
		}

		return StringUtils.getDamageTypeString(mAffectedDamageTypes, false, null) + " " + "Resistance";
	}

	@Override
	public String toString() {
		StringBuilder types = new StringBuilder("any");
		if (mAffectedDamageTypes != null) {
			types = new StringBuilder();
			for (final DamageType type : mAffectedDamageTypes) {
				if (!types.isEmpty()) {
					types.append(",");
				}
				types.append(type.name());
			}
		}
		return String.format("PercentDamageReceived duration:%d types:%s amount:%f", this.getDuration(), types, mAmount);
	}
}
