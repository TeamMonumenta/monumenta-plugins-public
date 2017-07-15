package pe.project.managers.potion;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.managers.potion.PotionManager.PotionID;
import pe.project.utils.PotionUtils.PotionInfo;

public class PlayerPotionInfo {
	//	Effect Type / Potion List
	public HashMap<PotionEffectType, PotionMap> mPotionInfo;
	
	public PlayerPotionInfo() {
		mPotionInfo = new HashMap<PotionEffectType, PotionMap>();
	}
	
	public void addPotionInfo(Player player, PotionID id, PotionInfo info) {
		PotionMap type = mPotionInfo.get(info.type);
		if (type != null) {
			type.addPotionMap(player, id, info);
		} else {
			PotionMap newMap = new PotionMap();
			newMap.addPotionMap(player, id, info);
			mPotionInfo.put(info.type, newMap);
		}
	}
	
	public void removePotionInfo(Player player, PotionID id, PotionEffectType type) {
		PotionMap potionMap = mPotionInfo.get(type);
		if (potionMap != null) {
			player.removePotionEffect(type);
			potionMap.removePotionMap(player, id);
		}
	}
	
	public void clearPotionIDType(Player player, PotionID id) {
		Iterator<Entry<PotionEffectType, PotionMap>> potionMapIter = mPotionInfo.entrySet().iterator();
		while (potionMapIter.hasNext()) {
			Entry<PotionEffectType, PotionMap> potionEntry = potionMapIter.next();
			potionEntry.getValue().clearPotionIDType(player, id);
		}
	}
	
	public void updatePotionStatus(Player player, int ticks) {
		Iterator<Entry<PotionEffectType, PotionMap>> potionMapIter = mPotionInfo.entrySet().iterator();
		while (potionMapIter.hasNext()) {
			Entry<PotionEffectType, PotionMap> potionEntry = potionMapIter.next();
			potionEntry.getValue().updatePotionStatus(player, ticks);
		}
	}
	
	public void applyBestPotionEffect(Player player) {
		Iterator<Entry<PotionEffectType, PotionMap>> potionMapIter = mPotionInfo.entrySet().iterator();
		while (potionMapIter.hasNext()) {
			Entry<PotionEffectType, PotionMap> potionEntry = potionMapIter.next();
			potionEntry.getValue().applyBestPotionEffect(player);
		}
	}
	
	JsonObject getAsJsonObject() {
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
	
	void loadFromJsonObject(JsonObject object) {
		Set<Entry<String, JsonElement>> potionInfo = object.entrySet();
		for (Entry<String, JsonElement> info : potionInfo) {
			PotionEffectType type = PotionEffectType.getByName(info.getKey());
			PotionMap map = new PotionMap();
			
			JsonElement mapElement = info.getValue();
			if (mapElement != null) {
				map.loadFromJsonObject(mapElement.getAsJsonObject(), type);
			
				mPotionInfo.put(type, map);
			}
		}
	}
}
