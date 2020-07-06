package com.playmonumenta.plugins.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Flying;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.potion.PotionEffectType;
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
import com.playmonumenta.plugins.bosses.bosses.TpBehindBoss;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class DelvesListener implements Listener {

	private static class MobInfo {
		private LivingEntity mMob;
		private int mPersistenceTicksRemaining = PERSISTENCE_TIME;

		public MobInfo(LivingEntity mob) {
			mMob = mob;
		}
	}

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

	private static final int PERIOD = 20 * 10;
	private static final int PERSISTENCE_TIME = 20 * 60 * 5;
	private static final int DEATH_REMOVE_PERSISTENCE_RADIUS = 24;

	private final Plugin mPlugin;
	private final Map<UUID, MobInfo> mMobTracker = new HashMap<UUID, MobInfo>();
	private final Set<UUID> mUnloadedMobTracker = new HashSet<UUID>();

	public DelvesListener(Plugin plugin) {
		mPlugin = plugin;

		new BukkitRunnable() {
			@Override
			public void run() {
				Iterator<Map.Entry<UUID, MobInfo>> iter = mMobTracker.entrySet().iterator();
				while (iter.hasNext()) {
					Map.Entry<UUID, MobInfo> entry = iter.next();
					MobInfo info = entry.getValue();

					info.mPersistenceTicksRemaining -= PERIOD;
					if (info.mPersistenceTicksRemaining <= 0) {
						if (info.mMob == null) {
							/*
							 * Mob chunk is unloaded, add to unloaded mob tracker, which
							 * removes persistence on chunk load
							 */
							mUnloadedMobTracker.add(entry.getKey());
						} else {
							info.mMob.setRemoveWhenFarAway(true);
						}

						iter.remove();
					} else if (info.mMob != null && info.mMob.isDead()) {
						iter.remove();
					}
				}
			}
		}.runTaskTimer(mPlugin, 0, PERIOD);
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

						if (mob.getRemoveWhenFarAway() && !(mob instanceof Vex) && !(mob instanceof Flying)
								&& !(mob.getScoreboardTags().contains(TpBehindBoss.identityTag))
								&& !(mob.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE) && mob.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE).getAmplifier() >= 4)) {
							mob.setRemoveWhenFarAway(false);
							mMobTracker.put(mob.getUniqueId(), new MobInfo(mob));
						}

						break;
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void chunkUnloadEvent(ChunkUnloadEvent event) {
		/*
		 * When a chunk unloads, not sure if entity objects will be preserved,
		 * so set those to null in the MobInfo objects and make note to get
		 * valid copies later
		 */
		Chunk chunk = event.getChunk();
		for (Entity entity : chunk.getEntities()) {
			MobInfo info = mMobTracker.get(entity.getUniqueId());
			if (info != null) {
				info.mMob = null;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void chunkLoadEvent(ChunkLoadEvent event) {
		/*
		 * When a chunk loads, remove persistence of mobs in the unloaded mob tracker
		 * and update the entity objects of MobInfo objects in the mob tracker
		 */
		for (Entity entity : event.getChunk().getEntities()) {
			UUID uuid = entity.getUniqueId();
			if (mUnloadedMobTracker.contains(uuid)) {
				((LivingEntity) entity).setRemoveWhenFarAway(true);
			} else {
				MobInfo info = mMobTracker.get(uuid);
				if (info != null) {
					info.mMob = (LivingEntity) entity;
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void playerDeathEvent(PlayerDeathEvent event) {
		/*
		 * Remove persistence of mobs in a radius around the dead player to prevent
		 * death camping
		 */
		for (LivingEntity mob : EntityUtils.getNearbyMobs(event.getEntity().getLocation(), DEATH_REMOVE_PERSISTENCE_RADIUS)) {
			/* 
			 * Attempt to remove the mob from the tracker. If something was removed, the mob 
			 * was present in the tracker - so make it not persistent again
			 */
			if (mMobTracker.remove(mob.getUniqueId()) != null) {
				mob.setRemoveWhenFarAway(true);
			}
		}
	}

}
