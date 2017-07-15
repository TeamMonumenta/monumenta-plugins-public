package pe.project.json.objects;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.potion.PotionEffectType;

import pe.project.managers.potion.PlayerPotionInfo;
import pe.project.managers.potion.PotionMap;

public class PlayerPotionInfoObject {
	HashMap<String, PotionMapObject> potionInfo;
	
	public PlayerPotionInfoObject(PlayerPotionInfo info) {
		potionInfo = new HashMap<String, PotionMapObject>();
		
		Iterator<Entry<PotionEffectType, PotionMap>> iter = info.mPotionInfo.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<PotionEffectType, PotionMap> entry = iter.next();
			
			PotionEffectType effectType = entry.getKey();
			PotionMap object = entry.getValue();
			
			potionInfo.put(effectType.getName(), new PotionMapObject(object));
		}
	}
	
	public PlayerPotionInfo convertToPlayerPotionInfo() {
		PlayerPotionInfo playerPotionInfo = new PlayerPotionInfo();
		
		Iterator<Entry<String, PotionMapObject>> iter =  potionInfo.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, PotionMapObject> entry = iter.next();
			
			String effectString = entry.getKey();
			PotionMapObject mapObject = entry.getValue();
			
			playerPotionInfo.mPotionInfo.put(PotionEffectType.getByName(effectString), mapObject.convtertToPotionMap());
		}
		
		return playerPotionInfo;
	}
}
