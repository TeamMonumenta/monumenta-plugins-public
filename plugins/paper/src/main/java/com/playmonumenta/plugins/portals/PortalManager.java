package com.playmonumenta.plugins.portals;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Rotation;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

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

	private static @Nullable PortalManager INSTANCE = null;
	public static Map<Player, Portal> mPlayerPortal1 = null;
	public static Map<Player, Portal> mPlayerPortal2 = null;
	public static Map<Player, PortalTeleportCheck> mPortalTeleportChecks = null;
	public static Map<Player, PortalAFKCheck> mPortalAFKChecks = null;
	public static Map<UUID, Map<Long, Set<Portal>>> mPortalsByChunk = null;
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
		//Setup map
		if (mPortalTeleportChecks == null || mPlayerPortal1 == null || mPlayerPortal2 == null) {
			mPlayerPortal1 = new HashMap<>();
			mPlayerPortal2 = new HashMap<>();
			mPortalTeleportChecks = new HashMap<>();
			mPortalAFKChecks = new HashMap<>();
			mPortalsByChunk = new HashMap<>();
		}

		//Raycast player eye to block face
		Location loc = player.getEyeLocation();
		BoundingBox box = BoundingBox.of(loc, 0.10, 0.10, 0.10);
		Vector dir = loc.getDirection();
		dir = dir.multiply(.5);
		box.shift(dir);
		World world = player.getWorld();
		String worldName = world.getName();
		if (worldName.startsWith("Project_Epic")) {
			worldName = "overworld";
		}

		Block blockHit = null;
		if (portalNum == 1) {
			blockHit = player.getTargetBlock(TRANSPARENT_BLOCKS_1, 50);
		} else if (portalNum == 2) {
			blockHit = player.getTargetBlock(TRANSPARENT_BLOCKS_2, 50);
		}

		BlockFace targetFace = player.getTargetBlockFace(50);

		boolean valid = true;

		if (blockHit == null) {
			return;
		}
		//Check invalid block
		if (blockHit.getType() != Material.SMOOTH_STONE) {
			valid = false;
		} else if (!blockHit.getType().isSolid()) {
			valid = false;
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
			world.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, .5f, 1f);
			if (bLoc.getBlock().getType().isSolid()) {
				break;
			}
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

		if (!valid) {
			return;
		}

		//Check adjacent block
		Block adjacent = null;

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
			} else if (playerFacing == BlockFace.WEST) {
				adjacent = world.getBlockAt(blockHit.getLocation().clone().add(new Vector(-1, 0, 0)));
			}
		} else if (targetFace == BlockFace.DOWN) {
			if (playerFacing == BlockFace.NORTH) {
				adjacent = world.getBlockAt(blockHit.getLocation().clone().add(new Vector(0, 0, 1)));
			} else if (playerFacing == BlockFace.SOUTH) {
				adjacent = world.getBlockAt(blockHit.getLocation().clone().add(new Vector(0, 0, -1)));
			} else if (playerFacing == BlockFace.WEST) {
				adjacent = world.getBlockAt(blockHit.getLocation().clone().add(new Vector(1, 0, 0)));
			} else if (playerFacing == BlockFace.EAST) {
				adjacent = world.getBlockAt(blockHit.getLocation().clone().add(new Vector(-1, 0, 0)));
			}
		}

		//Check invalid block
		if (adjacent.getType() != Material.SMOOTH_STONE) {
			return;
		} else if (!adjacent.getType().isSolid()) {
			return;
		}
		//Get the open blocks to place the portal on
		Block portalBlock1 = null;
		Block portalBlock2 = null;

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
		}

		if (portalBlock1 == null || portalBlock1.getType() != Material.AIR || portalBlock2 == null || portalBlock2.getType() != Material.AIR) {
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
			deletePortal(portal1);
		} else if (portal2 != null && portalNum == 2) {
			deletePortal(portal2);
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
		});
		Entity map2 = world.spawn(location2, GlowItemFrame.class, itemFrame -> {
			itemFrame.setFacingDirection(targetFace);
			itemFrame.setRotation(rotation2);
			itemFrame.setItem(mapItem, false);
			itemFrame.setVisible(false);
			itemFrame.setFixed(true);
		});
		UUID uuid1 = map1.getUniqueId();
		UUID uuid2 = map2.getUniqueId();

		//Store portals
		if (portalNum == 1) {
			portal1 = new Portal(portalNum, uuid1, uuid2, location1, location2, targetFace, blockHit.getLocation(), adjacent.getLocation());
			portal1.mOwner = player;
			portal1.mPair = portal2;
			if (portal2 != null) {
				portal2.mPair = portal1;
			}
		} else {
			portal2 = new Portal(portalNum, uuid1, uuid2, location1, location2, targetFace, blockHit.getLocation(), adjacent.getLocation());
			portal2.mOwner = player;
			portal2.mPair = portal1;
			if (portal1 != null) {
				portal1.mPair = portal2;
			}
		}

		mPlayerPortal1.put(player, portal1);
		mPlayerPortal2.put(player, portal2);

		Map<Long, Set<Portal>> worldPortalsByChunk = mPortalsByChunk.computeIfAbsent(world.getUID(), k -> new HashMap<>());
		if (portal1 != null) {
			long chunkKey1 = location1.getChunk().getChunkKey();
			worldPortalsByChunk.computeIfAbsent(chunkKey1, k -> new HashSet<>()).add(portal1);
		}
		if (portal2 != null) {
			long chunkKey2 = location2.getChunk().getChunkKey();
			worldPortalsByChunk.computeIfAbsent(chunkKey2, k -> new HashSet<>()).add(portal2);
		}

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
	}

	public static void clearPortal(Player player, int portalNum) {
		//Don't do anything if the manager isn't loaded
		if (mPlayerPortal1 == null || mPlayerPortal2 == null) {
			return;
		}

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
	}

	protected static void deletePortal(Portal portal) {
		PortalTeleportCheck teleportCheck = mPortalTeleportChecks.get(portal.mOwner);
		if (teleportCheck != null) {
			teleportCheck.cancel();
			mPortalTeleportChecks.remove(portal.mOwner);
		}

		if (portal.mPortalNum == 1) {
			mPlayerPortal1.remove(portal.mOwner);
		} else {
			mPlayerPortal2.remove(portal.mOwner);
		}

		if (portal.mPair != null) {
			portal.mPair.mPair = null;
		}

		Location loc1 = portal.mLocation1;
		Location loc2 = portal.mLocation2;

		World world = loc1.getWorld();
		UUID worldId = world.getUID();

		Map<Long, Set<Portal>> worldPortalsByChunk = mPortalsByChunk.get(worldId);
		if (worldPortalsByChunk != null) {
			long chunkKey1 = loc1.getChunk().getChunkKey();
			long chunkKey2 = loc2.getChunk().getChunkKey();

			Set<Portal> chunkPortals = worldPortalsByChunk.get(chunkKey1);
			if (chunkPortals != null) {
				chunkPortals.remove(portal);
				if (chunkPortals.isEmpty()) {
					worldPortalsByChunk.remove(chunkKey1);
				}
			}

			if (chunkKey1 != chunkKey2) {
				chunkPortals = worldPortalsByChunk.get(chunkKey2);
				if (chunkPortals != null) {
					chunkPortals.remove(portal);
					if (chunkPortals.isEmpty()) {
						worldPortalsByChunk.remove(chunkKey2);
					}
				}
			}

			if (worldPortalsByChunk.isEmpty()) {
				mPortalsByChunk.remove(worldId);
			}
		}

		deletePortalMap(portal.mUuid1);
		deletePortalMap(portal.mUuid2);
	}

	private static void deletePortalMap(UUID uuid) {
		Entity itemFrame = Bukkit.getEntity(uuid);
		if (itemFrame != null) {
			itemFrame.remove();
		}
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
		switch (portalNum) {
			case 1:
				return new Particle.DustOptions(Color.fromRGB(253, 255, 54), 1.0f);
			case 2:
				return new Particle.DustOptions(Color.fromRGB(0, 222, 60), 1.0f);
			case 3:
				return new Particle.DustOptions(Color.fromRGB(255, 1, 0), 1.0f);
			case 4:
				return new Particle.DustOptions(Color.fromRGB(209, 85, 0), 1.0f);
			case 5:
				return new Particle.DustOptions(Color.fromRGB(176, 75, 213), 1.0f);
			case 6:
				return new Particle.DustOptions(Color.fromRGB(61, 175, 227), 1.0f);
			case 7:
				return new Particle.DustOptions(Color.fromRGB(237, 125, 163), 1.0f);
			case 8:
				return new Particle.DustOptions(Color.fromRGB(122, 85, 50), 1.0f);
			default:
				return new Particle.DustOptions(Color.fromRGB(91, 187, 255), 1.0f);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void chunkUnloadEvent(ChunkUnloadEvent event) {
		if (mPortalsByChunk != null) {
			Chunk chunk = event.getChunk();
			UUID worldId = chunk.getWorld().getUID();
			Map<Long, Set<Portal>> worldPortalsByChunk = mPortalsByChunk.get(worldId);
			if (worldPortalsByChunk != null) {
				Set<Portal> liveChunkPortals = worldPortalsByChunk.get(chunk.getChunkKey());
				if (liveChunkPortals != null) {
					Set<Portal> chunkPortals = new HashSet<>(liveChunkPortals);
					for (Portal portal : chunkPortals) {
						deletePortal(portal);
					}
				}
			}
		}
	}
}
