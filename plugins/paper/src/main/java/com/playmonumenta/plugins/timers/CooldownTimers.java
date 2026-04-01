package com.playmonumenta.plugins.timers;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityCollection;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.network.ClientModHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class CooldownTimers {

	public static class Cooldown {
		private int mRemaining;
		private final int mInitial;

		public Cooldown(int ticks) {
			mRemaining = ticks;
			mInitial = ticks;
		}

		public int getRemaining() {
			return mRemaining;
		}

		public int getInitial() {
			return mInitial;
		}

		public void setRemaining(int ticks) {
			mRemaining = ticks;
		}
	}

	private final Map<UUID, Map<ClassAbility, List<Cooldown>>> mTimers = new HashMap<>();
	private final Plugin mPlugin;

	public CooldownTimers(Plugin plugin) {
		mPlugin = plugin;
	}

	public boolean isAbilityOnCooldown(UUID playerID, @Nullable ClassAbility spell) {
		if (spell == null) {
			return false;
		}
		//  First check if the player has any cooldowns in the HashMap.
		Map<ClassAbility, List<Cooldown>> playerCooldowns = mTimers.get(playerID);
		if (playerCooldowns != null) {
			//  Next check if the ability is in our HashMap, if not we're not on cooldown.
			List<Cooldown> spellCooldowns = playerCooldowns.get(spell);
			return spellCooldowns != null && !spellCooldowns.isEmpty() && spellCooldowns.get(0).getRemaining() > 0;
		} else {
			// No player, means no cooldown.
			return false;
		}
	}

	public void setCooldown(Player player, ClassAbility spell, int cooldownTime) {
		if (!player.isOnline()) {
			return;
		}
		UUID playerID = player.getUniqueId();
		Map<ClassAbility, List<Cooldown>> playerCooldowns = mTimers.computeIfAbsent(playerID, p -> new HashMap<>());
		playerCooldowns.computeIfAbsent(spell, s -> new ArrayList<>()).add(new Cooldown(cooldownTime));
		ClientModHandler.updateAbility(player, spell);
	}

	public void replaceCooldownList(Player player, ClassAbility spell, List<Cooldown> cooldowns) {
		UUID playerID = player.getUniqueId();
		Map<ClassAbility, List<Cooldown>> playerCooldowns = mTimers.computeIfAbsent(playerID, p -> new HashMap<>());
		playerCooldowns.put(spell, cooldowns);
		ClientModHandler.updateAbility(player, spell);
	}

	public void increaseCurrentCooldownOrCreateNew(Player player, ClassAbility spell, int cooldownTime) {
		if (!player.isOnline()) {
			return;
		}
		UUID playerID = player.getUniqueId();
		Map<ClassAbility, List<Cooldown>> playerCooldowns = mTimers.computeIfAbsent(playerID, p -> new HashMap<>());
		List<Cooldown> cooldownList = playerCooldowns.computeIfAbsent(spell, s -> new ArrayList<>());
		if (cooldownList.isEmpty()) {
			cooldownList.add(new Cooldown(cooldownTime));
		} else {
			Cooldown cd = cooldownList.get(0);
			cd.setRemaining(cd.getRemaining() + cooldownTime);
		}

		ClientModHandler.updateAbility(player, spell);
	}

	public void removeCooldown(Player player, ClassAbility spell) {
		removeCooldown(player, spell, true);
	}

	public void removeCooldown(Player player, ClassAbility spell, boolean updateMod) {
		UUID playerID = player.getUniqueId();
		Map<ClassAbility, List<Cooldown>> cooldownHash = mTimers.get(playerID);
		if (cooldownHash != null) {
			cooldownHash.remove(spell);
			if (updateMod) {
				ClientModHandler.updateAbility(player, spell);
			}
		}
	}

	public void removeLastCooldown(Player player, ClassAbility spell) {
		UUID playerID = player.getUniqueId();
		Map<ClassAbility, List<Cooldown>> cooldownHash = mTimers.get(playerID);
		if (cooldownHash != null) {
			List<Cooldown> cooldownList = cooldownHash.get(spell);
			if (cooldownList != null && !cooldownList.isEmpty()) {
				cooldownList.removeLast();
				if (cooldownList.isEmpty()) {
					cooldownHash.remove(spell);
				}
				ClientModHandler.updateAbility(player, spell);
			}
		}
	}

	public void updateCooldowns(int ticks) {
		for (UUID uuid : new HashSet<>(mTimers.keySet())) {
			updateCooldowns(uuid, mPlugin.getPlayer(uuid), ticks, false);
		}
	}

	private void updateCooldowns(UUID uuid, @Nullable Player player, int ticks, boolean alwaysUpdateClientMod) {
		updateCooldowns(uuid, player, s -> true, (c, s) -> c.getRemaining() - ticks, alwaysUpdateClientMod);
	}

	private void updateCooldowns(UUID uuid, @Nullable Player player, Predicate<ClassAbility> filter, BiFunction<Cooldown, ClassAbility, Integer> func, boolean alwaysUpdateClientMod) {
		Map<ClassAbility, List<Cooldown>> cooldownMap = mTimers.get(uuid);
		if (cooldownMap == null) {
			return;
		}

		if (player == null || !player.isOnline()) {
			return;
		}

		Iterator<Entry<ClassAbility, List<Cooldown>>> abilityIter = cooldownMap.entrySet().iterator();
		while (abilityIter.hasNext()) {
			Entry<ClassAbility, List<Cooldown>> cooldownEntry = abilityIter.next();
			ClassAbility spell = cooldownEntry.getKey();
			if (!filter.test(spell)) {
				continue;
			}

			List<Cooldown> cooldownList = cooldownEntry.getValue();
			if (cooldownList.isEmpty()) {
				abilityIter.remove();
				continue;
			}
			Cooldown cooldown = cooldownList.get(0);

			//  Update the cooldown time, if it's not over, set the value, else remove it.
			int time = func.apply(cooldown, spell);
			if (time <= 0) {

				cooldownList.remove(0);
				Ability ability = mPlugin.mAbilityManager.getPlayerAbilities(player).getAbilityIgnoringSilence(spell);

				if (cooldownList.isEmpty()) {
					abilityIter.remove();
					if (ability != null) {
						showOffCooldownMessage(player, spell);
					}
				}

				if (ability instanceof MultipleChargeAbility multipleChargeAbility) {
					multipleChargeAbility.updateCharges();
				}

				ClientModHandler.updateAbility(player, spell);
			} else {
				cooldown.setRemaining(time);
				// don't send update to client mod if this is just from time passing normally
				if (alwaysUpdateClientMod) {
					ClientModHandler.updateAbility(player, spell);
				}
			}
		}

		//  If this player no longer has any more cooldowns for them, remove the player.
		if (cooldownMap.isEmpty()) {
			mTimers.remove(uuid);
		}
	}

	/**
	 * Reduces the players ticks on all of their cooldowns.
	 *
	 * @param player The player whose cooldown ticks will be updated
	 * @param ticks  The cooldown reduction in ticks
	 */
	public void updateCooldowns(Player player, int ticks) {
		updateCooldowns(player.getUniqueId(), player, ticks, true);
	}

	/**
	 * Reduces the players ticks on all of their cooldowns.
	 *
	 * @param player   The player whose cooldown ticks will be updated
	 * @param modifier The cooldown reduction % to apply, e.g. 0.05 will be a 5% reduction.
	 * @return Number of abilities that had their cooldowns reduced.
	 */
	public int updateCooldownsPercent(Player player, double modifier) {
		Map<ClassAbility, List<Cooldown>> cds = mTimers.get(player.getUniqueId());
		if (cds == null) {
			return 0;
		}
		int abilitiesReduced = cds.size();

		updateCooldowns(player.getUniqueId(), player, s -> true, (c, s) -> (int) (c.getRemaining() - c.getInitial() * modifier), true);

		return abilitiesReduced;
	}

	public void updateCooldownPercent(Player player, ClassAbility spell, double modifier) {
		updateCooldowns(player.getUniqueId(), player, s -> s == spell, (c, s) -> (int) (c.getRemaining() - c.getInitial() * modifier), true);
	}

	public void updateCooldown(Player player, ClassAbility spell, int ticks) {
		updateCooldowns(player.getUniqueId(), player, s -> s == spell, (c, s) -> c.getRemaining() - ticks, true);
	}

	public void updateCooldownsExcept(Player player, ClassAbility spell, int ticks) {
		updateCooldowns(player.getUniqueId(), player, s -> s != spell, (c, s) -> c.getRemaining() - ticks, true);
	}

	public void refreshCurrentCooldowns(Player player) {
		updateCooldowns(player.getUniqueId(), player, s -> true, (c, s) -> 0, true);
	}

	public void removeAllCooldowns(Player player) {
		Map<ClassAbility, List<Cooldown>> cds = mTimers.remove(player.getUniqueId());
		if (cds != null) {
			for (ClassAbility classAbility : cds.keySet()) {
				ClientModHandler.updateAbility(player, classAbility);
			}
		}
	}

	public Map<ClassAbility, List<Cooldown>> getCooldowns(UUID playerID) {
		return mTimers.getOrDefault(playerID, new HashMap<>());
	}

	public int countAbilitiesOnCooldown(Player player) {
		return (int) getCooldowns(player.getUniqueId()).values().stream().filter(list -> !list.isEmpty() && list.get(0).getRemaining() > 0).count();
	}

	public List<Cooldown> getCooldownList(UUID playerID, ClassAbility ability) {
		Map<ClassAbility, List<Cooldown>> player = mTimers.get(playerID);
		if (player == null) {
			return new ArrayList<>();
		}
		List<Cooldown> cooldownList = player.get(ability);
		if (cooldownList == null) {
			return new ArrayList<>();
		}
		return cooldownList;
	}

	/**
	 * returns the remaining cooldown of the given ability, in ticks. Returns 0 if not on cooldown.
	 */
	public int getCooldown(UUID playerID, ClassAbility ability) {
		List<Cooldown> cooldownList = getCooldownList(playerID, ability);
		return cooldownList.isEmpty() ? 0 : cooldownList.get(0).getRemaining();
	}

	public void showOffCooldownMessage(Player player, ClassAbility spell) {
		AbilityCollection abilityCollection = mPlugin.mAbilityManager.getPlayerAbilities(player);
		if (abilityCollection.getAbilityIgnoringSilence(spell) != null) {
			Ability ability = abilityCollection.getAbility(spell);
			if (ability != null) {
				ability.showOffCooldownMessage();
			}
		}
	}
}
