package pe.project.managers.potion;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.managers.potion.PotionManager.PotionID;
import pe.project.utils.PotionUtils.PotionInfo;

public class PotionMap {
//	ID / Potion info
	public HashMap<PotionID, Vector<PotionInfo>> mPotionMap;

	public PotionMap() {
		mPotionMap = new HashMap<PotionID, Vector<PotionInfo>>();
	}

	public void addPotionMap(Player player, PotionID id, PotionInfo newPotionInfo) {
		Vector<PotionInfo> trackedPotionInfo = mPotionMap.get(id);
		if (trackedPotionInfo == null) {
			trackedPotionInfo = new Vector<PotionInfo>();
		}

		trackedPotionInfo.add(newPotionInfo);
		mPotionMap.put(id, trackedPotionInfo);

		applyBestPotionEffect(player);
	}

	public void removePotionMap(Player player, PotionID id) {
		if (id != PotionID.ALL) {
			Vector<PotionInfo> trackedPotionInfo = mPotionMap.get(id);
			if (trackedPotionInfo != null) {
				for (PotionInfo info : trackedPotionInfo) {
					player.removePotionEffect(info.type);
				}

				mPotionMap.remove(id);
			}
		} else {
			mPotionMap.clear();
		}

		applyBestPotionEffect(player);
	}

	public void clearPotionIDType(Player player, PotionID id) {
		removePotionMap(player, id);
	}

	public void updatePotionStatus(Player player, int ticks) {
		//	First update the timers of all our tracked potion timers.
		boolean effectWoreOff = false;
		Iterator<Entry<PotionID, Vector<PotionInfo>>> potionIter = mPotionMap.entrySet().iterator();
		while (potionIter.hasNext()) {
			Entry<PotionID, Vector<PotionInfo>> potionMapping = potionIter.next();
			if (potionMapping != null) {
				Vector<PotionInfo> potionInfo = potionMapping.getValue();
				Iterator<PotionInfo> potionInfoIter = potionInfo.iterator();
				while (potionInfoIter.hasNext()) {
					PotionInfo info = potionInfoIter.next();

					info.duration -= ticks;
					if (info.duration <= 0) {
						effectWoreOff = true;
						potionInfoIter.remove();
					}
				}

				if (potionInfo.size() == 0) {
					potionIter.remove();
				}
			}
		}

		//	If a timer wears out, run another check to make sure the best potion effect is applied.
		if (effectWoreOff) {
			applyBestPotionEffect(player);
		}
	}

	void applyBestPotionEffect(Player player) {
		PotionInfo bestEffect = null;
		Iterator<Entry<PotionID, Vector<PotionInfo>>> potionInfoIter = mPotionMap.entrySet().iterator();
		while (potionInfoIter.hasNext()) {
			Entry<PotionID, Vector<PotionInfo>> potionInfo = potionInfoIter.next();
			Vector<PotionInfo> potionVector = potionInfo.getValue();

			for (PotionInfo info : potionVector) {
				if (bestEffect == null) {
					bestEffect = info;
				} else if (info.amplifier > bestEffect.amplifier) {
					bestEffect = info;
				} else if (info.amplifier == bestEffect.amplifier &&
					info.duration > bestEffect.duration) {
					bestEffect = info;
				}
			}
		}

		if (bestEffect != null) {
			player.removePotionEffect(bestEffect.type);
			player.addPotionEffect(new PotionEffect(bestEffect.type, bestEffect.duration, bestEffect.amplifier, bestEffect.ambient, bestEffect.showParticles));
		}
	}

	JsonObject getAsJsonObject() {
		JsonObject potionIDObject = null;
		JsonObject potionMapObject = new JsonObject();
		boolean hasMapping = false;

		Iterator<Entry<PotionID, Vector<PotionInfo>>> potionIter = mPotionMap.entrySet().iterator();
		while (potionIter.hasNext()) {

			Entry<PotionID, Vector<PotionInfo>> potionMapping = potionIter.next();
			if (potionMapping != null) {
				JsonArray effectListArray = new JsonArray();

				Vector<PotionInfo> potionInfo = potionMapping.getValue();

				Iterator<PotionInfo> potionInfoIter = potionInfo.iterator();
				while (potionInfoIter.hasNext()) {
					PotionInfo info = potionInfoIter.next();
					effectListArray.add(info.getAsJsonObject(true));
				}

				if (effectListArray.size() > 0) {
					potionMapObject.add(potionMapping.getKey().getName(), effectListArray);
					hasMapping = true;
				}
			}
		}

		if (hasMapping) {
			if (potionIDObject == null) {
				potionIDObject = new JsonObject();
			}

			potionIDObject.add("potion_map", potionMapObject);
		}

		return potionIDObject;
	}

	void loadFromJsonObject(JsonObject object, PotionEffectType type) {
		JsonObject potionMap = object.get("potion_map").getAsJsonObject();
		if (potionMap != null) {
			Set<Entry<String, JsonElement>> entries = potionMap.entrySet();
			for (Entry<String, JsonElement> entry : entries) {
				Vector<PotionInfo> potionInfo = new Vector<PotionInfo>();

				PotionID id = PotionID.getFromString(entry.getKey());
				JsonArray potionInfoArray = entry.getValue().getAsJsonArray();

				Iterator<JsonElement> elementIter = potionInfoArray.iterator();
				while (elementIter.hasNext()) {
					JsonElement element = elementIter.next();

					PotionInfo info = new PotionInfo();
					info.loadFromJsonObject(element.getAsJsonObject(), type);

					potionInfo.add(info);
				}

				mPotionMap.put(id, potionInfo);
			}
		}
	}
}
