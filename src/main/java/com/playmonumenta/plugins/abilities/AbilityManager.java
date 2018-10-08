package com.playmonumenta.plugins.abilities;

import com.playmonumenta.plugins.abilities.AbilityCollection;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

public class AbilityManager {
	private Map<UUID, AbilityCollection> mAbilities = new HashMap<UUID, AbilityCollection>();

	private static AbilityManager manager = new AbilityManager();

	public static AbilityManager getManager() {
		return manager;
	}

	public void updatePlayerAbilities(Player player) {
		AbilityCollection collection = mAbilities.get(player.getUniqueId());
		if (collection == null) {
			collection = new AbilityCollection(player);
			mAbilities.put(player.getUniqueId(), collection);
		}
		collection.refreshAbilities();
	}

	/*
	 * Returns the ability collection for the player or null if
	 * there isn't one
	 */
	public AbilityCollection getPlayerAbilities(Player player) {
		if (!mAbilities.containsKey(player.getUniqueId())) {
			updatePlayerAbilities(player);
		}
		return mAbilities.get(player.getUniqueId());
	}
}
