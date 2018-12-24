package com.playmonumenta.plugins.managers.potion;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.PotionUtils.PotionInfo;

public class PlayerPotionInfo {
	//  Effect Type / Potion List
	private final HashMap<PotionEffectType, PotionMap> mPotionInfo = new HashMap<PotionEffectType, PotionMap>();

	public void addPotionInfo(Player player, PotionID id, PotionInfo info) {
		PotionMap type = mPotionInfo.get(info.type);
		if (type != null) {
			type.addPotionMap(player, id, info);
		} else {
			PotionMap newMap = new PotionMap(info.type);
			newMap.addPotionMap(player, id, info);
			mPotionInfo.put(info.type, newMap);
		}
	}

	public void removePotionInfo(Player player, PotionID id, PotionEffectType type) {
		PotionMap potionMap = mPotionInfo.get(type);
		if (potionMap != null) {
			potionMap.removePotionMap(player, id);
		}
		/*
		 * If we are removing all effects, make really sure to remove even
		 * non-potion-manager-tracked effects
		 */
		if (id == PotionID.ALL) {
			player.removePotionEffect(type);
		}
	}

	public void clearPotionIDType(Player player, PotionID id) {
		Iterator<Entry<PotionEffectType, PotionMap>> potionMapIter = mPotionInfo.entrySet().iterator();
		while (potionMapIter.hasNext()) {
			Entry<PotionEffectType, PotionMap> potionEntry = potionMapIter.next();
			potionEntry.getValue().removePotionMap(player, id);
		}
	}

	public void clearPotionEffectType(Player player, PotionEffectType type) {
		PotionMap map = mPotionInfo.get(type);
		if (map != null) {
			map.removePotionMap(player, PotionID.ALL);
		}
	}

	public void updatePotionStatus(Player player, int ticks) {
		Iterator<Entry<PotionEffectType, PotionMap>> potionMapIter = mPotionInfo.entrySet().iterator();
		while (potionMapIter.hasNext()) {
			Entry<PotionEffectType, PotionMap> potionEntry = potionMapIter.next();
			potionEntry.getValue().updatePotionStatus(player, ticks);
		}
	}

	protected JsonObject getAsJsonObject() {
		JsonObject playerPotionInfoObject = new JsonObject();

		Iterator<Entry<PotionEffectType, PotionMap>> potionMapIter = mPotionInfo.entrySet().iterator();
		while (potionMapIter.hasNext()) {
			Entry<PotionEffectType, PotionMap> potionEntry = potionMapIter.next();

			JsonElement element = potionEntry.getValue().getAsJsonObject();
			if (element != null) {
				playerPotionInfoObject.add(potionEntry.getKey().getName(), element);
			}
		}

		return playerPotionInfoObject;
	}

	protected void loadFromJsonObject(JsonObject object) throws Exception {
		Set<Entry<String, JsonElement>> potionInfo = object.entrySet();
		for (Entry<String, JsonElement> info : potionInfo) {
			PotionEffectType type = PotionEffectType.getByName(info.getKey());
			PotionMap map = new PotionMap(type);

			JsonElement mapElement = info.getValue();
			if (mapElement != null) {
				map.loadFromJsonObject(mapElement.getAsJsonObject());

				mPotionInfo.put(type, map);
			}
		}
	}
}
