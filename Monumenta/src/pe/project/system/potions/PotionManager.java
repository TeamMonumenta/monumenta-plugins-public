package pe.project.system.potions;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import pe.project.system.potions.PotionManager.PotionID;
import pe.project.utils.PotionUtils.PotionInfo;

class PotionMap {
	public boolean addPotionInfo(PotionID id, PotionInfo info) {
		PotionInfo potionInfo = mPotionInfo.get(id);
		if (potionInfo != null) {
			if (info.amplifier > potionInfo.amplifier
				|| (info.amplifier == potionInfo.amplifier && info.duration > potionInfo.duration)) {
				mPotionInfo.put(id, info);
				return true;
			}
		} else {
			mPotionInfo.put(id, info);
			return true;
		}
		
		return false;
	}
	
	public void updatePotionStatus(Player player) {
		PotionInfo bestEffect = null;
		Iterator<Entry<PotionID, PotionInfo>> potionInfoIter = mPotionInfo.entrySet().iterator();
		while (potionInfoIter.hasNext()) {
			Entry<PotionID, PotionInfo> potionInfo = potionInfoIter.next();
			PotionInfo info = potionInfo.getValue();
			
			if (bestEffect == null) {
				bestEffect = info;
			} else if (info.amplifier > bestEffect.amplifier) {
				bestEffect = info;
			} else if (info.amplifier == bestEffect.amplifier &&
				info.duration > bestEffect.duration) {
				bestEffect = info;
			}
			
			if (bestEffect != null) {
				player.removePotionEffect(bestEffect.type);
				player.addPotionEffect(new PotionEffect(bestEffect.type, bestEffect.duration, bestEffect.amplifier, true, false));
			}
		}
	}
	
	//	ID / Potion info
	HashMap<PotionID, PotionInfo> mPotionInfo;
}

class PlayerPotionInfo {
	public void addPotionInfo(PotionID id, PotionInfo info) {
		PotionMap type = mPotionInfo.get(info.type);
		if (type != null) {
			type.addPotionInfo(id, info);
		} else {
			PotionMap list = new PotionMap();
			list.addPotionInfo(id, info);
			mPotionInfo.put(info.type, list);
		}
	}
	
	public void updatePotionStatus(Player player) {
		Iterator<Entry<PotionEffectType, PotionMap>> potionMapIter = mPotionInfo.entrySet().iterator();
		while (potionMapIter.hasNext()) {
			Entry<PotionEffectType, PotionMap> potionEntry = potionMapIter.next();
			potionEntry.getValue().updatePotionStatus(player);
		}
	}
	
	//	Effect Type / Potion List
	HashMap<PotionEffectType, PotionMap> mPotionInfo;
}

public class PotionManager {
	public enum PotionID {
		CONSUMED_POTION(0),
		ABILITY(1),
		SAFE_ZONE(2);
		
		private int value;
		private PotionID(int value)	{	this.value = value;	}
		public int getValue()		{	return this.value;	}
	}
	
	public void addPotion(Player player, PotionID id, PotionInfo info) {
		UUID uuid = player.getUniqueId();
		PlayerPotionInfo potionInfo = mPotionInfo.get(uuid);
		if (potionInfo != null) {
			PlayerPotionInfo playerPotionInfo = mPotionInfo.get(uuid);
			if (playerPotionInfo != null) {
				playerPotionInfo.addPotionInfo(id, info);
			} else {
				PlayerPotionInfo newPotionInfo = new PlayerPotionInfo();
				newPotionInfo.addPotionInfo(id, info);
				mPotionInfo.put(uuid, newPotionInfo);
			}
		}
	}
	
	void updatePotionStatus(Player player) {
		PlayerPotionInfo info = mPotionInfo.get(player.getUniqueId());
		if (info != null) {
			info.updatePotionStatus(player);
		}
	}
	
	//	Player ID / Player Potion Info
	HashMap<UUID, PlayerPotionInfo> mPotionInfo;
}
