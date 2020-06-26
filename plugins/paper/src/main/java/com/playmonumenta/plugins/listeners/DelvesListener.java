package com.playmonumenta.plugins.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
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
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.scriptedquests.utils.MessagingUtils;

public class DelvesListener implements Listener {

	private static final int PERIOD = 20 * 10;

	private static final int PERSISTENCE_TIME = 20 * 60 * 20;
	private static final double PERSISTENCE_CHANCE = 0.2;

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

	private final Plugin mPlugin;
	private final World mWorld;
	private final Set<LivingEntity> mPersistenceProcessedMobs = new HashSet<LivingEntity>();
	private final Map<UUID, Integer> mPersistentMobs = new HashMap<UUID, Integer>();

	public DelvesListener(Plugin plugin, World world) {
		mPlugin = plugin;
		mWorld = world;

		// Runs forever, ticks down on removing persistence from mobs
		new BukkitRunnable() {
			@Override
			public void run() {
				Iterator<LivingEntity> iter1 = mPersistenceProcessedMobs.iterator();
				while (iter1.hasNext()) {
					LivingEntity mob = iter1.next();
					if (mob.isDead() || !mob.isValid()) {
						iter1.remove();
					}
				}

				Iterator<Map.Entry<UUID, Integer>> iter2 = mPersistentMobs.entrySet().iterator();
				while (iter2.hasNext()) {
					Map.Entry<UUID, Integer> entry = iter2.next();
					UUID uuid = entry.getKey();
					int persistenceTime = entry.getValue() - PERIOD;

					if (persistenceTime <= 0) {
						LivingEntity mob = (LivingEntity) EntityUtils.getEntity(mWorld, uuid);

						if (!EntityUtils.isElite(mob) && !EntityUtils.isBoss(mob)) {
							mob.setRemoveWhenFarAway(true);
						}

						iter2.remove();
					} else {
						mPersistentMobs.put(uuid, persistenceTime);
					}
				}
			}
		}.runTaskLater(mPlugin, PERIOD);
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

		// Handle stat multipliers
		if (entity instanceof LivingEntity) {
			LivingEntity mob = (LivingEntity) entity;

			// 128 is the mob auto-despawn range, so probably no spawners with a larger range, and dungeon cubes are at least this far apart
			List<Player> players = PlayerUtils.playersInRange(mob.getLocation(), 128, true);

			// We only need to check one player, since all players within range should have the same modifiers (and modifiers are only applied once)
			if (players.size() > 0) {
				Player player = players.get(0);

				for (int i = 0; i < DELVE_MODIFIERS.size(); i++) {
					StatMultiplier sm = (StatMultiplier) AbilityManager.getManager().getPlayerAbility(player, DELVE_MODIFIERS.get(i));
					if (sm != null) {
						sm.applyOnSpawnModifiers(mob);
						break;
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void entityTargetLivingEntityEvent(EntityTargetLivingEntityEvent event) {
		/*
		 * Anti-deathrunning mechanic; we run this check when a mob targets a player
		 * instead of when it spawns, so that mobs spawning through walls don't get
		 * persistence and start clogging up the dungeon.
		 *
		 * If the mob has targeted a player, then either it is an obstacle to the
		 * player (and the player should kill it, not despawn it) or it is not an
		 * obstacle to the player, in which case, adding 20 minutes to the despawn
		 * timer doesn't affect the player.
		 *
		 * The low chance (20%) means that dying normally will not be absurdly hard
		 * to recover from (since 80% of the mobs should go away), but it will make
		 * it significantly harder to deathrun normally, as more and more mobs build
		 * up.
		 */
		Entity entity = event.getEntity();
		if (entity instanceof LivingEntity) {
			LivingEntity mob = (LivingEntity) entity;
			LivingEntity target = event.getTarget();
			if (target instanceof Player) {
				Player player = (Player) target;

				// Check if the player has a delve modifier
				for (int i = 0; i < DELVE_MODIFIERS.size(); i++) {
					if (AbilityManager.getManager().getPlayerAbility(player, DELVE_MODIFIERS.get(i)) != null) {
						if (!mPersistenceProcessedMobs.contains(mob)) {
							mPersistenceProcessedMobs.add(mob);

							if (FastUtils.RANDOM.nextDouble() < PERSISTENCE_CHANCE) {
								mPersistentMobs.put(mob.getUniqueId(), PERSISTENCE_TIME);
								mob.setRemoveWhenFarAway(false);
							}
						}

						break;
					}
				}
			}
		}
	}
}
