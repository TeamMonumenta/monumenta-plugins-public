package com.playmonumenta.plugins.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.delves.StatMultiplier;
import com.playmonumenta.plugins.abilities.delves.cursed.Mystic;
import com.playmonumenta.plugins.abilities.delves.cursed.Ruthless;
import com.playmonumenta.plugins.abilities.delves.cursed.Spectral;
import com.playmonumenta.plugins.abilities.delves.cursed.Unyielding;
import com.playmonumenta.plugins.abilities.delves.twisted.Arcanic;
import com.playmonumenta.plugins.abilities.delves.twisted.Dreadful;
import com.playmonumenta.plugins.abilities.delves.twisted.Merciless;
import com.playmonumenta.plugins.abilities.delves.twisted.Relentless;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class DelvesListener implements Listener {

	private static final Map<Integer, String> DELVE_MESSAGES = new HashMap<Integer, String>();
	private static final List<Class<? extends Ability>> DELVE_MODIFIERS = new ArrayList<Class<? extends Ability>>();

	static {
		DELVE_MESSAGES.put(Ruthless.SCORE, Ruthless.MESSAGE);
		DELVE_MESSAGES.put(Unyielding.SCORE, Unyielding.MESSAGE);
		DELVE_MESSAGES.put(Mystic.SCORE, Mystic.MESSAGE);
		DELVE_MESSAGES.put(Spectral.SCORE, Spectral.MESSAGE);
		DELVE_MESSAGES.put(Merciless.SCORE, Merciless.MESSAGE);
		DELVE_MESSAGES.put(Relentless.SCORE, Relentless.MESSAGE);
		DELVE_MESSAGES.put(Arcanic.SCORE, Arcanic.MESSAGE);
		DELVE_MESSAGES.put(Dreadful.SCORE, Dreadful.MESSAGE);

		DELVE_MODIFIERS.add(Ruthless.class);
		DELVE_MODIFIERS.add(Unyielding.class);
		DELVE_MODIFIERS.add(Mystic.class);
		DELVE_MODIFIERS.add(Spectral.class);
		DELVE_MODIFIERS.add(Merciless.class);
		DELVE_MODIFIERS.add(Relentless.class);
		DELVE_MODIFIERS.add(Arcanic.class);
		DELVE_MODIFIERS.add(Dreadful.class);
	}

	/*
	 * This is called by the MonumentaRedisSyncIntegration, rather than catching the event here.
	 * That plugin is an optional dependency, so it might not always be present.
	 * Importing the event directly here will cause this entire class to fail to load.
	 */
	public static void onTransfer(Player player, String target) {
		String scoreboard = ScoreboardUtils.getDelveScoreboard(target);
		if (scoreboard != null) {
			String message = DELVE_MESSAGES.get(ScoreboardUtils.getScoreboardValue(player, scoreboard));

			if (message != null) {
				MessagingUtils.sendRawMessage(player, message);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void entitySpawnEvent(EntitySpawnEvent event) {
		Entity entity = event.getEntity();

		if (entity instanceof LivingEntity) {
			LivingEntity mob = (LivingEntity) entity;

			/*
			 * 128 is the mob auto-despawn range, so probably no spawners with a
			 * larger range, and dungeon cubes are at least this far apart
			 */
			List<Player> players = PlayerUtils.playersInRange(mob.getLocation(), 128, true);

			/*
			 * We only need to check one player, since all players within range
			 * should have the same modifiers (and modifiers are only applied once)
			 */
			if (players.size() > 0) {
				Player player = players.get(0);

				for (int i = 0; i < DELVE_MODIFIERS.size(); i++) {
					StatMultiplier sm = (StatMultiplier) AbilityManager.getManager().getPlayerAbility(player, DELVE_MODIFIERS.get(i));
					/*
					 * If StatMultiplier != null, we're in a delve, so apply modifiers
					 * and the anti-cheese mechanic
					 */
					if (sm != null) {
						sm.applyOnSpawnModifiers(mob);
						break;
					}
				}
			}
		}
	}
}
