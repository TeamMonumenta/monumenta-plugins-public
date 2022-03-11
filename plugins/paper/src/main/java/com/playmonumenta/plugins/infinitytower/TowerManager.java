package com.playmonumenta.plugins.infinitytower;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.networkrelay.NetworkRelayMessageEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.infinitytower.guis.TowerGuiFloorDesignMob;
import com.playmonumenta.plugins.infinitytower.guis.TowerGuiShowMobs;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class TowerManager implements Listener {

	protected static final Map<UUID, TowerGame> GAMES = new HashMap<>();

	public static Plugin mPlugin;


	public TowerManager(Plugin plugin) {
		mPlugin = plugin;

		String shardName = ServerProperties.getShardName();
		if (shardName.contains("valley") || shardName.contains("dev") || shardName.contains("mobs")) {
			Bukkit.getScheduler().runTaskLaterAsynchronously(mPlugin, () -> {
					TowerConstants.SHOULD_GAME_START = true;
					TowerFileUtils.loadTowerMobsInfo();
					TowerFileUtils.loadDefaultTeams();
					TowerFileUtils.loadPlayerTeams();
					TowerFileUtils.loadFloors();
					TowerGuiShowMobs.loadGuiItems();
					TowerGuiFloorDesignMob.loadGuiItems();
			}, 10);
		}

	}


	public static void unload() {
		for (Map.Entry<UUID, TowerGame> entry : new HashSet<>(GAMES.entrySet())) {
			entry.getValue().forceStopGame();
		}
		if (ServerProperties.getShardName().contains("valley")) {
			TowerFileUtils.savePlayerTower();
		}

		if (ServerProperties.getShardName().contains("mobs")) {
			TowerFileUtils.saveDefaultTower();
			TowerFileUtils.saveTowerMobs();
		}
	}




	//------------------------EVENTS-----------------------------------
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public static void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		if (player.getScoreboardTags().contains(TowerConstants.PLAYER_TAG)) {
			UUID uuid = player.getUniqueId();
			GAMES.get(uuid).forceStopGame();
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBlockChange(EntityChangeBlockEvent event) {
		//this is used for when Eldrask use GreatSword Slam
		if (event.getEntity() instanceof FallingBlock && ((FallingBlock)event.getEntity()).getBlockData().getMaterial() == Material.BLUE_ICE && event.getEntity().getScoreboardTags().contains(TowerConstants.FALLING_BLOCK_TAG)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public static void onBlockIgniteEvent(BlockIgniteEvent event) {
		if (event.getCause() == BlockIgniteEvent.IgniteCause.FIREBALL && event.getIgnitingEntity() instanceof Blaze blaze) {
			if (blaze.getScoreboardTags().contains(TowerConstants.MOB_TAG)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public static void onProjectileHitEvent(ProjectileHitEvent event) {
		if (event.getEntity().getShooter() instanceof LivingEntity mob) {
			if (mob.getScoreboardTags().contains(TowerConstants.MOB_TAG) && event.getHitEntity() == null) {
				event.setCancelled(true);
			}
		}
	}

	//-----------NetworkRelayAPI stuff to send towerupdate--------------------

	public static void broadcastUpdateTower(JsonObject newTeamFloor) {
		try {
			NetworkRelayAPI.sendBroadcastMessage("com.playmonumenta.plugins.infinitytower.updatetower", newTeamFloor);
		} catch (Exception e) {
			mPlugin.getLogger().warning("[TowerManager] can't send broadcast update tower. Reason : " + e.getMessage());
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void networkRelayMessageEvent(@NotNull NetworkRelayMessageEvent event) throws Exception {
		if (event.getChannel().equals("com.playmonumenta.plugins.infinitytower.updatetower")) {
			JsonObject newTeamFloor = event.getData();
			TowerFileUtils.updatePlayersTower(newTeamFloor);
		}
	}


}
