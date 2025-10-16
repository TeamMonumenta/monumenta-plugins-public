package com.playmonumenta.plugins.discoveries;

import com.bergerkiller.bukkit.common.events.EntityRemoveEvent;
import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.redissync.ConfigAPI;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.RedisAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTEntity;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import io.lettuce.core.KeyValue;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.Nullable;

public class DiscoveryManager implements Listener {
	private static final String DISCOVERY_IDENTIFIER_TAG = "ItemDiscovery";
	private static final String REDIS_PLAYER_KEY = "Discoveries";
	private static final String REDIS_STORAGE_ALL_KEY = ConfigAPI.getServerDomain() + ":discoveries:listall";
	private static final String REDIS_STORAGE_DEVSHARD_KEY = ConfigAPI.getServerDomain() + ":discoveries:listdevshard"; // separated storage for dev shards
	private static final String REDIS_ID_ALL_KEY = ConfigAPI.getServerDomain() + ":discoveries:idall";
	private static final String REDIS_ID_DEVSHARD_KEY = ConfigAPI.getServerDomain() + ":discoveries:iddevshard"; // separated storage for dev shards
	private static final int DISPLAY_RANGE = 30;

	private static final String VIEW_PERMISSION = "monumenta.discovery.view";
	private static final String COLLECT_PERMISSION = "monumenta.discovery.collect";

	// Data that is currently loaded
	private static final ArrayList<ItemDiscovery> mActiveDiscoveries = new ArrayList<>();
	private static final Map<UUID, List<Integer>> mPlayerDiscoveryData = new HashMap<>();

	// fix for "already collected" message after collecting discovery
	private static final String INTERACT_METAKEY = "DiscoveryManagerInteract";

	public static void update() {
		ArrayList<ItemDiscovery> invalidatedDiscoveries = new ArrayList<>();
		for (ItemDiscovery discovery : mActiveDiscoveries) {
			// ensure the discovery is valid before proceeding
			if (discovery.getMarker() != null && discovery.getMarker().isValid()) {
				// process what to show each nearby player
				Collection<Player> nearbyPlayers = discovery.getMarker().getLocation().getNearbyPlayers(DISPLAY_RANGE);
				nearbyPlayers.removeIf(player -> !player.hasPermission(VIEW_PERMISSION));
				List<Player> collectedPlayers = nearbyPlayers.stream().filter(nearbyPlayer -> getCollectedDiscoveries(nearbyPlayer).contains(discovery.mId)).toList();

				discovery.runEffect(collectedPlayers, true);

				nearbyPlayers.removeAll(collectedPlayers);
				discovery.runEffect(nearbyPlayers.stream().toList(), false);
			} else {
				invalidatedDiscoveries.add(discovery);
			}
		}
		mActiveDiscoveries.removeAll(invalidatedDiscoveries);
	}

	private static String getRedisStorageKey() {
		return ServerProperties.getShardName().contains("dev") ? REDIS_STORAGE_DEVSHARD_KEY : REDIS_STORAGE_ALL_KEY;
	}

	private static String getRedisIdKey() {
		return ServerProperties.getShardName().contains("dev") ? REDIS_ID_DEVSHARD_KEY : REDIS_ID_ALL_KEY;
	}

	// -- Loading and unloading player data --

