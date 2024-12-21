package com.playmonumenta.plugins.effects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class DamageImmunity extends Effect {
	public static final String effectID = "DamageImmunity";

	private final EnumSet<DamageEvent.DamageType> mAffectedDamageTypes;

	public DamageImmunity(int duration, EnumSet<DamageEvent.DamageType> affectedDamageTypes) {
		super(duration, effectID);
		mAffectedDamageTypes = affectedDamageTypes;
	}

	@Override
	public void onHurt(LivingEntity entity, DamageEvent event) {
		if (mAffectedDamageTypes.contains(event.getType())) {
			event.setCancelled(true);
		}
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);

		if (mAffectedDamageTypes != null) {
			JsonArray jsonArray = new JsonArray();
			for (DamageEvent.DamageType damageType : mAffectedDamageTypes) {
				jsonArray.add(damageType.name());
			}
			object.add("type", jsonArray);
		}

		return object;
	}

	public static DamageImmunity deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();

		if (object.has("type")) {
			JsonArray damageTypes = object.getAsJsonArray("type");
			List<DamageEvent.DamageType> damageTypeList = new ArrayList<>();
			for (JsonElement element : damageTypes) {
				String string = element.getAsString();
				damageTypeList.add(DamageEvent.DamageType.valueOf(string));
			}

			EnumSet<DamageEvent.DamageType> damageTypeSet = EnumSet.copyOf(damageTypeList);
			return new DamageImmunity(duration, damageTypeSet);
		} else {
			return new DamageImmunity(duration, EnumSet.noneOf(DamageEvent.DamageType.class));
		}
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public @Nullable String getDisplayedName() {
		String typeString = StringUtils.getDamageTypeString(mAffectedDamageTypes, false, "Damage");
		return typeString + " Immunity";
	}

	@Override
	public String toString() {
		return String.format("DamageImmunity duration:%d types:%s", getDuration(), mAffectedDamageTypes);
	}

}
