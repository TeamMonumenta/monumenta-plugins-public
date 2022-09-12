package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.cleric.SanctifiedArmor;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Effect for the healing of enhanced sanctified armor
 */
public class SanctifiedArmorHeal extends Effect {
	public static final String effectID = "SanctifiedArmorHeal";

	private static final int DURATION = 20 * 60 * 5; // 5 minutes

	private final Set<UUID> mPlayerUuids = new HashSet<>(); // store UUIDs instead of players to prevent memory leaks

	public SanctifiedArmorHeal(UUID playerUuid) {
		super(DURATION, effectID);
		mPlayerUuids.add(playerUuid);
	}

	public void addPlayer(Player player) {
		mPlayerUuids.add(player.getUniqueId());
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		for (UUID playerUuid : mPlayerUuids) {
			Player player = Bukkit.getPlayer(playerUuid);
			if (player == null) { // player logged off, don't do any damage
				continue;
			}
			SanctifiedArmor sanctifiedArmor = Plugin.getInstance().mAbilityManager.getPlayerAbility(player, SanctifiedArmor.class);
			if (sanctifiedArmor != null) {
				sanctifiedArmor.onMobKilled(event.getEntity());
			}
		}
	}

	@Override
	public String toString() {
		return String.format("SanctifiedArmorHeal, players=%s", mPlayerUuids);
	}
}
