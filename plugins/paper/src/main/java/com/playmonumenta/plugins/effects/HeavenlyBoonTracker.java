package com.playmonumenta.plugins.effects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.cleric.HeavenlyBoon;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Effect for tracking "kill" procs by Cleric's Heavenly Boon skill.
 */
public class HeavenlyBoonTracker extends Effect {
	public static final String effectID = "HeavenlyBoonTracker";

	private final UUID mPlayerId; // store UUIDs instead of players to prevent memory leaks

	public HeavenlyBoonTracker(int duration, UUID playerUuid) {
		super(duration, effectID);
		mPlayerId = playerUuid;
	}

	@Override
	public void onDeath(EntityDeathEvent event) {
		Player player = Bukkit.getPlayer(mPlayerId);
		// Player logged off OR Player used
		if (player == null) {
			return;
		}

		HeavenlyBoon heavenlyBoon = Plugin.getInstance().mAbilityManager.getPlayerAbility(player, HeavenlyBoon.class);
		if (heavenlyBoon != null) {
			heavenlyBoon.triggerOnKill(event.getEntity());
		}
	}

	@Override
	public String toString() {
		return String.format("HeavenlyBoonTracker, player=%s", mPlayerId);
	}
}
