package com.playmonumenta.plugins.timers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.MessagingUtils;

public class CooldownTimers {
	public HashMap<UUID, HashMap<ClassAbility, Integer>> mTimers = null;
	private Plugin mPlugin = null;

	public CooldownTimers(Plugin plugin) {
		mPlugin = plugin;
		mTimers = new HashMap<UUID, HashMap<ClassAbility, Integer>>();
	}

	public void registerCooldown(Player player, ClassAbility spell, Integer cooldownTime) {
		HashMap<ClassAbility, Integer> cd = new HashMap<ClassAbility, Integer>();
		cd.put(spell, cooldownTime);
		mTimers.put(player.getUniqueId(), cd);
	}

	public boolean isAbilityOnCooldown(UUID playerID, ClassAbility spell) {
		//  First check if the player has any cooldowns in the HashMap.
		HashMap<ClassAbility, Integer> player = mTimers.get(playerID);
		if (player != null) {
			//  Next check if the ability is in our HashMap, if not we're not on cooldown.
			Integer ability = player.get(spell);
			if (ability == null) {
				return false;
			}
		} else {
			// No player, means no cooldown.
			return false;
		}

		return true;
	}

	public void addCooldown(Player player, ClassAbility spell, Integer cooldownTime) {
		UUID playerID = player.getUniqueId();
		// First let's investigate whether this player already has existing cooldowns.
		HashMap<ClassAbility, Integer> playerCooldowns = mTimers.get(playerID);
		// Is there a player already storing cooldowns?
		if (playerCooldowns != null) {
			// Set the cooldown, even if it already exists
			playerCooldowns.put(spell, cooldownTime);
		} else {
			// Else add a new player entry with its info.
			HashMap<ClassAbility, Integer> cooldownHash = new HashMap<ClassAbility, Integer>();

			cooldownHash.put(spell, cooldownTime);
			mTimers.put(playerID, cooldownHash);
		}
		ClientModHandler.updateAbility(player, spell);
	}

	public void removeCooldown(UUID playerID, ClassAbility spell) {
		HashMap<ClassAbility, Integer> cooldownHash = mTimers.get(playerID);
		if (cooldownHash != null) {
			cooldownHash.remove(spell);
		}
	}

	public void updateCooldowns(int ticks) {
		//  Our set of player cooldowns is broken down into a Hashmap of Hashmaps.
		//  Because of this, we first loop through each player (UUID), than we loop
		//  through their different ability ID's.
		Iterator<Entry<UUID, HashMap<ClassAbility, Integer>>> playerIter = mTimers.entrySet().iterator();
		while (playerIter.hasNext()) {
			Entry<UUID, HashMap<ClassAbility, Integer>> element = playerIter.next();

			Iterator<Entry<ClassAbility, Integer>> abilityIter = element.getValue().entrySet().iterator();
			while (abilityIter.hasNext()) {
				Entry<ClassAbility, Integer> cooldown = abilityIter.next();

				Player player = mPlugin.getPlayer(element.getKey());
				if (player != null && player.isOnline()) {
					//  Update the cooldown time, if it's not over, set the value, else remove it.
					int time = cooldown.getValue() - ticks;
					if (time <= 0) {
						ClassAbility spell = cooldown.getKey();
						MessagingUtils.sendActionBarMessage(mPlugin, player, spell.getName() + " is now off cooldown!");

						abilityIter.remove();

						ClientModHandler.updateAbility(player, spell);
					} else {
						cooldown.setValue(time);
						// don't send update to client mod, as this is the normal case of time passing
					}
				}
			}

			//  If this player no longer has any more cooldowns for them, remove the player.
			if (element.getValue().isEmpty()) {
				playerIter.remove();
			}
		}
	}

	/**
	 * Reduces the players ticks on all of their cooldowns.
	 * @param player The player whose cooldown ticks will be updated
	 * @param ticks The cooldown reduction in ticks
	 */
	public void updateCooldowns(Player player, int ticks) {
		HashMap<ClassAbility, Integer> cds = mTimers.get(player.getUniqueId());

		if (cds != null) {
			Iterator<ClassAbility> it = cds.keySet().iterator();
			while (it.hasNext()) {
				ClassAbility spell = it.next();
				int cd = cds.get(spell);
				cd -= ticks;
				if (cd <= 0) {
					MessagingUtils.sendActionBarMessage(mPlugin, player, spell.getName() + " is now off cooldown!");
					it.remove();
				} else {
					cds.put(spell, cd);
				}
				ClientModHandler.updateAbility(player, spell);
			}

			if (cds.isEmpty()) {
				mTimers.remove(player.getUniqueId());
			}
		}
	}

	public void updateCooldown(Player player, ClassAbility spell, int ticks) {
		HashMap<ClassAbility, Integer> cds = mTimers.get(player.getUniqueId());

		if (cds != null && cds.containsKey(spell)) {
			int cd = cds.get(spell);
			cd -= ticks;
			if (cd <= 0) {
				MessagingUtils.sendActionBarMessage(mPlugin, player, spell.getName() + " is now off cooldown!");
				cds.remove(spell);
			} else {
				cds.put(spell, cd);
			}
			ClientModHandler.updateAbility(player, spell);

			if (cds.isEmpty()) {
				mTimers.remove(player.getUniqueId());
			}
		}
	}

	public void removeAllCooldowns(Player player) {
		HashMap<ClassAbility, Integer> cds = mTimers.remove(player.getUniqueId());
		if (cds != null) {
			for (ClassAbility classAbility : cds.keySet()) {
				ClientModHandler.updateAbility(player, classAbility);
			}
		}
	}

	public Set<ClassAbility> getCooldowns(UUID playerID) {
		HashMap<ClassAbility, Integer> player = mTimers.get(playerID);
		if (player != null) {
			return player.keySet();
		} else {
			return null;
		}
	}

	/**
	 * returns the remaining cooldown of the given ability, in ticks. Returns 0 if not on cooldown.
	 */
	public int getCooldown(UUID playerID, ClassAbility ability) {
		HashMap<ClassAbility, Integer> player = mTimers.get(playerID);
		if (player != null) {
			return player.getOrDefault(ability, 0);
		} else {
			return 0;
		}
	}

}
