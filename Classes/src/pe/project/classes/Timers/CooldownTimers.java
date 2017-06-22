package pe.project.classes.Timers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.entity.Player;

import pe.project.classes.Main;

public class CooldownTimers {
	public HashMap<UUID, HashMap<Integer, Integer>> mTimers = null;
	private Main mPlugin = null;
	
	public CooldownTimers(Main plugin) {
		mPlugin = plugin;
		mTimers = new HashMap<UUID, HashMap<Integer, Integer>>();
	}
	
	public void RegisterCooldown(Player player, Integer cooldownID, Integer cooldownTime) {
		mTimers.put(player.getUniqueId(), new HashMap<Integer, Integer>(cooldownID, cooldownTime));
	}
	
	public boolean isAbilityOnCooldown(UUID playerID, Integer abilityID) {
		//	First check if the player has any cooldowns in the HashMap.
		HashMap<Integer, Integer> player = mTimers.get(playerID);
		if (player != null) {
			//	Next check if the ability is in our HashMap, if not we're not on cooldown.
			Integer ability = player.get(abilityID);
			if (ability == null) {
				return false;
			}
		}
		//	No player, means no cooldown.
		else {
			return false;
		}
		
		return true;
	}
	
	public boolean AddCooldown(UUID playerID, Integer abilityID, Integer cooldownTime) {
		//	First let's investigate whether this player already has existing cooldowns.
		HashMap<Integer, Integer> player = mTimers.get(playerID);
		//	Is there a player already storing cooldowns?
		if (player != null) {
			//	Next check to see if this abilityID already exist in this HashMap, if not than we're
			//	not on cooldown and we should put it on cooldown.
			Integer ability = player.get(abilityID);
			if (ability == null) {
				player.put(abilityID, cooldownTime);
				return true;
			}
		}
		//	Else add a new player entry with it's info.
		else {
			HashMap<Integer, Integer> cooldownHash = new HashMap<Integer, Integer>();
			
			cooldownHash.put(abilityID, cooldownTime);
			mTimers.put(playerID, cooldownHash);

			return true;
		}
		
		return false;
	}
	
	public void UpdateCooldowns(Integer ticks) {
		//	Our set of player cooldowns is broken down into a Hashmap of Hashmaps.
		//	Because of this, we first loop through each player (UUID), than we loop
		//	through their different ability ID's.
		Iterator<Entry<UUID, HashMap<Integer, Integer>>> playerIter = mTimers.entrySet().iterator();
		while (playerIter.hasNext()) {
			Entry<UUID, HashMap<Integer, Integer>> player = playerIter.next();
			
		    Iterator<Entry<Integer, Integer>> abilityIter = player.getValue().entrySet().iterator();
		    while(abilityIter.hasNext()) {
		    	Entry<Integer, Integer> cooldown = abilityIter.next();
		    	
		    	Player _player = mPlugin.getPlayer(player.getKey());
		    	if (_player != null && _player.isOnline()) {
			    	//	Update the cooldown time, if it's not over, set the value, else remove it.
			    	int time = cooldown.getValue() - ticks;
			    	if (time <= 0) {
			    		int cooldownID = cooldown.getKey();
			    		
			    		if (cooldownID < 100) {
			    			mPlugin.getClass(_player).AbilityOffCooldown(_player, cooldownID);
			    		} else {
			    			mPlugin.getClass(_player).FakeAbilityOffCooldown(_player, cooldownID);
			    		}
			    		
			    		abilityIter.remove();
			    	} else {
			    		cooldown.setValue(time);
			    	}
		    	}
		    }
		    
		    //	If this player no longer has any more cooldowns for them, remove the player.
		    if (player.getValue().isEmpty()) {
		    	playerIter.remove();
		    }
		}
	}
	
	public void removeCooldowns(UUID playerID) {
		mTimers.remove(playerID);
	}
}
