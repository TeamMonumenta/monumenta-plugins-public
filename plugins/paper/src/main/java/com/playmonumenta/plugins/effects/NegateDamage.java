package com.playmonumenta.plugins.effects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

public class NegateDamage extends Effect {
	public static final String effectID = "NegateDamage";
	private int mCount;
	private final @Nullable EnumSet<DamageEvent.DamageType> mAffectedTypes;
	private final @Nullable PartialParticle mParticleData;

	public NegateDamage(int duration, int count) {
		this(duration, count, null);
	}

	public NegateDamage(int duration, int count, @Nullable EnumSet<DamageEvent.DamageType> affectedTypes) {
		this(duration, count, affectedTypes, null);
	}

	public NegateDamage(int duration, int count, @Nullable EnumSet<DamageEvent.DamageType> affectedTypes, @Nullable PartialParticle particleData) {
		super(duration, effectID);
		mCount = count;
		mAffectedTypes = affectedTypes;
		mParticleData = particleData;
	}

	@Override
	public void onHurt(LivingEntity entity, DamageEvent event) {
		if (mCount > 0 && (mAffectedTypes == null || mAffectedTypes.contains(event.getType())) && !event.isCancelled() && !event.isBlockedByShield()) {
			if (event.getSource() != null && entity instanceof Player player) {
				ItemStatUtils.applyBlockedOnHitItemStats(Plugin.getInstance(), player);
			}
			event.setCancelled(true);
			World world = entity.getWorld();
			Location loc = entity.getLocation();
			world.playSound(loc, Sound.ITEM_SHIELD_BLOCK, 1, 1.2f);
			if (mParticleData != null) {
				mParticleData.spawnAsPlayerActive((Player) entity);
			}
			mCount--;
		}
	}

	@Override
	public JsonObject serialize() {
		JsonObject object = new JsonObject();
		object.addProperty("effectID", mEffectID);
		object.addProperty("duration", mDuration);
		object.addProperty("count", mCount);

		if (mAffectedTypes != null) {
			JsonArray jsonArray = new JsonArray();
			for (DamageEvent.DamageType damageType : mAffectedTypes) {
				jsonArray.add(damageType.name());
			}
			object.add("type", jsonArray);
		}

		return object;
	}

	public static NegateDamage deserialize(JsonObject object, Plugin plugin) {
		int duration = object.get("duration").getAsInt();
		int count = object.get("count").getAsInt();

		if (object.has("type")) {
			JsonArray damageTypes = object.getAsJsonArray("type");
			List<DamageEvent.DamageType> damageTypeList = new ArrayList<>();
			for (JsonElement element : damageTypes) {
				String string = element.getAsString();
				damageTypeList.add(DamageEvent.DamageType.valueOf(string));
			}

			EnumSet<DamageEvent.DamageType> damageTypeSet = EnumSet.copyOf(damageTypeList);
			return new NegateDamage(duration, count, damageTypeSet);
		} else {
			return new NegateDamage(duration, count);
		}
	}

	@Override
	public double getMagnitude() {
		return mCount;
	}

	@Override
	public boolean isBuff() {
		return true;
	}

	@Override
	public @Nullable String getSpecificDisplay() {
		if (mCount <= 0) {
			return null;
		}
		return "+" + mCount + StringUtils.getDamageTypeString(mAffectedTypes) + " Damage Blocked";
	}

	@Override
	public String toString() {
		String types = "any";
		if (mAffectedTypes != null) {
			types = "";
			for (DamageEvent.DamageType type : mAffectedTypes) {
				if (!types.isEmpty()) {
					types += ",";
				}
				types += type.name();
			}
		}
		return String.format("NegateDamage duration:%d types:%s count:%d", this.getDuration(), types, mCount);
	}
}
