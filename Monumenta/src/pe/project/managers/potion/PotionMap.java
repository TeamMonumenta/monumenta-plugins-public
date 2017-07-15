package pe.project.managers.potion;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

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
		Vector<PotionInfo> trackedPotionInfo = mPotionMap.get(id);
		if (trackedPotionInfo != null) {
			trackedPotionInfo.clear();
		}

		applyBestPotionEffect(player);
	}
	
	public void clearPotionIDType(Player player, PotionID id) {
		mPotionMap.remove(id);
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
}
