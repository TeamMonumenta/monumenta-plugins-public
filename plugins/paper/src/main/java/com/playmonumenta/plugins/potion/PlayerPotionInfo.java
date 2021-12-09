package com.playmonumenta.plugins.potion;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.PotionUtils.PotionInfo;

public class PlayerPotionInfo {
	//  Effect Type / Potion List
	private final HashMap<PotionEffectType, PotionMap> mPotionInfo = new HashMap<PotionEffectType, PotionMap>();

	protected void addPotionInfo(Player player, PotionID id, PotionInfo info) {
		PotionMap type = mPotionInfo.get(info.mType);
		if (type != null) {
			type.addPotionMap(player, id, info);
		} else {
			PotionMap newMap = new PotionMap(info.mType);
			newMap.addPotionMap(player, id, info);
			mPotionInfo.put(info.mType, newMap);
		}
	}

	protected void removePotionInfo(Player player, PotionID id, PotionEffectType type) {
		PotionMap potionMap = mPotionInfo.get(type);
		if (potionMap != null) {
			potionMap.removePotionMap(player, id);
		}
	}

	protected void clearPotionIDType(Player player, PotionID id) {
		Iterator<Entry<PotionEffectType, PotionMap>> potionMapIter = mPotionInfo.entrySet().iterator();
		while (potionMapIter.hasNext()) {
			Entry<PotionEffectType, PotionMap> potionEntry = potionMapIter.next();
			potionEntry.getValue().removePotionMap(player, id);
		}
	}

	protected void clearPotionEffectType(Player player, PotionEffectType type) {
		mPotionInfo.remove(type);
	}

	protected void updatePotionStatus(Player player, int ticks) {
		Iterator<Entry<PotionEffectType, PotionMap>> potionMapIter = mPotionInfo.entrySet().iterator();
		while (potionMapIter.hasNext()) {
			Entry<PotionEffectType, PotionMap> potionEntry = potionMapIter.next();
			potionEntry.getValue().updatePotionStatus(player, ticks);
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
			PotionMap map = new PotionMap(type);

			map.loadFromJsonObject(info.getValue().getAsJsonObject());

			mPotionInfo.put(type, map);
		}
	}
}