	@EventHandler(ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		JsonObject discoveryData = MonumentaRedisSyncAPI.getPlayerPluginData(player.getUniqueId(), REDIS_PLAYER_KEY);
		if (discoveryData != null) {
			List<Integer> collectedIds = new ArrayList<>();
			for (JsonElement id : discoveryData.getAsJsonArray("collectedIds")) {
				collectedIds.add(id.getAsInt());
			}
			mPlayerDiscoveryData.put(player.getUniqueId(), collectedIds);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void playerSaveEvent(PlayerSaveEvent event) {
		Player player = event.getPlayer();
		List<Integer> collectedIds = getCollectedDiscoveries(player);
		if (collectedIds != null) {
			JsonObject json = new JsonObject();
			JsonArray idsArray = new JsonArray();
			for (int i : collectedIds) {
				idsArray.add(i);
			}
			json.add("collectedIds", idsArray);
			event.setPluginData(REDIS_PLAYER_KEY, json);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			if (!player.isOnline()) {
				mPlayerDiscoveryData.remove(player.getUniqueId());
			}
		}, 100);
	}

	// -- Loading and unloading discoveries --

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void entityAddToWorld(EntityAddToWorldEvent event) {
		if (event.getEntity() instanceof Marker marker && marker.getScoreboardTags().contains(DISCOVERY_IDENTIFIER_TAG)) {
			tryReadDiscovery(marker);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void entityRemoveFromWorld(EntityRemoveFromWorldEvent event) {
		if (event.getEntity() instanceof Marker marker && marker.getScoreboardTags().contains(DISCOVERY_IDENTIFIER_TAG)) {
			mActiveDiscoveries.removeIf(itemDiscovery -> itemDiscovery.mMarkerUUID.equals(marker.getUniqueId()));
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void entityRemoveEvent(EntityRemoveEvent event) {
		if (event.getEntity() instanceof Marker marker && marker.isDead() && marker.getScoreboardTags().contains(DISCOVERY_IDENTIFIER_TAG)) {
			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				String uuid = marker.getUniqueId().toString();
				String discoveryString = RedisAPI.getInstance().async().hget(getRedisStorageKey(), uuid).toCompletableFuture().join();
				JsonObject discovery = new Gson().fromJson(discoveryString, JsonObject.class);

				discovery.addProperty("deleted", Instant.now().getEpochSecond());

				String data = discovery.toString();
				RedisAPI.getInstance().async().hset(getRedisStorageKey(), uuid, data).toCompletableFuture().join();
			});
		}
	}

	// Attempt to read discovery data off the marker entity and load it if valid
	private static void tryReadDiscovery(Marker marker) {
		try {
			NBTEntity entity = new NBTEntity(marker);
			NBTCompound container = entity.getPersistentDataContainer().getOrCreateCompound("discovery");

			int id = container.getInteger("id");

			ItemDiscovery.ItemDiscoveryTier tier = ItemDiscovery.ItemDiscoveryTier.valueOf(container.getString("tier"));

			String lootString = container.getString("loot");
			NamespacedKey lootKey = null;
			if (lootString != null && lootString.contains(":")) {
				String[] split = lootString.split(":", 2);
				lootKey = new NamespacedKey(split[0], split[1]);
			}
			if (lootKey == null) {
				MMLog.warning(String.format("[Discovery] Read invalid loot path when parsing Discovery at Location: [%s, %s, %s] in World: %s", marker.getLocation().getX(), marker.getLocation().getY(), marker.getLocation().getZ(), marker.getWorld().getName()));
				return;
			}

			String functionString = container.getString("function");
			NamespacedKey functionKey = null;
			if (functionString != null && functionString.contains(":")) {
				String[] split = functionString.split(":", 2);
				functionKey = new NamespacedKey(split[0], split[1]);
			}
			if (functionKey != null && !FunctionWrapper.getFunctions().contains(functionKey)) {
				MMLog.warning(String.format("[Discovery] Read invalid function path when parsing Discovery at Location: [%s, %s, %s] in World: %s", marker.getLocation().getX(), marker.getLocation().getY(), marker.getLocation().getZ(), marker.getWorld().getName()));
				functionKey = null;
			}

			ItemDiscovery discovery = new ItemDiscovery(marker, id, tier, lootKey, functionKey);

			// add to the json list if it is somehow missing or update it if present
			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				String uuid = marker.getUniqueId().toString();
				RedisAPI.getInstance().async().hset(getRedisStorageKey(), uuid, discovery.toJson().toString()).toCompletableFuture().join();
			});

			mActiveDiscoveries.add(discovery);
		} catch (Exception e) {
			MMLog.warning(String.format("[Discoveries] Failed to read Discovery at Location: [%s, %s, %s] in World: %s", marker.getLocation().getX(), marker.getLocation().getY(), marker.getLocation().getZ(), marker.getWorld().getName()));
			MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), e);
		}
	}

	// -- Interaction events --

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerAnimationEvent(PlayerAnimationEvent event) {
		if (event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
			Player player = event.getPlayer();

			RayTraceResult result = player.getWorld().rayTraceEntities(player.getEyeLocation(), player.getLocation().getDirection(), 3, 0.5, entity -> entity instanceof Marker && entity.getScoreboardTags().contains(DISCOVERY_IDENTIFIER_TAG));
			if (result != null && result.getHitEntity() instanceof Marker marker
				&& MetadataUtils.checkOnceInRecentTicks(Plugin.getInstance(), player, INTERACT_METAKEY, 5)) {
				if (!player.hasPermission(COLLECT_PERMISSION)) {
					player.sendActionBar(Component.text("You cannot currently collect this", NamedTextColor.RED));
					return;
				}

				Optional<ItemDiscovery> discoveryOptional = mActiveDiscoveries.stream().filter(discovery -> discovery.mMarkerUUID.equals(marker.getUniqueId())).findFirst();
				if (discoveryOptional.isPresent()) {
					ItemDiscovery targetedDiscovery = discoveryOptional.get();

					// check if not collected previously
					List<Integer> collectedIds = getCollectedDiscoveries(player);
					if (!collectedIds.contains(targetedDiscovery.mId)) {
						if (targetedDiscovery.giveLootToPlayer(player)) {
							// successfully given
							MMLog.info(String.format("[Discoveries] %s collected discovery with id %s", player.getName(), targetedDiscovery.mId));

							collectedIds.add(targetedDiscovery.mId);
							mPlayerDiscoveryData.replace(player.getUniqueId(), collectedIds);

							if (targetedDiscovery.mOptionalFunctionPath != null) {
								String command = String.format("execute in %s as %s at %s run function %s",
									targetedDiscovery.mWorldName,
									player.getUniqueId(),
									marker.getUniqueId(),
									targetedDiscovery.mOptionalFunctionPath);

								NmsUtils.getVersionAdapter().runConsoleCommandSilently(command);
							}

							// handle information advancement
							AdvancementUtils.grantAdvancement(player, "monumenta:handbook/money/discoveries");
						}
					} else {
						player.sendActionBar(Component.text("You have already collected this", TextColor.color(117, 117, 117)));
					}
				} else {
					MMLog.warning(String.format("[Discoveries] Player interacted with Discovery that was not loaded at Location: [%s, %s, %s] in World: %s", marker.getLocation().getX(), marker.getLocation().getY(), marker.getLocation().getZ(), marker.getWorld().getName()));
				}
			}
		}
	}

	private static List<Integer> getCollectedDiscoveries(Player player) {
		return mPlayerDiscoveryData.computeIfAbsent(player.getUniqueId(), uuid -> new ArrayList<>());
	}

	// returns the lowest unused id
	private static int getNewId() {
		return Math.toIntExact(RedisAPI.getInstance().async().incr(getRedisIdKey()).toCompletableFuture().join());
	}

	// returns the newly created discovery
	public static @Nullable ItemDiscovery createDiscovery(Location location, NamespacedKey lootPath, ItemDiscovery.ItemDiscoveryTier tier, @Nullable NamespacedKey optionalFunction) {
		try {
			Marker marker = location.getWorld().spawn(location, Marker.class);
			marker.addScoreboardTag(DISCOVERY_IDENTIFIER_TAG);
			marker.addScoreboardTag("RespawnPersistent"); // prevent the marker from being removed by respawning structures

			ItemDiscovery discovery = new ItemDiscovery(marker, getNewId(), tier, lootPath, optionalFunction);
			mActiveDiscoveries.add(discovery);
			discovery.writeDataOnMarker();

			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				String uuid = marker.getUniqueId().toString();
				RedisAPI.getInstance().async().hset(getRedisStorageKey(), uuid, discovery.toJson().toString()).toCompletableFuture().join();
			});

			return discovery;
		} catch (Exception e) {
			MMLog.warning(String.format("[Discoveries] Failed to create discovery with data [Location: %s, %s, %s, Loot table: %s, Tier: %s, Optional function: %s]",
				location.getX(),
				location.getY(),
				location.getZ(),
				lootPath.getNamespace() + ":" + lootPath.getKey(),
				tier.name(),
				optionalFunction == null ? "-" : (optionalFunction.getNamespace() + ":" + optionalFunction.getKey())));
			MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), e);

			return null;
		}
	}

	// returns whether the provided discovery was deleted
	public static boolean removeDiscovery(ItemDiscovery discovery) {
		boolean wasPresent = mActiveDiscoveries.remove(discovery);
		Marker marker = discovery.getMarker();
		if (marker != null) {
			marker.remove();
		}
		return wasPresent;
	}

	// returns the amount of discoveries deleted
	public static int removeDiscovery(int id) {
		List<ItemDiscovery> toRemove = new ArrayList<>();
		for (ItemDiscovery discovery : mActiveDiscoveries) {
			if (discovery.mId == id) {
				toRemove.add(discovery);
			}
		}
		int total = 0;
		for (ItemDiscovery discovery : toRemove) {
			if (removeDiscovery(discovery)) {
				total++;
			}
		}
		return total;
	}

	// should be executed async
	public static boolean removeDeleted(String uuid) {
		List<String> allUuids = RedisAPI.getInstance().async().hkeys(getRedisStorageKey()).toCompletableFuture().join();

		if (!allUuids.contains(uuid)) {
			return false;
		}

		RedisAPI.getInstance().async().hdel(getRedisStorageKey(), uuid).toCompletableFuture().join();
		return true;
	}

	// returns whether the provided player's data was updated
	public static boolean setPlayerCollected(Player player, int id, boolean setCollected) {
		if (mPlayerDiscoveryData.containsKey(player.getUniqueId())) {
			// player is currently loaded
			List<Integer> collected = getCollectedDiscoveries(player);

			boolean updated = false;
			if (setCollected) {
				if (!collected.contains(id)) {
					collected.add(id);
					updated = true;
				}
			} else {
				updated = collected.removeIf(i -> i == id);
			}

			mPlayerDiscoveryData.replace(player.getUniqueId(), collected);
			return updated;
		} else {
			// player is not currently loaded - either offline or error
			return false;
		}
	}

	// returns the list of all ids that the provided player has collected
	public static @Nullable List<Integer> getPlayerCollected(Player player) {
		if (mPlayerDiscoveryData.containsKey(player.getUniqueId())) {
			return getCollectedDiscoveries(player);
		} else {
			// player is not currently loaded - either offline or error
			return null;
		}
	}

	// returns the discovery nearest to the provided location
	public static @Nullable ItemDiscovery getNearestToLocation(Location location) {
		List<ItemDiscovery> validDiscoveries = new ArrayList<>();
		for (ItemDiscovery discovery : mActiveDiscoveries) {
			if (discovery.mWorldName.equals(location.getWorld().getKey().asString())) {
				validDiscoveries.add(discovery);
			}
		}
		if (!validDiscoveries.isEmpty()) {
			validDiscoveries.sort(Comparator.comparingDouble(o -> o.mLocation.distance(location)));
			return validDiscoveries.get(0);
		}

		return null;
	}

	public static List<ItemDiscovery> getDiscoveriesInRange(Location location, double range) {
		return mActiveDiscoveries.stream().filter(discovery -> discovery.mLocation.distance(location) < range).toList();
	}

	// returns a list of all loaded discoveries with the provided id
	public static List<ItemDiscovery> getById(int id) {
		List<ItemDiscovery> discoveries = new ArrayList<>();
		for (ItemDiscovery discovery : mActiveDiscoveries) {
			if (discovery.mId == id) {
				discoveries.add(discovery);
			}
		}
		return discoveries;
	}

	// should be executed async
	public static boolean setNextId(int nextId) {
		try {
			RedisAPI.getInstance().async().set(getRedisIdKey(), String.valueOf(nextId - 1)).toCompletableFuture().join();

			return true;
		} catch (Exception e) {
			MMLog.warning("[Discoveries] Failed to update next id to " + nextId);
			MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), e);

			return false;
		}
	}

	// attempt to update the matching entry in the json list
	// should be executed async
	private static void updateJsonList(ItemDiscovery discovery) {
		String uuid = discovery.mMarkerUUID.toString();
		RedisAPI.getInstance().async().hset(getRedisStorageKey(), uuid, discovery.toJson().toString()).toCompletableFuture().join();
	}

	// returns whether the provided discovery was updated
	public static boolean setNewId(ItemDiscovery discovery, int newId) {
		if (discovery.mId == newId) {
			return false;
		}
		discovery.mId = newId;
		discovery.writeDataOnMarker();
		updateJsonList(discovery);
		return true;
	}

	// returns the amount of discoveries with the provided id updated
	public static int setNewId(int id, int newId) {
		int total = 0;
		for (ItemDiscovery discovery : mActiveDiscoveries) {
			if (discovery.mId == id) {
				if (setNewId(discovery, newId)) {
					total++;
				}
			}
		}
		return total;
	}

	// returns whether the provided discovery was updated
	public static boolean setNewLoot(ItemDiscovery discovery, NamespacedKey newLootTablePath) {
		if (discovery.mLootTablePath.equals(newLootTablePath)) {
			return false;
		}
		discovery.mLootTablePath = newLootTablePath;
		discovery.writeDataOnMarker();
		updateJsonList(discovery);
		return true;
	}

	// returns the amount of discoveries with the provided id updated
	public static int setNewLoot(int id, NamespacedKey newLootTablePath) {
		int total = 0;
		for (ItemDiscovery discovery : mActiveDiscoveries) {
			if (discovery.mId == id) {
				if (setNewLoot(discovery, newLootTablePath)) {
					total++;
				}
			}
		}
		return total;
	}

	// returns whether the provided discovery was updated
	public static boolean setNewTier(ItemDiscovery discovery, ItemDiscovery.ItemDiscoveryTier newTier) {
		if (discovery.mTier.equals(newTier)) {
			return false;
		}
		discovery.mTier = newTier;
		discovery.writeDataOnMarker();
		updateJsonList(discovery);
		return true;
	}

	// returns the amount of discoveries with the provided id updated
	public static int setNewTier(int id, ItemDiscovery.ItemDiscoveryTier newTier) {
		int total = 0;
		for (ItemDiscovery discovery : mActiveDiscoveries) {
			if (discovery.mId == id) {
				if (setNewTier(discovery, newTier)) {
					total++;
				}
			}
		}
		return total;
	}

	// returns whether the provided discovery was updated
	public static boolean setNewFunction(ItemDiscovery discovery, @Nullable NamespacedKey newOptionalFunctionPath) {
		if ((discovery.mOptionalFunctionPath == null && newOptionalFunctionPath == null)
			|| (!(discovery.mOptionalFunctionPath == null || newOptionalFunctionPath == null) && discovery.mOptionalFunctionPath.equals(newOptionalFunctionPath))) {
			return false;
		}
		discovery.mOptionalFunctionPath = newOptionalFunctionPath;
		discovery.writeDataOnMarker();
		updateJsonList(discovery);
		return true;
	}

	// returns the amount of discoveries with the provided id updated
	public static int setNewFunction(int id, @Nullable NamespacedKey newOptionalFunctionPath) {
		int total = 0;
		for (ItemDiscovery discovery : mActiveDiscoveries) {
			if (discovery.mId == id) {
				if (setNewFunction(discovery, newOptionalFunctionPath)) {
					total++;
				}
			}
		}
		return total;
	}

	public static List<ItemDiscovery> getAllLoadedDiscoveries() {
		return new ArrayList<>(mActiveDiscoveries);
	}

	// should be executed async
	public static @Nullable List<JsonObject> getAllDiscoveries() {
		try {
			List<String> uuidKeys = RedisAPI.getInstance().async().hkeys(getRedisStorageKey()).toCompletableFuture().join();

			// getting redis data if empty throws an error
			if (uuidKeys.isEmpty()) {
				return new ArrayList<>();
			}

			List<KeyValue<String, String>> pairedData = RedisAPI.getInstance().async().hmget(getRedisStorageKey(), uuidKeys.toArray(new String[0])).toCompletableFuture().join();

			Gson gson = new Gson();
			List<JsonObject> discoveryData = new ArrayList<>();
			for (KeyValue<String, String> pair : pairedData) {
				discoveryData.add(gson.fromJson(pair.getValue(), JsonObject.class));
			}

			return discoveryData;
		} catch (Exception e) {
			MMLog.warning("[Discoveries] Failed to get all discoveries");
			MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), e);

			return null;
		}
	}
}
