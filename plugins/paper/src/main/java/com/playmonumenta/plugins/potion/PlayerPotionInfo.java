package com.playmonumenta.plugins.potion;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.PotionUtils.PotionInfo;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.function.ToIntFunction;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class PlayerPotionInfo {
	//  Effect Type / Potion List
	private final HashMap<PotionEffectType, PotionMap> mPotionInfo = new HashMap<>();

	protected void addPotionInfo(Player player, PotionID id, PotionInfo info) {
		if (info.mType == null) {
			return;
		}
		mPotionInfo.computeIfAbsent(info.mType, PotionMap::new)
			.addPotion(player, id, info);
	}

	protected Collection<PotionMap> getAllPotionMaps() {
		return mPotionInfo.values();
	}

	protected void removePotionInfo(Player player, PotionID id, PotionEffectType type, int amplifier) {
		PotionMap potionMap = mPotionInfo.get(type);
		if (potionMap != null) {
			potionMap.removePotion(player, id, amplifier);
		}
	}

	protected void clearPotionInfo(Player player, PotionID id, PotionEffectType type) {
		PotionMap potionMap = mPotionInfo.get(type);
		if (potionMap != null) {
			potionMap.clearPotion(player, id);
		}
	}

	protected void clearPotionIDType(Player player, PotionID id) {
		for (Entry<PotionEffectType, PotionMap> potionEntry : mPotionInfo.entrySet()) {
			potionEntry.getValue().clearPotion(player, id);
		}
	}

	protected void clearPotionEffectType(PotionEffectType type) {
		mPotionInfo.remove(type);
	}

	protected void updatePotionStatus(Player player, int ticks) {
		for (Entry<PotionEffectType, PotionMap> potionEntry : mPotionInfo.entrySet()) {
			potionEntry.getValue().updatePotionStatus(player, ticks);
		}
	}

	public void modifyPotionDuration(Player player, ToIntFunction<PotionInfo> function) {
		for (PotionMap potionMap : mPotionInfo.values()) {
			potionMap.modifyPotionDuration(player, function);
		}
	}

	protected JsonObject getAsJsonObject(boolean includeAll) {
		JsonObject playerPotionInfoObject = new JsonObject();

		for (Entry<PotionEffectType, PotionMap> potionEntry : mPotionInfo.entrySet()) {
			JsonElement element = potionEntry.getValue().getAsJsonObject(includeAll);
			if (element != null) {
				playerPotionInfoObject.add(potionEntry.getKey().getName(), element);
			}
		}

		return playerPotionInfoObject;
	}

	protected void loadFromJsonObject(JsonObject object) throws Exception {
		for (Entry<String, JsonElement> info : object.entrySet()) {
			PotionEffectType type = PotionEffectType.getByName(info.getKey());
			if (type == null) {
				throw new Exception("Invalid potion type " + info.getKey());
			}
			PotionMap map = new PotionMap(type);

			map.loadFromJsonObject(info.getValue().getAsJsonObject());

			mPotionInfo.put(type, map);
		}
	}
}
