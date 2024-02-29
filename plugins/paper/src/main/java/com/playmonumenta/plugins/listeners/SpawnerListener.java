package com.playmonumenta.plugins.listeners;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.delves.DelvesManager;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.spawners.SpawnerActionManager;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.SpawnerUtils;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
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

	private @Nullable String mRecentlyBrokenSpawnerLosPool = null;

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

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void spawnerSpawnEvent(SpawnerSpawnEvent event) {
		if (event.getEntity() instanceof LivingEntity mob) {
			List<MobInfo> spawnerInfo = mSpawnerInfos.computeIfAbsent(event.getSpawner().getLocation(), k -> new ArrayList<>());

			// Generate list of player locations a single time
			List<Location> playerLocations = new ArrayList<Location>();
			for (Player player : mob.getWorld().getPlayers()) {
				playerLocations.add(player.getLocation());
			}

			// Check the list of mobs from the spawner to see if any should be disposed of
			// or, if the spawner has a LoS Pool attached, replace it with a mob from the pool first
			String poolName = SpawnerUtils.getLosPool(event.getSpawner().getBlock());
			LoSPool losPool = LoSPool.EMPTY;
			boolean hasPool = false;
			if (poolName != null) {
				hasPool = true;
				losPool = new LoSPool.LibraryPool(poolName);
			}

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
			if (hasPool) {
				DelvesManager.setForcedReferenceToSpawner(event.getSpawner());
				Entity spawnedEntity = losPool.spawn(mob.getLocation());
				if (spawnedEntity instanceof LivingEntity livingEntity) {
					// Persistent mobs don't need to be tracked
					if (mob.getRemoveWhenFarAway()) {
						Location loc = event.getSpawner().getLocation();
						MMLog.fine(() -> "SpawnerListener: Not tracking persistent mob from spawner at " + loc.getWorld().getName() + "(" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + "): " + mob.getUniqueId());
					} else {
						MMLog.fine(() -> "SpawnerListener: Started tracking mob: " + mob.getUniqueId());
						mMobInfos.put(livingEntity.getUniqueId(), new MobInfo(livingEntity));
					}
				} else {
					DelvesManager.setForcedReferenceToSpawner(null);
				}
				event.setCancelled(true);
			} else {
				// Persistent mobs don't need to be tracked
				if (mob.getRemoveWhenFarAway()) {
					Location loc = event.getSpawner().getLocation();
					MMLog.fine(() -> "SpawnerListener: Not tracking persistent mob from spawner at " + loc.getWorld().getName() + "(" + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + "): " + mob.getUniqueId());
				} else {
					MMLog.fine(() -> "SpawnerListener: Started tracking mob: " + mob.getUniqueId());
					MobInfo mobInfo = new MobInfo(mob);
					spawnerInfo.add(mobInfo);
					mMobInfos.put(mob.getUniqueId(), mobInfo);
				}
			}
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
		if (event.getEntity() instanceof LivingEntity) {
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
			UUID mobUuid = mob.getUniqueId();
			@Nullable MobInfo mobInfo = mMobInfos.get(mobUuid);
			if (mobInfo != null) {
				MMLog.fine(() -> "SpawnerListener: Marked mob as despawned: " + mobUuid);
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
			UUID mobUuid = mob.getUniqueId();
			@Nullable MobInfo mobInfo = mMobInfos.get(mobUuid);
			if (mobInfo != null) {
				MMLog.fine(() -> "SpawnerListener: Updated mob with new entity: " + mobUuid);
				mobInfo.mDespawned = false;
				mobInfo.mMob = new WeakReference<>(mob);
				mobInfo.mUUID = event.getEntity().getUniqueId();
			}
		}

		// Shielded spawner markers
		if (event.getEntity() instanceof Marker marker) {
			SpawnerUtils.startSpawnerEffectsDisplay(marker);
		}
	}

	// Prevent block explosions (for example respawn anchors) from breaking special spawners.
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void blockExplodeEvent(BlockExplodeEvent event) {
		event.blockList().removeIf(block -> SpawnerUtils.isSpawner(block) && (SpawnerUtils.getShields(block) > 0 || SpawnerUtils.getBreakActionIdentifiers(block).size() > 0));
	}

	// Prevent entity explosions from breaking special spawners.
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void entityExplodeEvent(EntityExplodeEvent event) {
		event.blockList().removeIf(block -> SpawnerUtils.isSpawner(block) && (SpawnerUtils.getShields(block) > 0 || SpawnerUtils.getBreakActionIdentifiers(block).size() > 0));
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		Block block = event.getBlock();

		if (!SpawnerUtils.isSpawner(block)) {
			return;
		}

		if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
			SpawnerUtils.removeEffectsDisplayMarker(block);
			return;
		}

		int damage = 1 + Plugin.getInstance().mItemStatManager.getEnchantmentLevel(event.getPlayer(), EnchantmentType.DRILLING);
		int shieldsBefore = SpawnerUtils.getShields(block);
		boolean brokeSpawner = SpawnerUtils.tryBreakSpawner(block, damage);
		int shieldsAfter = SpawnerUtils.getShields(block);

		if (!brokeSpawner) {
			event.setCancelled(true);
		} else {
			mRecentlyBrokenSpawnerLosPool = SpawnerUtils.getLosPool(block);
		}

		Location blockLoc = BlockUtils.getCenterBlockLocation(block);

		if (SpawnerUtils.hasShieldsAttribute(block)) {
			if (shieldsBefore != 0 && shieldsAfter == 0) {
				doShieldFullBreakAnimation(blockLoc);
			} else {
				doShieldBreakAnimation(blockLoc, shieldsAfter);
			}
		}
	}

	// Only fires if the event has not been cancelled by anything else prior.
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockWasBrokenEvent(BlockBreakEvent event) {
		Block block = event.getBlock();

		if (!SpawnerUtils.isSpawner(block)) {
			return;
		}

		if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) {
			return;
		}

		SpawnerUtils.removeEffectsDisplayMarker(block);
		SpawnerActionManager.triggerActions(SpawnerUtils.getBreakActionIdentifiers(block), event.getPlayer(), block, mRecentlyBrokenSpawnerLosPool);
	}

	private void doShieldBreakAnimation(Location blockLoc, int shields) {
		if (shields == 0) {
			return;
		}

		blockLoc.getWorld().playSound(blockLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 0.6f, 2f);
		blockLoc.getWorld().playSound(blockLoc, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 0.6f, 2f);
		blockLoc.getNearbyPlayers(15).forEach(player ->
			ParticleUtils.drawSevenSegmentNumber(
				shields, blockLoc.clone().add(0, 1.5, 0),
				player, 0.65, 0.5, Particle.REDSTONE, new Particle.DustOptions(Color.AQUA, 1f)
			)
		);
	}

	private void doShieldFullBreakAnimation(Location blockLoc) {
		SpawnerUtils.removeEffectsDisplayMarker(blockLoc.getBlock());
		blockLoc.getWorld().playSound(blockLoc, Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.HOSTILE, 0.6f, 2f);
		new PPCircle(Particle.FLAME, blockLoc, 1).rotateDelta(true).delta(1, 0, 0)
			.directionalMode(true).extra(0.1).countPerMeter(4).distanceFalloff(16).spawnFull();
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockPlaceEvent(BlockPlaceEvent event) {
		Block block = event.getBlock();

		if (!SpawnerUtils.isSpawner(block)) {
			return;
		}

		Player player = event.getPlayer();
		ItemStack item = event.getItemInHand();

		int shields = SpawnerUtils.getShields(item);
		if (shields > 0) {
			player.sendMessage(Component.text("Placed a spawner with " + shields + " shields.", NamedTextColor.GOLD));
			SpawnerUtils.setShields(block, shields);
		}

		String losPool = SpawnerUtils.getLosPool(item);
		if (losPool != null) {
			player.sendMessage(Component.text("Placed a spawner with " + losPool + " LoS Pool.", NamedTextColor.GOLD));
			SpawnerUtils.setLosPool(block, losPool);
		}

		List<String> breakActions = SpawnerUtils.getBreakActionIdentifiers(item);
		if (breakActions.size() > 0) {
			player.sendMessage(Component.text("Placed a spawner with " + breakActions.size() + " break action(s):", NamedTextColor.GOLD));
			player.sendMessage(Component.text(SpawnerUtils.getBreakActionIdentifiers(item).toString(), NamedTextColor.GOLD));
			SpawnerUtils.transferBreakActionList(item, block);
		}

		if (shields > 0 || losPool != null || breakActions.size() > 0) {
			SpawnerUtils.addEffectsDisplayMarker(block);
		}
	}

}
