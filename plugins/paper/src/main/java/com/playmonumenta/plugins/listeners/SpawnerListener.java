package com.playmonumenta.plugins.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;

import com.playmonumenta.plugins.Plugin;

public class SpawnerListener implements Listener {

	private static class MobInfo {
		private Entity mMob;
		private Entity mLastTarget = null;
		private int mTickLastTargeted = -1;

		public MobInfo(Entity mob) {
			mMob = mob;
		}
	}

	private static final int PLAYER_CHECK_RADIUS_SQUARED = 8 * 8;
	private static final int INACTIVITY_TIMER = 20 * 45;
	private static final int CLEANER_INTERVAL = 20 * 30;

	private final Map<UUID, MobInfo> mMobInfos = new HashMap<UUID, MobInfo>();
	private final Map<BlockVector, List<MobInfo>> mSpawnerInfos = new HashMap<BlockVector, List<MobInfo>>();

	private final BukkitRunnable mCleaner;

	public SpawnerListener(Plugin plugin) {
		mCleaner = new BukkitRunnable() {
			@Override
			public void run() {
				// Removes mob info entries from the map if mob is dead or invalid
				Iterator<MobInfo> mobInfoIter = mMobInfos.values().iterator();
				while (mobInfoIter.hasNext()) {
					Entity mob = mobInfoIter.next().mMob;
					if (mob != null && (mob.isDead() || !mob.isValid())) {
						mobInfoIter.remove();
					}
				}

				// Removes spawner mob list entries from the map if mob list for spawner is empty
				Iterator<List<MobInfo>> spawnerInfoIter = mSpawnerInfos.values().iterator();
				while (spawnerInfoIter.hasNext()) {
					List<MobInfo> spawnerInfo = spawnerInfoIter.next();

					Iterator<MobInfo> spawnerListIter = spawnerInfo.iterator();
					while (spawnerListIter.hasNext()) {
						Entity mob = spawnerListIter.next().mMob;
						if (mob != null && (mob.isDead() || !mob.isValid())) {
							mobInfoIter.remove();
						}
					}

					if (spawnerInfo.size() == 0) {
						spawnerInfoIter.remove();
					}
				}
			}
		};

		mCleaner.runTaskTimer(plugin, 0, CLEANER_INTERVAL);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void spawnerSpawnEvent(SpawnerSpawnEvent event) {
		BlockVector spawnerLoc = new BlockVector(event.getSpawner().getLocation().toVector());
		Entity mob = event.getEntity();

		List<MobInfo> spawnerInfo = mSpawnerInfos.get(spawnerLoc);
		if (spawnerInfo == null) {
			spawnerInfo = new ArrayList<MobInfo>();
			mSpawnerInfos.put(spawnerLoc, spawnerInfo);
		}

		// Generate list of player locations a single time
		List<Location> playerLocations = new ArrayList<Location>();
		for (Player player : Bukkit.getOnlinePlayers()) {
			playerLocations.add(player.getLocation());
		}

		// Check the list of mobs from the spawner to see if any should be disposed of
		Iterator<MobInfo> iter = spawnerInfo.iterator();
		while (iter.hasNext()) {
			MobInfo mobInfo = iter.next();

			if (mobInfo.mMob != null) {
				if (mobInfo.mMob.isDead() || !mobInfo.mMob.isValid()) {
					iter.remove();
					continue;
				}

				// Get rid of the mob if it's been 30+ seconds since having a target
				if (mobInfo.mLastTarget == null && mobInfo.mMob.getTicksLived() - mobInfo.mTickLastTargeted > INACTIVITY_TIMER) {
					Location mobLocation = mobInfo.mMob.getLocation();
					boolean remove = true;
					for (Location loc : playerLocations) {
						if (mobLocation.distanceSquared(loc) < PLAYER_CHECK_RADIUS_SQUARED) {
							remove = false;
							break;
						}
					}

					if (remove) {
						iter.remove();
						mobInfo.mMob.remove();
					}
				}
			}
		}

		// Same object in both maps, updating one updates the other; one map is used with the target event, the other with the spawn event
		MobInfo mobInfo = new MobInfo(mob);
		spawnerInfo.add(mobInfo);
		mMobInfos.put(mob.getUniqueId(), mobInfo);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void entityTargetEvent(EntityTargetEvent event) {
		// Update the mob's last target whenever it targets or untargets a mob
		MobInfo mobInfo = mMobInfos.get(event.getEntity().getUniqueId());
		if (mobInfo != null) {
			mobInfo.mLastTarget = event.getTarget();
			mobInfo.mTickLastTargeted = mobInfo.mMob.getTicksLived();
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void chunkUnloadEvent(ChunkUnloadEvent event) {
		// Mark the mob as unloaded by setting the entity object to null
		for (Entity entity : event.getChunk().getEntities()) {
			MobInfo mobInfo = mMobInfos.get(entity.getUniqueId());
			if (mobInfo != null) {
				mobInfo.mMob = null;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void chunkLoadEvent(ChunkLoadEvent event) {
		// Mark the mob as loaded by setting the entity object to the newly generated one
		for (Entity entity : event.getChunk().getEntities()) {
			MobInfo mobInfo = mMobInfos.get(entity.getUniqueId());
			if (mobInfo != null) {
				mobInfo.mMob = entity;
			}
		}
	}

}
