package pe.project.json.objects;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

import java.util.Map.Entry;

import pe.project.managers.potion.PlayerPotionInfo;
public class PotionManagerObject {
	HashMap<UUID, PlayerPotionInfoObject> potionManager;
	
	public PotionManagerObject(HashMap<UUID, PlayerPotionInfo> manager) {
		potionManager = new HashMap<UUID, PlayerPotionInfoObject>();

		Iterator<Entry<UUID, PlayerPotionInfo>> iter = manager.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<UUID, PlayerPotionInfo> entry = iter.next();
			
			UUID id = entry.getKey();
			PlayerPotionInfo playerPotionInfo = entry.getValue();
			
			potionManager.put(id, new PlayerPotionInfoObject(playerPotionInfo));
		}
	}
	
	public HashMap<UUID, PlayerPotionInfo> convertToPotionManager() {
		HashMap<UUID, PlayerPotionInfo> manager = new HashMap<UUID, PlayerPotionInfo>();
		
		Iterator<Entry<UUID, PlayerPotionInfoObject>> infoIter = potionManager.entrySet().iterator();
		while (infoIter.hasNext()) {
			Entry<UUID, PlayerPotionInfoObject> entry = infoIter.next();
			
			UUID id = entry.getKey();
			PlayerPotionInfoObject playerPotionInfo = entry.getValue();
			
			manager.put(id, playerPotionInfo.convertToPlayerPotionInfo());
		}
		
		return manager;
	}
}
