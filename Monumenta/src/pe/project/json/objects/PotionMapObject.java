package pe.project.json.objects;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;

import pe.project.managers.potion.PotionMap;
import pe.project.managers.potion.PotionManager.PotionID;
import pe.project.utils.PotionUtils.PotionInfo;

public class PotionMapObject {
	HashMap<String, Vector<PotionInfoObject>> potionMap;
	
	public PotionMapObject(PotionMap map) {
		potionMap = new HashMap<String, Vector<PotionInfoObject>>();
		
		Iterator<Entry<PotionID, Vector<PotionInfo>>> iter = map.mPotionMap.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<PotionID, Vector<PotionInfo>> entry = iter.next();
			
			PotionID potionID = entry.getKey();
			Vector<PotionInfo> potionInfo = entry.getValue();
			
			Vector<PotionInfoObject> newPotionInfoList = new Vector<PotionInfoObject>();
			for (PotionInfo info : potionInfo ) {
				newPotionInfoList.add(new PotionInfoObject(info.type.getName(), info.duration, info.amplifier, info.ambient, info.showParticles));
			}
			
			potionMap.put(potionID.getName(), newPotionInfoList);
		}
	}
	
	public PotionMap convtertToPotionMap() {
		PotionMap map = new PotionMap();
		
		Iterator<Entry<String, Vector<PotionInfoObject>>> iter = potionMap.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, Vector<PotionInfoObject>> entry = iter.next();
			
			String potionIDString = entry.getKey();
			Vector<PotionInfoObject> object = entry.getValue();
			
			Vector<PotionInfo> potionInfoVector = new Vector<PotionInfo>();
			for (PotionInfoObject info : object) {
				PotionInfo potionInfo = info.convtertToPotionInfo();
				potionInfoVector.add(potionInfo);
			}
			
			map.mPotionMap.put(PotionID.getFromString(potionIDString), potionInfoVector);
		}
		
		return map;
	}
}
