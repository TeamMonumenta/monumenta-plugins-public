package com.playmonumenta.plugins.listeners;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class SpawnerListener implements Listener {
	private static final int PLAYER_LOGOUT_MOB_PERSIST_RADIUS = 20;
	private static final int PLAYER_LOGOUT_MOB_PERSIST_TICKS = Constants.TEN_MINUTES;

	private static class MobInfo {
		private WeakReference<LivingEntity> mMob;
		private UUID mUUID;
		private boolean mDespawned = false;
		private boolean mHasTarget = false;
		private int mTickLastTargeted = -1;
		private int mPersistentUntil = -1;

		public MobInfo(LivingEntity mob) {
			mMob = new WeakReference<>(mob);
			mUUID = mob.getUniqueId();
		}

		public @Nullable LivingEntity getMob() {
			return mMob.get();
		}

		public boolean hasTarget() {
			return mHasTarget;
		}

		public boolean isDespawned() {
			return mDespawned;
		}

		public UUID getUniqueId() {
			return mUUID;
		}

		/*
		 * Updates the mob's persistent state based on the mPersistentUntil field
		 *   Used when a player logs out near a tracked mob
		 *
		 * returns true if mob is still persistent, false if not
		 */
		public boolean checkUpdatePersistent() {
			if (mPersistentUntil > 0) {
				@Nullable LivingEntity mob = getMob();
				if (mob != null && mob.getTicksLived() > mPersistentUntil) {
					MMLog.fine(() -> "SpawnerListener: Mob persistence removed after time elapsed: " + mob.getUniqueId());
					mob.setRemoveWhenFarAway(true);
					mPersistentUntil = -1;
					return false;
				}
				return true;
			}
			return false;
		}

		@Override
		public String toString() {
			@Nullable LivingEntity mob = getMob();
			return "mob=" + (mob == null ? "null                                " : mob.getUniqueId()) +
				" mDespawned=" + mDespawned + " mHasTarget=" + mHasTarget + " mTickLastTargeted=" + mTickLastTargeted +
				" mPersistentUntil=" + mPersistentUntil;
		}
	}

	private static final int PLAYER_CHECK_RADIUS_SQUARED = 5 * 5;
	private static final int INACTIVITY_TIMER = 20 * 45;
	private static final int CLEANER_INTERVAL = 20 * 30;

	private final Map<UUID, MobInfo> mMobInfos = new HashMap<>();
	/*
	 * Need to use the full spawner Location, not just x/y/z, to ensure that only this
	 * specific spawner on this specific world is included
	 */
	private final Map<Location, List<MobInfo>> mSpawnerInfos = new HashMap<>();

	private final BukkitRunnable mCleaner;

	public SpawnerListener(Plugin plugin) {
		mCleaner = new BukkitRunnable() {
			@Override
			public void run() {
				// Removes mob info entries from the map if mob is dead or invalid
				if (mMobInfos.size() > 0) {
					MMLog.finer("SpawnerListener: mMobInfos current entries:");
				}
				Iterator<MobInfo> mobInfoIter = mMobInfos.values().iterator();
				while (mobInfoIter.hasNext()) {
					MobInfo info = mobInfoIter.next();
					@Nullable LivingEntity mob = info.getMob();

					MMLog.finer(() -> "SpawnerListener:    " + info.toString());

					// If the mob has NOT despawned but is dead or was removed, remove this tracker
					if (!info.checkUpdatePersistent() && !info.isDespawned() && (mob == null || mob.isDead() || !mob.isValid())) {
						MMLog.fine(() -> "SpawnerListener: Removing non-persistent, non-despawned dead mob from mMobInfos: " + info.getUniqueId());
						mobInfoIter.remove();
					}
				}

				// Removes spawner mob list entries from the map if mob list for spawner is empty
				if (mSpawnerInfos.size() > 0) {
					MMLog.finer("SpawnerListener: mSpawnerInfos current entries:");
				}
				Iterator<Map.Entry<Location, List<MobInfo>>> spawnerInfoIter = mSpawnerInfos.entrySet().iterator();
				while (spawnerInfoIter.hasNext()) {
					Map.Entry<Location, List<MobInfo>> entry = spawnerInfoIter.next();
					List<MobInfo> spawnerInfo = entry.getValue();

					Iterator<MobInfo> spawnerListIter = spawnerInfo.iterator();
					while (spawnerListIter.hasNext()) {
						MobInfo info = spawnerListIter.next();
						@Nullable LivingEntity mob = info.getMob();

						Location loc = entry.getKey();
						MMLog.finer(() -> "SpawnerListener:    " + loc.getWorld().getName() + "(" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + ") " + info.toString());

						if (!info.checkUpdatePersistent() && !info.isDespawned() && (mob == null || mob.isDead() || !mob.isValid())) {
							MMLog.fine(() -> "SpawnerListener: Removing non-persistent, non-despawned dead mob from mSpawnerInfos: " + info.getUniqueId());
							spawnerListIter.remove();
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

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void spawnerSpawnEvent(SpawnerSpawnEvent event) {
		if (event.getEntity() instanceof LivingEntity mob) {
			// Do not handle persistent mobs spawned from spawners
			if (!mob.getRemoveWhenFarAway()) {
				Location loc = event.getSpawner().getLocation();
				MMLog.fine(() -> "SpawnerListener: Ignoring persistent mob from spawner at " + loc.getWorld().getName() + "(" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + "): " + mob.getUniqueId());
				return;
			}

			List<MobInfo> spawnerInfo = mSpawnerInfos.computeIfAbsent(event.getSpawner().getLocation(), k -> new ArrayList<>());

			// Generate list of player locations a single time
			List<Location> playerLocations = new ArrayList<Location>();
			for (Player player : mob.getWorld().getPlayers()) {
				playerLocations.add(player.getLocation());
			}

			// Check the list of mobs from the spawner to see if any should be disposed of
			Iterator<MobInfo> iter = spawnerInfo.iterator();
			while (iter.hasNext()) {
				MobInfo mobInfo = iter.next();

				@Nullable LivingEntity innerMob = mobInfo.getMob();
				if (innerMob == null || innerMob.isDead() || !innerMob.isValid()) {
					MMLog.fine(() -> "SpawnerListener: Removed dead/invalid mob from spawnerInfo on new spawn: " + mobInfo.getUniqueId());
					iter.remove();
					mMobInfos.remove(mobInfo.getUniqueId());
					continue;
				}

				// Get rid of the mob if it's been 45 seconds since having a target
				if (!mobInfo.hasTarget() && (innerMob.getTicksLived() - mobInfo.mTickLastTargeted > INACTIVITY_TIMER)) {
					Location mobLocation = innerMob.getLocation();
					boolean remove = true;
					for (Location loc : playerLocations) {
						if (mobLocation.distanceSquared(loc) < PLAYER_CHECK_RADIUS_SQUARED) {
							remove = false;
							break;
						}
					}

					if (remove) {
						MMLog.fine(() -> "SpawnerListener: Removed mob from world due to anti buildup inactivity: " + innerMob.getUniqueId());
						iter.remove();
						mMobInfos.remove(innerMob.getUniqueId());
						innerMob.remove();
					}
				}
			}

			// Same object in both maps, updating one updates the other; one map is used with the target event, the other with the spawn event
			MMLog.fine(() -> "SpawnerListener: Started tracking mob: " + mob.getUniqueId());
			MobInfo mobInfo = new MobInfo(mob);
			spawnerInfo.add(mobInfo);
			mMobInfos.put(mob.getUniqueId(), mobInfo);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), PLAYER_LOGOUT_MOB_PERSIST_RADIUS)) {
			// Note that we don't check for existing persistence here - only that the mob was in the spawner tracker to begin with,
			// which only happens when the mob spawns from a spawner and is not persistent
			// So - this system can extend the persistence of an already persistent mob
			@Nullable MobInfo info = mMobInfos.get(mob.getUniqueId());

			if (info != null) {
				MMLog.fine(() -> "SpawnerListener: Marking mob persistent due to nearby player logout: " + mob.getUniqueId());
				mob.setRemoveWhenFarAway(false);
				info.mPersistentUntil = mob.getTicksLived() + PLAYER_LOGOUT_MOB_PERSIST_TICKS;
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void worldUnloadEvent(WorldUnloadEvent event) {
		// Remove all entries tracking things in unloaded worlds
		mSpawnerInfos.keySet().removeIf((loc) -> loc.getWorld().equals(event.getWorld()));
		mMobInfos.values().removeIf((info) -> {
			@Nullable LivingEntity mob = info.getMob();
			if (mob != null && mob.getWorld().equals(event.getWorld())) {
				MMLog.fine(() -> "SpawnerListener: Unloaded mob due to world unload: " + mob.getUniqueId());
				return true;
			}
			return false;
		});
	}

	// handles cancelled events because this is used to test for mob activity only
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void entityTargetEvent(EntityTargetEvent event) {
		// Update the mob's last target whenever it targets or untargets a mob
		if (event.getEntity() instanceof LivingEntity mob) {
			@Nullable MobInfo mobInfo = mMobInfos.get(event.getEntity().getUniqueId());
			if (mobInfo != null) {
				mobInfo.mHasTarget = (event.getTarget() != null);
				MMLog.fine(() -> "SpawnerListener: Set mob hasTarget=" + mobInfo.hasTarget() + " : " + event.getEntity().getUniqueId());
				mobInfo.mTickLastTargeted = event.getEntity().getTicksLived();
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void entityRemoveFromWorldEvent(EntityRemoveFromWorldEvent event) {
		// This could fire for many reasons - such as the mob dying or unloading
		// Regardless of source, mark the mob as despawned
		if (event.getEntity() instanceof LivingEntity mob) {
			@Nullable MobInfo mobInfo = mMobInfos.get(event.getEntity().getUniqueId());
			if (mobInfo != null) {
				MMLog.fine(() -> "SpawnerListener: Marked mob as despawned: " + event.getEntity().getUniqueId());
				mobInfo.mDespawned = true;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void entityAddToWorldEvent(EntityAddToWorldEvent event) {
		// Mob was loaded into the world, either spawned or chunk loaded
		// If the mob was spawned, it won't be in the map or will already be up to date, so this change won't do much
		// If the mob was chunk loaded, mark it as loaded and update the reference to the new mob
		if (event.getEntity() instanceof LivingEntity mob) {
			@Nullable MobInfo mobInfo = mMobInfos.get(event.getEntity().getUniqueId());
			if (mobInfo != null) {
				MMLog.fine(() -> "SpawnerListener: Updated mob with new entity: " + event.getEntity().getUniqueId());
				mobInfo.mDespawned = false;
				mobInfo.mMob = new WeakReference<>(mob);
				mobInfo.mUUID = event.getEntity().getUniqueId();
			}
		}
	}

}
