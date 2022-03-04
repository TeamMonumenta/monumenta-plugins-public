package com.playmonumenta.plugins.portals;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.CommandUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class PortalManager {
	public static final Set<Material> TRANSPARENT_BLOCKS_1 = Set.of(
		Material.LIGHT_BLUE_STAINED_GLASS,
		Material.PURPLE_STAINED_GLASS,
		Material.AIR,
		Material.WATER,
		Material.LAVA,
		Material.VINE
	);
	public static final Set<Material> TRANSPARENT_BLOCKS_2 = Set.of(
		Material.ORANGE_STAINED_GLASS,
		Material.PURPLE_STAINED_GLASS,
		Material.AIR,
		Material.WATER,
		Material.LAVA,
		Material.VINE
	);

	public static Portal mPortal1 = null;
	public static Portal mPortal2 = null;
	public static Map<Player, Portal> mPlayerPortal1 = null;
	public static Map<Player, Portal> mPlayerPortal2 = null;
	public static Map<Player, PortalTeleportCheck> mPortalTeleportChecks = null;
	public static Map<Player, PortalAFKCheck> mPortalAFKChecks = null;
	public static String mCurrentShard = ServerProperties.getShardName();

	//Timer for portals to despawn after placing
	public static final int PORTAL_AFK_TIMER = 6000;

	public static void spawnPortal(Player player, int portalNum) {
		//Setup map
		if (mPortalTeleportChecks == null || mPlayerPortal1 == null || mPlayerPortal2 == null) {
			mPlayerPortal1 = new HashMap<Player, Portal>();
			mPlayerPortal2 = new HashMap<Player, Portal>();
			mPortalTeleportChecks = new HashMap<Player, PortalTeleportCheck>();
			mPortalAFKChecks = new HashMap<Player, PortalAFKCheck>();
		}

		//Raycast player eye to block face
		Location loc = player.getEyeLocation();
		BoundingBox box = BoundingBox.of(loc, 0.10, 0.10, 0.10);
		Vector dir = loc.getDirection();
		dir = dir.multiply(.5);
		box.shift(dir);
		World mWorld = player.getWorld();

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
			Location bLoc = box.getCenter().toLocation(mWorld);
			if (portalNum == 1) {
				mWorld.spawnParticle(Particle.REDSTONE, bLoc, 3, .15, .15, .15, new Particle.DustOptions(Color.fromRGB(91, 187, 255), 1.0f));
			} else {
				mWorld.spawnParticle(Particle.REDSTONE, bLoc, 3, .15, .15, .15, new Particle.DustOptions(Color.fromRGB(255, 69, 0), 1.0f));
			}
			mWorld.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, .5f, 1f);
			if (bLoc.getBlock().getType().isSolid()) {
				break;
			}
		}
		//Calculate player primary direction
		BlockFace playerFacing = null;
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
			adjacent = mWorld.getBlockAt(blockHit.getLocation().add(new Vector(0, 1, 0)));
		} else if (targetFace == BlockFace.UP) {
			//Set adjacent for up and down faces
			if (playerFacing == BlockFace.SOUTH) {
				adjacent = mWorld.getBlockAt(blockHit.getLocation().clone().add(new Vector(0, 0, 1)));
			} else if (playerFacing == BlockFace.NORTH) {
				adjacent = mWorld.getBlockAt(blockHit.getLocation().clone().add(new Vector(0, 0, -1)));
			} else if (playerFacing == BlockFace.EAST) {
				adjacent = mWorld.getBlockAt(blockHit.getLocation().clone().add(new Vector(1, 0, 0)));
			} else if (playerFacing == BlockFace.WEST) {
				adjacent = mWorld.getBlockAt(blockHit.getLocation().clone().add(new Vector(-1, 0, 0)));
			}
		} else if (targetFace == BlockFace.DOWN) {
			if (playerFacing == BlockFace.NORTH) {
				adjacent = mWorld.getBlockAt(blockHit.getLocation().clone().add(new Vector(0, 0, 1)));
			} else if (playerFacing == BlockFace.SOUTH) {
				adjacent = mWorld.getBlockAt(blockHit.getLocation().clone().add(new Vector(0, 0, -1)));
			} else if (playerFacing == BlockFace.WEST) {
				adjacent = mWorld.getBlockAt(blockHit.getLocation().clone().add(new Vector(1, 0, 0)));
			} else if (playerFacing == BlockFace.EAST) {
				adjacent = mWorld.getBlockAt(blockHit.getLocation().clone().add(new Vector(-1, 0, 0)));
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
		int faceId = 0;

		if (targetFace == BlockFace.UP) {
			portalBlock1 = mWorld.getBlockAt(blockHit.getLocation().clone().add(new Vector(0, 1, 0)));
			portalBlock2 = mWorld.getBlockAt(adjacent.getLocation().clone().add(new Vector(0, 1, 0)));
			faceId = 1;
		} else if (targetFace == BlockFace.DOWN) {
			portalBlock1 = mWorld.getBlockAt(blockHit.getLocation().clone().add(new Vector(0, -1, 0)));
			portalBlock2 = mWorld.getBlockAt(adjacent.getLocation().clone().add(new Vector(0, -1, 0)));
			faceId = 0;
		} else if (targetFace == BlockFace.EAST) {
			portalBlock1 = mWorld.getBlockAt(blockHit.getLocation().clone().add(new Vector(1, 0, 0)));
			portalBlock2 = mWorld.getBlockAt(adjacent.getLocation().clone().add(new Vector(1, 0, 0)));
			faceId = 5;
		} else if (targetFace == BlockFace.WEST) {
			portalBlock1 = mWorld.getBlockAt(blockHit.getLocation().clone().add(new Vector(-1, 0, 0)));
			portalBlock2 = mWorld.getBlockAt(adjacent.getLocation().clone().add(new Vector(-1, 0, 0)));
			faceId = 4;
		} else if (targetFace == BlockFace.NORTH) {
			portalBlock1 = mWorld.getBlockAt(blockHit.getLocation().clone().add(new Vector(0, 0, -1)));
			portalBlock2 = mWorld.getBlockAt(adjacent.getLocation().clone().add(new Vector(0, 0, -1)));
			faceId = 2;
		} else if (targetFace == BlockFace.SOUTH) {
			portalBlock1 = mWorld.getBlockAt(blockHit.getLocation().clone().add(new Vector(0, 0, 1)));
			portalBlock2 = mWorld.getBlockAt(adjacent.getLocation().clone().add(new Vector(0, 0, 1)));
			faceId = 3;
		}

		if (portalBlock1 == null || portalBlock1.getType() != Material.AIR || portalBlock2 == null || portalBlock2.getType() != Material.AIR || portalBlock1.getState() instanceof ItemFrame) {
			return;
		}
		//Check if the desired spots are occupied by other portal
		ArrayList<Location> occupiedLocations = new ArrayList<Location>();
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
		int rotation1 = 2;
		int rotation2 = 0;
		if (targetFace == BlockFace.UP && playerFacing == BlockFace.SOUTH) {
			rotation1 = 0;
			rotation2 = 2;
		} else if (targetFace == BlockFace.DOWN && playerFacing == BlockFace.SOUTH) {
			rotation1 = 0;
			rotation2 = 2;
		} else if (targetFace == BlockFace.UP && playerFacing == BlockFace.EAST) {
			rotation1 = 3;
			rotation2 = 1;
		} else if (targetFace == BlockFace.UP && playerFacing == BlockFace.WEST) {
			rotation1 = 1;
			rotation2 = 3;
		} else if (targetFace == BlockFace.DOWN && playerFacing == BlockFace.EAST) {
			rotation1 = 1;
			rotation2 = 3;
		} else if (targetFace == BlockFace.DOWN && playerFacing == BlockFace.WEST) {
			rotation1 = 3;
			rotation2 = 1;
		}

		//Destroy old portal
		mPortal1 = mPlayerPortal1.get(player);
		mPortal2 = mPlayerPortal2.get(player);

		if (mPortal1 != null && portalNum == 1) {
			CommandUtils.runCommandViaConsole("execute positioned " + mPortal1.mLocation1.toCenterLocation().getX() + " " + mPortal1.mLocation1.toCenterLocation().getY() + " " + mPortal1.mLocation1.toCenterLocation().getZ() + " run kill @e[type=item_frame,distance=..0.8]");
			CommandUtils.runCommandViaConsole("execute positioned " + mPortal1.mLocation2.toCenterLocation().getX() + " " + mPortal1.mLocation2.toCenterLocation().getY() + " " + mPortal1.mLocation2.toCenterLocation().getZ() + " run kill @e[type=item_frame,distance=..0.8]");
		} else if (mPortal2 != null && portalNum == 2) {
			CommandUtils.runCommandViaConsole("execute positioned " + mPortal2.mLocation1.toCenterLocation().getX() + " " + mPortal2.mLocation1.toCenterLocation().getY() + " " + mPortal2.mLocation1.toCenterLocation().getZ() + " run kill @e[type=item_frame,distance=..0.8]");
			CommandUtils.runCommandViaConsole("execute positioned " + mPortal2.mLocation2.toCenterLocation().getX() + " " + mPortal2.mLocation2.toCenterLocation().getY() + " " + mPortal2.mLocation2.toCenterLocation().getZ() + " run kill @e[type=item_frame,distance=..0.8]");
		}

		//Store portals
		if (portalNum == 1) {
			mPortal1 = new Portal(portalBlock1.getLocation(), portalBlock2.getLocation(), targetFace, blockHit.getLocation(), adjacent.getLocation());
			mPortal1.mOwner = player;
			Portal other = mPlayerPortal2.get(player);
			mPortal1.mPair = other;
			if (other != null) {
				other.mPair = mPortal1;
			}
		} else if (portalNum == 2) {
			mPortal2 = new Portal(portalBlock1.getLocation(), portalBlock2.getLocation(), targetFace, blockHit.getLocation(), adjacent.getLocation());
			mPortal2.mOwner = player;
			Portal other = mPlayerPortal1.get(player);
			mPortal2.mPair = other;
			if (other != null) {
				other.mPair = mPortal2;
			}
		}

		mPlayerPortal1.put(player, mPortal1);
		mPlayerPortal2.put(player, mPortal2);

		//Summon the item frames
		//Replace map ID with the maps for the current shard
		int mapNum = portalNum + getMapNum(mCurrentShard);

		if (portalNum == 1) {
			CommandUtils.runCommandViaConsole("summon item_frame " + mPortal1.mLocation1.getX() + " " + mPortal1.mLocation1.getY() + " " + mPortal1.mLocation1.getZ() + " {Facing:" + faceId + "b,ItemRotation:" + rotation1 + "b,Invisible:1b,Item:{id:\"minecraft:filled_map\",Count:1b,tag:{map:" + mapNum + "}}}");
			CommandUtils.runCommandViaConsole("summon item_frame " + mPortal1.mLocation2.getX() + " " + mPortal1.mLocation2.getY() + " " + mPortal1.mLocation2.getZ() + " {Facing:" + faceId + "b,ItemRotation:" + rotation2 + "b,Invisible:1b,Item:{id:\"minecraft:filled_map\",Count:1b,tag:{map:" + mapNum + "}}}");
		} else if (portalNum == 2) {
			CommandUtils.runCommandViaConsole("summon item_frame " + mPortal2.mLocation1.getX() + " " + mPortal2.mLocation1.getY() + " " + mPortal2.mLocation1.getZ() + " {Facing:" + faceId + "b,ItemRotation:" + rotation1 + "b,Invisible:1b,Item:{id:\"minecraft:filled_map\",Count:1b,tag:{map:" + mapNum + "}}}");
			CommandUtils.runCommandViaConsole("summon item_frame " + mPortal2.mLocation2.getX() + " " + mPortal2.mLocation2.getY() + " " + mPortal2.mLocation2.getZ() + " {Facing:" + faceId + "b,ItemRotation:" + rotation2 + "b,Invisible:1b,Item:{id:\"minecraft:filled_map\",Count:1b,tag:{map:" + mapNum + "}}}");
		}

		//Activate teleport logic
		if (mPortal1 != null && mPortal2 != null) {
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
		if (mPortalTeleportChecks == null || mPlayerPortal1 == null || mPlayerPortal2 == null) {
			return;
		}

		Portal p = null;
		if (portalNum == 1) {
			if (mPlayerPortal1.get(player) != null) {
				p = mPlayerPortal1.get(player);
				mPlayerPortal1.remove(player);
			}

		} else if (portalNum == 2) {
			if (mPlayerPortal2.get(player) != null) {
				p = mPlayerPortal2.get(player);
				mPlayerPortal2.remove(player);
			}
		}
		if (mPortalTeleportChecks.get(player) != null) {
			mPortalTeleportChecks.get(player).cancel();
			mPortalTeleportChecks.remove(player);
		}
		//Kill item frames
		if (p != null) {
			CommandUtils.runCommandViaConsole("execute positioned " + p.mLocation1.toCenterLocation().getX() + " " + p.mLocation1.toCenterLocation().getY() + " " + p.mLocation1.toCenterLocation().getZ() + " run kill @e[type=item_frame,distance=..0.8]");
			CommandUtils.runCommandViaConsole("execute positioned " + p.mLocation2.toCenterLocation().getX() + " " + p.mLocation2.toCenterLocation().getY() + " " + p.mLocation2.toCenterLocation().getZ() + " run kill @e[type=item_frame,distance=..0.8]");
		}
	}

	public static int getMapNum(String shard) {
		switch (shard) {
		case "dev1":
			return 1;
		case "valley":
			return 421;
		case "isles":
			return 204;
		case "build":
			return 277;
		default:
			//Dungeon ids
			return 439;
		}
	}
}
