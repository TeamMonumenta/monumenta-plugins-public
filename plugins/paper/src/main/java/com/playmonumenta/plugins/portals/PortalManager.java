package com.playmonumenta.plugins.portals;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.MMLog;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Rotation;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class PortalManager implements Listener {
	public static final Set<Material> TRANSPARENT_BLOCKS_1 = Set.of(
		Material.LIGHT_BLUE_STAINED_GLASS,
		Material.PURPLE_STAINED_GLASS,
		Material.AIR,
		Material.WATER,
		Material.LAVA,
		Material.VINE,
		Material.LIGHT
	);
	public static final Set<Material> TRANSPARENT_BLOCKS_2 = Set.of(
		Material.ORANGE_STAINED_GLASS,
		Material.PURPLE_STAINED_GLASS,
		Material.AIR,
		Material.WATER,
		Material.LAVA,
		Material.VINE,
		Material.LIGHT
	);
	public static final long LOG_SIZE_LIMIT = 1_000_000;
	public static final String MAP_TAG = "PortalMap";

	private static @Nullable PortalManager INSTANCE = null;
	public static final Map<Player, Portal> mPlayerPortal1 = new HashMap<>();
	public static final Map<Player, Portal> mPlayerPortal2 = new HashMap<>();
	public static final Map<Player, PortalTeleportCheck> mPortalTeleportChecks = new HashMap<>();
	public static final Map<Player, PortalAFKCheck> mPortalAFKChecks = new HashMap<>();
	public static final Map<UUID, Map<Long, Set<Portal>>> mPortalsByChunk = new HashMap<>();
	public static final Set<UUID> mPortalUuids = new HashSet<>();
	public static String mCurrentShard = ServerProperties.getShardName();

	//Timer for portals to despawn after placing
	public static final int PORTAL_AFK_TIMER = 6000;

	private PortalManager() {
		INSTANCE = this;
	}

	public static PortalManager getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new PortalManager();
		}
		return INSTANCE;
	}

	public static void spawnPortal(Player player, int portalNum, int gunId) {
		portalLog("enter spawnPortal(" + player.getName() + ", " + portalNum + ", " + gunId + ");");

		//Raycast player eye to block face
		Location loc = player.getEyeLocation();
		BoundingBox box = BoundingBox.of(loc, 0.10, 0.10, 0.10);
		Vector dir = loc.getDirection();
		dir = dir.multiply(.5);
		box.shift(dir);
		World world = player.getWorld();

		Block blockHit = null;
		if (portalNum == 1) {
			blockHit = player.getTargetBlock(TRANSPARENT_BLOCKS_1, 50);
		} else if (portalNum == 2) {
			blockHit = player.getTargetBlock(TRANSPARENT_BLOCKS_2, 50);
		}

		BlockFace targetFace = player.getTargetBlockFace(50);

		if (blockHit == null) {
			portalLog("exit spawnPortal: X - no block hit");
			return;
		}
		//Check invalid block
		boolean failed = false;
		if (blockHit.getType() != Material.SMOOTH_STONE) {
			failed = true;
		} else if (!blockHit.getType().isSolid()) {
			failed = true;
		}

		for (int i = 0; i < 50; i++) {
			box.shift(dir);
			Location bLoc = box.getCenter().toLocation(world);
			if (mCurrentShard.startsWith("portal")) {
				world.spawnParticle(Particle.REDSTONE, bLoc, 3, .15, .15, .15, getDustOptions(portalNum + (2 * gunId)));
			} else if (portalNum == 1) {
				world.spawnParticle(Particle.REDSTONE, bLoc, 3, .15, .15, .15, new Particle.DustOptions(Color.fromRGB(91, 187, 255), 1.0f));
			} else {
				world.spawnParticle(Particle.REDSTONE, bLoc, 3, .15, .15, .15, new Particle.DustOptions(Color.fromRGB(255, 69, 0), 1.0f));
			}
			world.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, .5f, 1f);
			if (bLoc.getBlock().getType().isSolid()) {
				break;
			}
		}
		if (failed) {
			portalLog("exit spawnPortal: X - hit block is not valid");
			return;
		}
		//Calculate player primary direction
		BlockFace playerFacing;
		if (Math.abs(dir.getX()) >= Math.abs(dir.getZ())) {
			if (dir.getX() > 0) {
				playerFacing = BlockFace.EAST;
			} else {
				playerFacing = BlockFace.WEST;
			}
		} else {
			if (dir.getZ() > 0) {
				playerFacing = BlockFace.SOUTH;
			} else {
				playerFacing = BlockFace.NORTH;
			}
		}

		//Check adjacent block
		Block adjacent;

		if (targetFace != BlockFace.UP && targetFace != BlockFace.DOWN) {
			adjacent = world.getBlockAt(blockHit.getLocation().add(new Vector(0, 1, 0)));
		} else if (targetFace == BlockFace.UP) {
			//Set adjacent for up and down faces
			if (playerFacing == BlockFace.SOUTH) {
				adjacent = world.getBlockAt(blockHit.getLocation().clone().add(new Vector(0, 0, 1)));
			} else if (playerFacing == BlockFace.NORTH) {
				adjacent = world.getBlockAt(blockHit.getLocation().clone().add(new Vector(0, 0, -1)));
			} else if (playerFacing == BlockFace.EAST) {
				adjacent = world.getBlockAt(blockHit.getLocation().clone().add(new Vector(1, 0, 0)));
			} else {
				adjacent = world.getBlockAt(blockHit.getLocation().clone().add(new Vector(-1, 0, 0)));
			}
		} else {
			if (playerFacing == BlockFace.NORTH) {
				adjacent = world.getBlockAt(blockHit.getLocation().clone().add(new Vector(0, 0, 1)));
			} else if (playerFacing == BlockFace.SOUTH) {
				adjacent = world.getBlockAt(blockHit.getLocation().clone().add(new Vector(0, 0, -1)));
			} else if (playerFacing == BlockFace.WEST) {
				adjacent = world.getBlockAt(blockHit.getLocation().clone().add(new Vector(1, 0, 0)));
			} else {
				adjacent = world.getBlockAt(blockHit.getLocation().clone().add(new Vector(-1, 0, 0)));
			}
		}

		//Check invalid block
		if (adjacent.getType() != Material.SMOOTH_STONE) {
			portalLog("exit spawnPortal: X - adjacent block is not valid");
			return;
		} else if (!adjacent.getType().isSolid()) {
			portalLog("exit spawnPortal: X - adjacent block is not valid");
			return;
		}
		//Get the open blocks to place the portal on
		Block portalBlock1;
		Block portalBlock2;

		if (targetFace == BlockFace.UP) {
			portalBlock1 = world.getBlockAt(blockHit.getLocation().clone().add(new Vector(0, 1, 0)));
			portalBlock2 = world.getBlockAt(adjacent.getLocation().clone().add(new Vector(0, 1, 0)));
		} else if (targetFace == BlockFace.DOWN) {
			portalBlock1 = world.getBlockAt(blockHit.getLocation().clone().add(new Vector(0, -1, 0)));
			portalBlock2 = world.getBlockAt(adjacent.getLocation().clone().add(new Vector(0, -1, 0)));
		} else if (targetFace == BlockFace.EAST) {
			portalBlock1 = world.getBlockAt(blockHit.getLocation().clone().add(new Vector(1, 0, 0)));
			portalBlock2 = world.getBlockAt(adjacent.getLocation().clone().add(new Vector(1, 0, 0)));
		} else if (targetFace == BlockFace.WEST) {
			portalBlock1 = world.getBlockAt(blockHit.getLocation().clone().add(new Vector(-1, 0, 0)));
			portalBlock2 = world.getBlockAt(adjacent.getLocation().clone().add(new Vector(-1, 0, 0)));
		} else if (targetFace == BlockFace.NORTH) {
			portalBlock1 = world.getBlockAt(blockHit.getLocation().clone().add(new Vector(0, 0, -1)));
			portalBlock2 = world.getBlockAt(adjacent.getLocation().clone().add(new Vector(0, 0, -1)));
		} else if (targetFace == BlockFace.SOUTH) {
			portalBlock1 = world.getBlockAt(blockHit.getLocation().clone().add(new Vector(0, 0, 1)));
			portalBlock2 = world.getBlockAt(adjacent.getLocation().clone().add(new Vector(0, 0, 1)));
		} else {
			portalLog("exit spawnPortal: X - unknown direction");
			return;
		}

		if (portalBlock1.getType() != Material.AIR || portalBlock2.getType() != Material.AIR) {
			portalLog("exit spawnPortal: X - a portal block is not air");
			return;
		}
		//Check if the desired spots are occupied by other portal
		ArrayList<Location> occupiedLocations = new ArrayList<>();
		if (mPlayerPortal1.values().size() > 0) {
			for (Portal p : mPlayerPortal1.values()) {
				if (p != null) {
					occupiedLocations.add(p.mLocation1);
					occupiedLocations.add(p.mLocation2);
				}
			}
		}
		if (mPlayerPortal2.values().size() > 0) {
			for (Portal p : mPlayerPortal2.values()) {
				if (p != null) {
					occupiedLocations.add(p.mLocation1);
					occupiedLocations.add(p.mLocation2);
				}
			}
		}

		//Check to see if there is already a portal at desired location
		if (occupiedLocations.contains(portalBlock1.getLocation()) || occupiedLocations.contains(portalBlock2.getLocation())) {
			portalLog("exit spawnPortal: X - a portal is already present");
			return;
		}

		//Change rotation if needed for up and down blocks
		final Rotation rotation1;
		final Rotation rotation2;
		if (targetFace == BlockFace.UP && playerFacing == BlockFace.SOUTH) {
			rotation1 = Rotation.NONE;
			rotation2 = Rotation.CLOCKWISE;
		} else if (targetFace == BlockFace.DOWN && playerFacing == BlockFace.SOUTH) {
			rotation1 = Rotation.NONE;
			rotation2 = Rotation.CLOCKWISE;
		} else if (targetFace == BlockFace.UP && playerFacing == BlockFace.EAST) {
			rotation1 = Rotation.CLOCKWISE_135;
			rotation2 = Rotation.CLOCKWISE_45;
		} else if (targetFace == BlockFace.UP && playerFacing == BlockFace.WEST) {
			rotation1 = Rotation.CLOCKWISE_45;
			rotation2 = Rotation.CLOCKWISE_135;
		} else if (targetFace == BlockFace.DOWN && playerFacing == BlockFace.EAST) {
			rotation1 = Rotation.CLOCKWISE_45;
			rotation2 = Rotation.CLOCKWISE_135;
		} else if (targetFace == BlockFace.DOWN && playerFacing == BlockFace.WEST) {
			rotation1 = Rotation.CLOCKWISE_135;
			rotation2 = Rotation.CLOCKWISE_45;
		} else {
			rotation1 = Rotation.CLOCKWISE;
			rotation2 = Rotation.NONE;
		}

		//Destroy old portal
		Portal portal1 = mPlayerPortal1.get(player);
		Portal portal2 = mPlayerPortal2.get(player);

		if (portal1 != null && portalNum == 1) {
			clearPortal(player, 1);
		} else if (portal2 != null && portalNum == 2) {
			clearPortal(player, 2);
		}


		//Summon the item frames
		//Replace map ID with the maps for the current shard
		int mapNum = portalNum + 2 * gunId + getMapNum(mCurrentShard);

		Location location1 = portalBlock1.getLocation();
		Location location2 = portalBlock2.getLocation();
		ItemStack mapItem = new ItemStack(Material.FILLED_MAP, 1);
		mapItem.editMeta(itemMeta -> {
			if (itemMeta instanceof MapMeta mapMeta) {
				// The Bukkit team can learn to deal with this. There's no other way to set an existing map ID.
				mapMeta.setMapId(mapNum);
			}
		});
		Entity map1 = world.spawn(location1, GlowItemFrame.class, itemFrame -> {
			itemFrame.setFacingDirection(targetFace);
			itemFrame.setRotation(rotation1);
			itemFrame.setItem(mapItem.clone(), false);
			itemFrame.setVisible(false);
			itemFrame.setFixed(true);
			itemFrame.addScoreboardTag(MAP_TAG);
		});
		Entity map2 = world.spawn(location2, GlowItemFrame.class, itemFrame -> {
			itemFrame.setFacingDirection(targetFace);
			itemFrame.setRotation(rotation2);
			itemFrame.setItem(mapItem, false);
			itemFrame.setVisible(false);
			itemFrame.setFixed(true);
			itemFrame.addScoreboardTag(MAP_TAG);
		});
		UUID uuid1 = map1.getUniqueId();
		UUID uuid2 = map2.getUniqueId();

		//Store portals
		Map<Long, Set<Portal>> worldPortalsByChunk = mPortalsByChunk.computeIfAbsent(world.getUID(), k -> new HashMap<>());
		if (portalNum == 1) {
			portal1 = new Portal(portalNum, uuid1, uuid2, location1, location2, targetFace, blockHit.getLocation(), adjacent.getLocation());
			portal1.mOwner = player;
			portal1.mPair = portal2;
			if (portal2 != null) {
				portal2.mPair = portal1;
			}
			for (Location occupiedLoc : portal1.occupiedLocations()) {
				long chunkKey = occupiedLoc.getChunk().getChunkKey();
				worldPortalsByChunk.computeIfAbsent(chunkKey, k -> new HashSet<>()).add(portal1);
			}
		} else {
			portal2 = new Portal(portalNum, uuid1, uuid2, location1, location2, targetFace, blockHit.getLocation(), adjacent.getLocation());
			portal2.mOwner = player;
			portal2.mPair = portal1;
			if (portal1 != null) {
				portal1.mPair = portal2;
			}
			for (Location occupiedLoc : portal2.occupiedLocations()) {
				long chunkKey = occupiedLoc.getChunk().getChunkKey();
				worldPortalsByChunk.computeIfAbsent(chunkKey, k -> new HashSet<>()).add(portal2);
			}
		}
		mPortalUuids.add(uuid1);
		mPortalUuids.add(uuid2);

		mPlayerPortal1.put(player, portal1);
		mPlayerPortal2.put(player, portal2);

		//Activate teleport logic
		if (portal1 != null && portal2 != null) {
			if (mPortalTeleportChecks.get(player) == null) {
				PortalTeleportCheck ptc = new PortalTeleportCheck(player);
				ptc.runTaskTimer(Plugin.getInstance(), 0, 1);
				mPortalTeleportChecks.put(player, ptc);
			}
		}

		//Cancel last check
		PortalAFKCheck oldCheck = mPortalAFKChecks.get(player);
		if (oldCheck != null) {
			oldCheck.cancel();
		}
		//Start afk check timer for the portal
		PortalAFKCheck check = new PortalAFKCheck(player);
		check.runTaskLater(Plugin.getInstance(), PORTAL_AFK_TIMER);
		mPortalAFKChecks.put(player, check);
		portalLog("exit spawnPortal: + spawned portal");
	}

	public static void clearAllPortals(Player player) {
		clearPortal(player, 1);
		clearPortal(player, 2);
	}

	public static void clearPortal(Player player, int portalNum) {
		//Don't do anything if the manager isn't loaded
		if (mPlayerPortal1 == null || mPlayerPortal2 == null) {
			return;
		}
		portalLog("enter clearPortal(" + player.getName() + ", " + portalNum + ");");

		if (portalNum == 1) {
			Portal portal = mPlayerPortal1.get(player);
			if (portal != null) {
				deletePortal(portal);
			}
		} else if (portalNum == 2) {
			Portal portal = mPlayerPortal2.get(player);
			if (portal != null) {
				deletePortal(portal);
			}
		}
		portalLog("exit clearPortal: cleared portal object (but did we clear the map(s)?)");
	}

	protected static void deletePortal(Portal portal) {
		if (portal.mOwner != null) {
			portalLog("enter deletePortal(<" + portal.mOwner.getName() + ", " + portal.mPortalNum + ">);");
		} else {
			portalLog("enter deletePortal(<NULL PLAYER, " + portal.mPortalNum + ">);");
		}
		PortalTeleportCheck teleportCheck = mPortalTeleportChecks.get(portal.mOwner);
		if (teleportCheck != null) {
			teleportCheck.cancel();
			mPortalTeleportChecks.remove(portal.mOwner);
		} else {
			portalLog("   - deletePortal: no PortalTeleportCheck object found!");
		}

		if (portal.mPortalNum == 1) {
			if (mPlayerPortal1.remove(portal.mOwner) == null) {
				portalLog("   - deletePortal: Player did not have portal 1!");
			}
		} else {
			if (mPlayerPortal2.remove(portal.mOwner) == null) {
				portalLog("   - deletePortal: Player did not have portal 2!");
			}
		}

		if (portal.mPair != null) {
			if (portal.mPair.mPair == null) {
				portalLog("   - deletePortal: pair's pair was null!");
			}
			portal.mPair.mPair = null;
		}

		Location loc1 = portal.mLocation1;
		Location loc2 = portal.mLocation2;

		World world = loc1.getWorld();
		UUID worldId = world.getUID();

		Map<Long, Set<Portal>> worldPortalsByChunk = mPortalsByChunk.get(worldId);
		if (worldPortalsByChunk == null) {
			portalLog("   - deletePortal: no worldPortalsByChunk!");
		} else {
			for (Location occupiedLoc : portal.occupiedLocations()) {
				long chunkKey = Chunk.getChunkKey(occupiedLoc);
				Set<Portal> chunkPortals = worldPortalsByChunk.get(chunkKey);
				if (chunkPortals == null) {
					portalLog("   - deletePortal: no chunkPortals for " + occupiedLoc + "!");
				} else {
					if (!chunkPortals.remove(portal)) {
						portalLog("   - deletePortal: portal " + occupiedLoc + " not in chunkPortals!");
					}
					if (chunkPortals.isEmpty()) {
						worldPortalsByChunk.remove(chunkKey);
					}
				}
			}

			if (worldPortalsByChunk.isEmpty()) {
				mPortalsByChunk.remove(worldId);
			}
		}

		deletePortalMap(portal.mUuid1, loc1);
		deletePortalMap(portal.mUuid2, loc2);
		portalLog("exit deletePortal: removed references to portal");
	}

	private static void deletePortalMap(UUID uuid, Location loc) {
		portalLog("enter deletePortalMap(" + uuid + ");");
		mPortalUuids.remove(uuid);
		Entity itemFrame = Bukkit.getEntity(uuid);
		if (itemFrame != null) {
			portalLog("- deletePortalMap: Found at " + itemFrame.getLocation());
			itemFrame.remove();
		} else {
			portalLog("Failed to delete portal map; uuid=" + uuid + ", location=" + loc);
		}
		portalLog("exit deletePortalMap");
	}

	public static int getMapNum(String shard) {
		return switch (shard) {
			case "dev1" -> 1;
			case "valley" -> 421;
			case "isles" -> 204;
			case "build" -> 277;
			default ->
				//Dungeon ids
				15;
		};
	}

	private static Particle.DustOptions getDustOptions(int portalNum) {
		return switch (portalNum) {
			case 1 -> new Particle.DustOptions(Color.fromRGB(253, 255, 54), 1.0f);
			case 2 -> new Particle.DustOptions(Color.fromRGB(0, 222, 60), 1.0f);
			case 3 -> new Particle.DustOptions(Color.fromRGB(255, 1, 0), 1.0f);
			case 4 -> new Particle.DustOptions(Color.fromRGB(209, 85, 0), 1.0f);
			case 5 -> new Particle.DustOptions(Color.fromRGB(176, 75, 213), 1.0f);
			case 6 -> new Particle.DustOptions(Color.fromRGB(61, 175, 227), 1.0f);
			case 7 -> new Particle.DustOptions(Color.fromRGB(237, 125, 163), 1.0f);
			case 8 -> new Particle.DustOptions(Color.fromRGB(122, 85, 50), 1.0f);
			default -> new Particle.DustOptions(Color.fromRGB(91, 187, 255), 1.0f);
		};
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entitiesUnloadEvent(EntitiesUnloadEvent event) {
		// Delete portals in unloading chunks
		if (mPortalsByChunk != null) {
			Chunk chunk = event.getChunk();
			UUID worldId = chunk.getWorld().getUID();
			Map<Long, Set<Portal>> worldPortalsByChunk = mPortalsByChunk.get(worldId);
			if (worldPortalsByChunk != null) {
				Set<Portal> liveChunkPortals = worldPortalsByChunk.get(chunk.getChunkKey());
				if (liveChunkPortals != null && !liveChunkPortals.isEmpty()) {
					portalLog("enter entitiesUnloadEvent(<" + chunk.getX() + ", " + chunk.getZ() + ">);");
					Set<Portal> chunkPortals = new HashSet<>(liveChunkPortals);
					for (Portal portal : chunkPortals) {
						Player owner = portal.mOwner;
						if (owner == null) {
							deletePortal(portal);
						} else {
							clearPortal(owner, portal.mPortalNum);
						}
					}
					portalLog("exit entitiesUnloadEvent");
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entitiesLoadEvent(EntitiesLoadEvent event) {
		// Remove assumed-unloaded portal maps
		if (mPortalUuids != null) {
			Chunk chunk = event.getChunk();
			boolean foundEntity = false;
			for (Entity entity : chunk.getEntities()) {
				if (!entity.getScoreboardTags().contains(MAP_TAG)) {
					continue;
				}
				if (!mPortalUuids.contains(entity.getUniqueId())) {
					if (!foundEntity) {
						portalLog("enter entitiesLoadEvent(<" + chunk.getX() + ", " + chunk.getZ() + ">);");
					}
					foundEntity = true;
					portalLog("- entitiesLoadEvent: removed escapee portal map " + entity.getLocation());
					entity.remove();
				}
			}
			if (foundEntity) {
				portalLog("exit entitiesLoadEvent");
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		handleBlockEvent(event.getBlock());
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockDestroyEvent(BlockDestroyEvent event) {
		handleBlockEvent(event.getBlock());
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockDispenseEvent(BlockDispenseEvent event) {
		handleBlockEvent(event.getBlock());
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockExplodeEvent(BlockExplodeEvent event) {
		handleBlockEvent(event.getBlock());
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockFadeEvent(BlockFadeEvent event) {
		handleBlockEvent(event.getBlock());
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockFromToEvent(BlockFromToEvent event) {
		handleBlockEvent(event.getBlock());
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockGrowEvent(BlockGrowEvent event) {
		handleBlockEvent(event.getBlock());
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockIgniteEvent(BlockIgniteEvent event) {
		handleBlockEvent(event.getBlock());
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockPistonExtendEvent(BlockPistonExtendEvent event) {
		handleBlockEvent(event.getBlock());
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockPistonRetractEvent(BlockPistonRetractEvent event) {
		handleBlockEvent(event.getBlock());
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockPlaceEvent(BlockPlaceEvent event) {
		handleBlockEvent(event.getBlock());
	}

	public void handleBlockEvent(Block block) {
		// Delete portals affected by block events
		if (mPortalsByChunk != null) {
			Location loc = block.getLocation();
			Chunk chunk = loc.getChunk();
			UUID worldId = chunk.getWorld().getUID();
			Map<Long, Set<Portal>> worldPortalsByChunk = mPortalsByChunk.get(worldId);
			if (worldPortalsByChunk != null) {
				Set<Portal> liveChunkPortals = worldPortalsByChunk.get(chunk.getChunkKey());
				if (liveChunkPortals != null && !liveChunkPortals.isEmpty()) {
					portalLog("Chunk with block change contains portal!");
					Set<Portal> chunkPortals = new HashSet<>(liveChunkPortals);
					for (Portal portal : chunkPortals) {
						if (portal.occupiedLocations().contains(loc)) {
							Player owner = portal.mOwner;
							if (owner == null) {
								deletePortal(portal);
							} else {
								clearPortal(owner, portal.mPortalNum);
							}
						}
					}
				}
			}
		}
	}

	private static void portalLog(String message) {
		Plugin plugin = Plugin.getInstance();
		if (plugin == null) {
			return;
		}
		File logFile = new File(plugin.getDataFolder(), "portals.log");
		boolean failed = false;
		try {
			if (!logFile.exists()) {
				try {
					if (!logFile.createNewFile()) {
						failed = true;
					}
				} catch (IOException e) {
					failed = true;
				}
			}
			if (failed) {
				MMLog.severe("Could not create portals.log");
				return;
			}
			if (logFile.length() > LOG_SIZE_LIMIT) {
				File rotatedLogFile = new File(plugin.getDataFolder(), "portals.log.1");
				if (rotatedLogFile.exists()) {
					if (!rotatedLogFile.delete()) {
						MMLog.severe("Could not delete portals.log.1 for log rotation");
						return;
					}
				}
				if (!logFile.renameTo(rotatedLogFile)) {
					MMLog.severe("Could not rotate portals.log to portals.log.1");
					return;
				}
				try {
					if (!logFile.createNewFile()) {
						failed = true;
					}
				} catch (IOException e) {
					failed = true;
				}
				if (failed) {
					MMLog.severe("Could not create portals.log after rotation");
					return;
				}
			}

			OutputStreamWriter writer;
			try {
				writer = new OutputStreamWriter(new FileOutputStream(logFile, true), StandardCharsets.UTF_8);
			} catch (FileNotFoundException e) {
				MMLog.severe("Could not create writer for portals.log");
				return;
			}

			LocalDateTime dateTime = LocalDateTime.now(ZoneId.systemDefault());
			String timestamp = dateTime.toString();

			String fullClassName = Thread.currentThread().getStackTrace()[2].getClassName();
			String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
			String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
			int lineNumber = Thread.currentThread().getStackTrace()[2].getLineNumber();
			String lineInfo = className + "." + methodName + "():" + lineNumber;

			String logLine = "[" + timestamp + "] " + lineInfo + ": " + message + "\n";

			try {
				writer.write(logLine);
				writer.close();
			} catch (IOException e) {
				MMLog.severe("Could not write to portals.log");
			}
		} catch (SecurityException ex) {
			MMLog.severe("A security exception occurred with portals.log - how?");
		}
	}
}
