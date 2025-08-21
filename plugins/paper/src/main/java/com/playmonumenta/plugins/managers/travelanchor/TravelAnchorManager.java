package com.playmonumenta.plugins.managers.travelanchor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TravelAnchorManager implements Listener {
	public static final String TRAVEL_ANCHOR_TAG = "TravelAnchor";
	public static final @NotNull NamespacedKey TRAVEL_ANCHOR_PDC_KEY
		= Objects.requireNonNull(NamespacedKey.fromString("monumenta:travel_anchor"));

	private static @Nullable TravelAnchorManager INSTANCE = null;
	private static final Map<UUID, WorldTravelAnchors> mWorlds = new HashMap<>();

	private TravelAnchorManager() {
		for (World world : Bukkit.getWorlds()) {
			mWorlds.put(world.getUID(), new WorldTravelAnchors(world));
		}
	}

	public static TravelAnchorManager getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new TravelAnchorManager();
		}
		return INSTANCE;
	}

	public @Nullable EntityTravelAnchor newAnchor(Entity entity) {
		World world = entity.getWorld();
		UUID worldId = world.getUID();
		WorldTravelAnchors worldAnchors = mWorlds.computeIfAbsent(worldId, k -> new WorldTravelAnchors(world));
		return worldAnchors.newAnchor(entity);
	}

	public void removeAnchor(Entity entity) {
		World world = entity.getWorld();
		UUID worldId = world.getUID();
		WorldTravelAnchors worldAnchors = mWorlds.computeIfAbsent(worldId, k -> new WorldTravelAnchors(world));
		worldAnchors.removeAnchor(entity);
	}

	public void unloadAnchor(Entity entity) {
		World world = entity.getWorld();
		UUID worldId = world.getUID();
		WorldTravelAnchors worldAnchors = mWorlds.computeIfAbsent(worldId, k -> new WorldTravelAnchors(world));
		worldAnchors.unloadAnchor(entity);
	}

	public WorldTravelAnchors anchorsInWorld(World world) {
		UUID worldId = world.getUID();
		return mWorlds.computeIfAbsent(worldId, k -> new WorldTravelAnchors(world));
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void worldLoadEvent(WorldLoadEvent event) {
		World world = event.getWorld();
		mWorlds.put(world.getUID(), new WorldTravelAnchors(world));
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void entitiesLoadEvent(EntitiesLoadEvent event) {
		World world = event.getWorld();
		UUID worldId = world.getUID();
		WorldTravelAnchors worldAnchors = mWorlds.computeIfAbsent(worldId, k -> new WorldTravelAnchors(world));
		for (Entity entity : event.getEntities()) {
			worldAnchors.loadAnchor(entity);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void entityTeleportEvent(EntityTeleportEvent event) {
		Location toLoc = event.getTo();
		if (toLoc == null) {
			return;
		}
		World toWorld = toLoc.getWorld();
		UUID toWorldId = toWorld.getUID();
		World fromWorld = event.getFrom().getWorld();
		UUID fromWorldId = fromWorld.getUID();

		Entity entity = event.getEntity();

		if (toWorldId.equals(fromWorldId)) {
			WorldTravelAnchors worldAnchors = mWorlds.computeIfAbsent(fromWorldId, k -> new WorldTravelAnchors(fromWorld));
			worldAnchors.updateAnchor(entity);
			return;
		}

		WorldTravelAnchors fromWorldAnchors = mWorlds.computeIfAbsent(fromWorldId, k -> new WorldTravelAnchors(fromWorld));
		WorldTravelAnchors toWorldAnchors = mWorlds.computeIfAbsent(toWorldId, k -> new WorldTravelAnchors(toWorld));

		fromWorldAnchors.unloadAnchor(entity);
		toWorldAnchors.loadAnchor(entity);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void worldSaveEvent(WorldSaveEvent event) {
		World world = event.getWorld();
		UUID worldId = world.getUID();
		WorldTravelAnchors worldAnchors = mWorlds.computeIfAbsent(worldId, k -> new WorldTravelAnchors(world));
		worldAnchors.updateAnchors();
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void entityDeathEvent(EntityDeathEvent event) {
		Entity entity = event.getEntity();
		World world = entity.getWorld();
		UUID worldId = world.getUID();
		WorldTravelAnchors worldAnchors = mWorlds.computeIfAbsent(worldId, k -> new WorldTravelAnchors(world));
		worldAnchors.removeAnchor(entity);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void entitiesUnloadEvent(EntitiesUnloadEvent event) {
		World world = event.getWorld();
		UUID worldId = world.getUID();
		WorldTravelAnchors worldAnchors = mWorlds.computeIfAbsent(worldId, k -> new WorldTravelAnchors(world));

		for (Entity entity : event.getEntities()) {
			worldAnchors.updateAnchor(entity);
			worldAnchors.unloadAnchor(entity);
		}
	}

	// Doesn't trigger EntitiesUnloadEvent for remaining chunks
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void worldUnloadEvent(WorldUnloadEvent event) {
		World world = event.getWorld();
		UUID worldId = world.getUID();
		WorldTravelAnchors worldAnchors = mWorlds.computeIfAbsent(worldId, k -> new WorldTravelAnchors(world));
		worldAnchors.updateAnchors();
		mWorlds.remove(worldId);
	}
}
