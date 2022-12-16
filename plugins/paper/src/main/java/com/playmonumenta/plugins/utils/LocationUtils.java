package com.playmonumenta.plugins.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.structures.StructuresPlugin;
import com.playmonumenta.structures.managers.RespawningStructure;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class LocationUtils {
	public static Vector getVectorTo(Location to, Location from) {
		Vector vFrom = from.toVector();
		Vector vTo = to.toVector();
		return vTo.subtract(vFrom);
	}

	public static Vector getDirectionTo(Location to, Location from) {
		Vector vFrom = from.toVector();
		Vector vTo = to.toVector();
		return vTo.subtract(vFrom).normalize();
	}

	public static Location getEntityCenter(Entity e) {
		return e.getLocation().add(0, e.getHeight() / 2, 0);
	}

	@Deprecated
	public static boolean isLosBlockingBlock(Material mat) {
		return BlockUtils.isLosBlockingBlock(mat);
	}

	@Deprecated
	public static boolean isPathBlockingBlock(Material mat) {
		return BlockUtils.isPathBlockingBlock(mat);
	}

	@Deprecated
	public static boolean isWaterlogged(Block block) {
		return BlockUtils.isWaterlogged(block);
	}

	@Deprecated
	public static boolean containsWater(Block block) {
		return BlockUtils.containsWater(block);
	}

	@Deprecated
	public static boolean isRail(Block block) {
		return BlockUtils.isRail(block);
	}

	public static boolean isValidMinecartLocation(Location loc) {
		Block block = loc.getBlock();
		if (BlockUtils.isRail(block)) {
			return true;
		}

		block = loc.subtract(0, 1, 0).getBlock();
		if (BlockUtils.isRail(block)) {
			return true;
		}

		/*
		 * Check up to 50 blocks underneath the location. Stop when
		 * a non-air block is hit. If it's a rail, this is allowed, otherwise it's not
		 */
		loc = loc.clone();
		for (int i = loc.getBlockY(); i > Math.max(0, loc.getBlockY() - 50); i--) {
			loc.setY(i);
			block = loc.getBlock();
			if (BlockUtils.isRail(block)) {
				return true;
			} else if (!block.isEmpty()) {
				return false;
			}
		}

		return false;
	}

	public static boolean isLocationInWater(Location loc) {
		Block block = loc.getBlock();
		return block.getType() == Material.WATER || BlockUtils.containsWater(block);
	}

	public static boolean isValidBoatLocation(Location loc) {
		/*
		 * Check up to 50 blocks underneath the location.
		 * Stop when a non-air block is hit (except for the first).
		 * If it's a liquid or ice, this is allowed, otherwise it's not.
		 */
		loc = loc.clone();
		if (ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.LAND_BOAT_POSSIBLE)) {
			return true;
		}
		for (int i = 0; i < 50; i++) {
			Block block = loc.getBlock();
			if (block.isLiquid()
				    || BlockUtils.containsWater(block)
				    || block.getType() == Material.ICE
				    || block.getType() == Material.BLUE_ICE
				    || block.getType() == Material.PACKED_ICE
				    || block.getType() == Material.FROSTED_ICE) {
				return true;
			} else if (i > 0 && !block.isEmpty()) {
				return false;
			}
			loc.subtract(0, 1, 0);
			if (loc.getY() < 0) {
				break;
			}
		}

		return false;
	}

	public static boolean hasLineOfSight(Entity from, Entity to) {
		return hasLineOfSight((from instanceof LivingEntity ? ((LivingEntity)from).getEyeLocation() : from.getLocation()),
		                      (to instanceof LivingEntity ? ((LivingEntity)to).getEyeLocation() : to.getLocation()));
	}

	public static boolean hasLineOfSight(Location fromLocation, Location toLocation) {
		int range = (int)fromLocation.distance(toLocation) + 1;
		Vector direction = toLocation.toVector().subtract(fromLocation.toVector()).normalize();

		try {
			BlockIterator bi = new BlockIterator(fromLocation.getWorld(), fromLocation.toVector(), direction, 0, range);

			while (bi.hasNext()) {
				Block b = bi.next();

				// If block is occluding (shouldn't include transparent blocks, liquids etc.),
				// line of sight is broken, return false
				if (BlockUtils.isLosBlockingBlock(b.getType())) {
					return false;
				}
			}
		} catch (IllegalStateException e) {
			// Thrown sometimes when chunks aren't loaded at exactly the right time
			return false;
		}

		return true;
	}

	// Search a cuboid around a Location and return the first Location found with a block matching one of the given Materials
	public static @Nullable Location getNearestBlock(Location center, int radius, Material... materials) {
		int cx = center.getBlockX();
		int cy = center.getBlockY();
		int cz = center.getBlockZ();
		World world = center.getWorld();
		Location nearest = null;
		double nearestDistance = Double.MAX_VALUE;

		for (double x = cx - radius; x <= cx + radius; x++) {
			for (double z = cz - radius; z <= cz + radius; z++) {
				for (double y = (cy - radius); y <= (cy + radius); y++) {
					Location loc = new Location(world, x, y, z);
					double distance = Math.sqrt(((cx - x) * (cx - x)) + ((cz - z) * (cz - z)) + ((cy - y) * (cy - y)));
					if (distance < nearestDistance) {
						for (Material material : materials) {
							if (loc.getBlock().getType() == material) {
								nearest = loc;
								nearestDistance = distance;
								break;
							}
						}
					}
				}
			}
		}
		return nearest;
	}

	// Search a cuboid around a Location and return a List of all Chests inside the area
	public static List<Chest> getNearbyChests(Location center, int radius) {
		int cx = center.getBlockX();
		int cy = center.getBlockY();
		int cz = center.getBlockZ();
		World world = center.getWorld();
		List<Chest> chests = new ArrayList<>();

		for (int x = cx - radius; x <= cx + radius; x++) {
			for (int z = cz - radius; z <= cz + radius; z++) {
				for (int y = (cy - radius); y < (cy + radius); y++) {
					Location loc = new Location(world, x, y + 2, z);
					if (loc.getBlock().getState() instanceof Chest) {
						chests.add((Chest) loc.getBlock().getState());
					}
				}
			}
		}
		return chests;
	}

	public static String locationToString(Location loc) {
		JsonObject locObj = new JsonObject();
		locObj.addProperty("x", loc.getX());
		locObj.addProperty("y", loc.getY());
		locObj.addProperty("z", loc.getZ());
		locObj.addProperty("yaw", loc.getYaw());
		locObj.addProperty("pitch", loc.getPitch());
		return locObj.toString();
	}

	public static Location locationFromString(World world, String locStr) throws Exception {
		Gson gson = new Gson();
		JsonObject obj = gson.fromJson(locStr, JsonObject.class);
		return new Location(world, obj.get("x").getAsDouble(), obj.get("y").getAsDouble(), obj.get("z").getAsDouble(),
		                    obj.get("yaw").getAsFloat(), obj.get("pitch").getAsFloat());
	}

	/**
	 * Determines if a location "loc" collides with the given Block "block"
	 * @param loc the given location
	 * @return true if the location is inside the hitbox of the block, false if not
	 */
	public static boolean collidesWithSolid(Location loc) {
		return collidesWithBlocks(BoundingBox.of(loc, 0.001, 0.001, 0.001), loc.getWorld());
	}

	/* Note:
	 * loc1 must be the location with a lesser x, y, and z coordinate than loc2.
	 * loc2 must be the location with a greater x, y, and z coordinate than loc1.
	 */
	public static List<Block> getEdge(Location loc1, Location loc2) {
		List<Block> blocks = new ArrayList<>();
		int x1 = loc1.getBlockX();
		int y1 = loc1.getBlockY();
		int z1 = loc1.getBlockZ();

		int x2 = loc2.getBlockX();
		int y2 = loc2.getBlockY();
		int z2 = loc2.getBlockZ();

		World world = loc1.getWorld();
		for (int xPoint = x1; xPoint <= x2; xPoint++) {
			Block currentBlock = world.getBlockAt(xPoint, y1, z1);
			blocks.add(currentBlock);
		}
		for (int xPoint = x1; xPoint <= x2; xPoint++) {
			Block currentBlock = world.getBlockAt(xPoint, y2, z2);
			blocks.add(currentBlock);
		}

		for (int yPoint = y1; yPoint <= y2; yPoint++) {
			Block currentBlock = world.getBlockAt(x1, yPoint, z1);
			blocks.add(currentBlock);
		}
		for (int yPoint = y1; yPoint <= y2; yPoint++) {
			Block currentBlock = world.getBlockAt(x2, yPoint, z2);
			blocks.add(currentBlock);
		}

		for (int zPoint = z1; zPoint <= z2; zPoint++) {
			Block currentBlock = world.getBlockAt(x1, y1, zPoint);
			blocks.add(currentBlock);
		}
		for (int zPoint = z1; zPoint <= z2; zPoint++) {
			Block currentBlock = world.getBlockAt(x2, y2, zPoint);
			blocks.add(currentBlock);
		}
		return blocks;
	}

	public static ArrayList<Block> getNearbyBlocks(Block start, int radius) {
		ArrayList<Block> blocks = new ArrayList<>();
		for (double x = start.getLocation().getX() - radius; x <= start.getLocation().getX() + radius; x++) {
			for (double y = start.getLocation().getY() - radius; y <= start.getLocation().getY() + radius; y++) {
				for (double z = start.getLocation().getZ() - radius; z <= start.getLocation().getZ() + radius; z++) {
					Location loc = new Location(start.getWorld(), x, y, z);
					blocks.add(loc.getBlock());
				}
			}
		}
		return blocks;
	}

	// Returns all block locations the bounding box touches.
	// Eg for a player, this could be as little as 2 or as many as 12 full blocks,
	// depending on how offset their bounding box's location is
	public static ArrayList<Location> getLocationsTouching(BoundingBox boundingBox, World world) {
		ArrayList<Location> locationsTouching = new ArrayList<>();
		int startX = (int) Math.floor(boundingBox.getMinX());
		int endX = (int) Math.ceil(boundingBox.getMaxX());
		int startY = (int) Math.floor(boundingBox.getMinY());
		int endY = (int) Math.ceil(boundingBox.getMaxY());
		int startZ = (int) Math.floor(boundingBox.getMinZ());
		int endZ = (int) Math.ceil(boundingBox.getMaxZ());
		for (int x = startX; x < endX; x++) {
			for (int y = startY; y < endY; y++) {
				for (int z = startZ; z < endZ; z++) {
					locationsTouching.add(new Location(world, x, y, z));
				}
			}
		}
		return locationsTouching;
	}

	/**
	 * Tests if a given bounding box collides with any blocks.
	 *
	 * @param boundingBox The box to check
	 * @param world       The world to check in
	 * @return Whether the box collides with any blocks or is in unloaded chunks
	 */
	public static boolean collidesWithBlocks(BoundingBox boundingBox, World world) {
		return NmsUtils.getVersionAdapter().hasCollisionWithBlocks(world, boundingBox, true);
	}

	public static boolean travelTillObstructed(
		World world,
		BoundingBox movingBoundingBox,
		double maxDistance,
		Vector vector,
		double increment
	) {
		return travelTillObstructed(world, movingBoundingBox, maxDistance, vector, increment, false, null, -1, -1);
	}

	/**
	 * Moved a {@link BoundingBox} along a straight path for the given max distance or until it collides with a block.
	 *
	 * @param world             World to travel in
	 * @param movingBoundingBox The bounding box to move and check collisions with. Is uses as starting location, and it will hold the final location after the method returns.
	 * @param maxDistance       Maximum travel distance
	 * @param vector            Direction of travel, does not need to be normalized
	 * @param increment         Step size of travel. Should be smaller than the smallest dimension of the bounding box to prevent travelling through walls.
	 * @param wiggleY           Whether to allow a bit of vertical wiggle room for the box while traveling to be able to scale over/under obstacles up to 1 block in size
	 * @param travelAction      Action to execute every {@code actionFrequency} steps, may be null. Will get the center of the bounding box as argument.
	 * @param actionFrequency   How often to execute {@code travelAction}, in steps
	 * @param actionChance      Reciprocal random chance to perform the action (for a given n, the chance is 1/n)
	 * @return Whether travel was stopped by a block before the maximum distance has been reached
	 */
	public static boolean travelTillObstructed(
		World world,
		BoundingBox movingBoundingBox,
		double maxDistance,
		Vector vector,
		double increment,
		boolean wiggleY,
		@Nullable TravelAction travelAction,
		int actionFrequency,
		int actionChance
	) {
		Vector start = movingBoundingBox.getCenter(); // For checking if exceeded maxDistance
		Vector vectorIncrement = vector.clone().normalize().multiply(increment);
		int frequencyTracker = actionFrequency; // For deciding whether to run travelAction for this interval

		// this box always moves along a straight line, even if wiggle room is enabled
		BoundingBox testBox = movingBoundingBox.clone();

		double maxIterations = maxDistance / increment * 1.1;
		for (int i = 0; i < maxIterations; i++) {
			testBox.shift(vectorIncrement);
			Vector testBoxCentre = testBox.getCenter();

			if (start.distanceSquared(testBoxCentre) > maxDistance * maxDistance) {
				// Gone too far
				return false;
			}
			if (collidesWithBlocks(testBox, world)) {
				// Collision on path
				// If wiggleY is enabled, look for a free spot up to half of the bounding box height below or above the blocked location
				if (wiggleY) {
					boolean blocked = true;
					BoundingBox wiggleBox = testBox.clone();
					double step = 0.1;
					int steps = (int) (wiggleBox.getHeight() / step);
					wiggleBox.shift(0, -wiggleBox.getHeight() / 2 + step / 2, 0);
					for (int dy = 0; dy < steps; dy++) {
						// Scan along the y-axis, from -height/2+step/2 to +height/2-step/2, to find the lowest available space.
						if (!collidesWithBlocks(wiggleBox, world)) {
							blocked = false;
							break;
						}
						wiggleBox.shift(0, step, 0);
					}
					if (blocked) {
						return true;
					}
					movingBoundingBox.copy(wiggleBox);
				} else {
					return true;
				}
			} else {
				movingBoundingBox.copy(testBox);
			}

			// The central location of the bounding box is valid;
			// it was not reverse shifted. Run travelAction if frequency is right
			if (travelAction != null) {
				if (frequencyTracker >= actionFrequency) {
					actionFrequency = 1;

					if (actionChance <= 1 || FastUtils.RANDOM.nextInt(actionChance) == 0) {
						travelAction.run(testBoxCentre.toLocation(world));
					}
				} else {
					actionFrequency++;
				}
			}
		}

		// Neither went too far nor got obstructed (this should not happen)
		return false;
	}

	public static void travelTillMaxDistance(
		World world,
		BoundingBox movingBoundingBox,
		double maxDistance,
		Vector vector,
		double increment,
		@Nullable TravelAction travelAction,
		int actionFrequency,
		int actionChance
	) {
		Vector start = movingBoundingBox.getCenter(); // For checking if exceeded maxDistance
		Vector vectorIncrement = vector.clone().normalize().multiply(increment);
		int frequencyTracker = actionFrequency; // For deciding whether to run travelAction for this interval

		// this box always moves along a straight line, even if wiggle room is enabled
		BoundingBox testBox = movingBoundingBox.clone();

		double maxIterations = maxDistance / increment * 1.1;
		for (int i = 0; i < maxIterations; i++) {
			testBox.shift(vectorIncrement);
			Vector testBoxCentre = testBox.getCenter();

			if (start.distanceSquared(testBoxCentre) > maxDistance * maxDistance) {
				// Gone too far
				return;
			}
			movingBoundingBox.copy(testBox);

			// The central location of the bounding box is valid;
			// it was not reverse shifted. Run travelAction if frequency is right
			if (travelAction != null) {
				if (frequencyTracker >= actionFrequency) {
					actionFrequency = 1;

					if (actionChance <= 1 || FastUtils.RANDOM.nextInt(actionChance) == 0) {
						travelAction.run(testBoxCentre.toLocation(world));
					}
				} else {
					actionFrequency++;
				}
			}
		}
	}

	// Adds part or all of y height above feet location, based on multiplier. Player height 1.8, player sneaking height 1.5
	public static Location getHeightLocation(Entity entity, double heightMultiplier) {
		return entity.getLocation().add(0, entity.getHeight() * heightMultiplier, 0);
	}

	public static Location getHalfHeightLocation(Entity entity) {
		return getHeightLocation(entity, 0.5);
	}

	// Player eye height 1.62 when not sneaking
	public static Location getHalfEyeLocation(LivingEntity entity) {
		return entity.getLocation().add(0, entity.getEyeHeight() / 2, 0);
	}

	public static Location getLocationCentre(Block block) {
		return getLocationCentre(block.getLocation());
	}

	public static Location getLocationCentre(Location location) {
		return location.add(0.5, 0.5, 0.5);
	}

	// Fills blocks between two locations with a specific material. Locations are inclusive (will place a block at both start and end)
	public static void fillBlocks(Location pos1, Location pos2, Material mat) {
		if (!pos1.getWorld().equals(pos2.getWorld())) {
			// Can't fill blocks between two different worlds
			Plugin.getInstance().getLogger().severe("Attempted to fill blocks between " + pos1 + " and " + pos2 + " which are in different worlds");
			return;
		}
		World world = pos1.getWorld();
		Location min = new Location(world,
									Math.min(pos1.getX(), pos2.getX()),
									Math.min(pos1.getY(), pos2.getY()),
									Math.min(pos1.getZ(), pos2.getZ()));
		Location max = new Location(world,
									Math.max(pos1.getX(), pos2.getX()),
									Math.max(pos1.getY(), pos2.getY()),
									Math.max(pos1.getZ(), pos2.getZ()));
		Location temp = min.clone();
		for (int dx = 0; dx <= (max.getBlockX() - min.getBlockX()); dx++) {
			for (int dy = 0; dy <= (max.getBlockY() - min.getBlockY()); dy++) {
				for (int dz = 0; dz <= (max.getBlockZ() - min.getBlockZ()); dz++) {
					temp.setX(min.getBlockX() + dx);
					temp.setY(min.getBlockY() + dy);
					temp.setZ(min.getBlockZ() + dz);
					temp.getBlock().setType(mat);
				}
			}
		}
	}

	public static List<Chunk> getSurroundingChunks(Block block, int radius) {
		ArrayList<Chunk> chunkList = new ArrayList<Chunk>();
		Location location = block.getLocation();
		// 16 block offset guarantees we get an adjacent chunk
		for (int x = -radius; x <= radius; x += 16) {
			for (int z = -radius; z <= radius; z += 16) {
				Location offsetLocation = location.clone().add(x, 0, z);
				if (offsetLocation.isChunkLoaded()) {
					chunkList.add(block.getRelative(x, 0, z).getChunk());
				}
			}
		}
		return chunkList;
	}

	public static boolean blocksAreWithinRadius(Block block1, Block block2, int radius) {
		return block1.getLocation().distanceSquared(block2.getLocation()) <= radius * radius;
	}

	// TODO use Consumer?
	@FunctionalInterface
	public interface TravelAction {
		/**
		 * Depending on accompanying actionFrequency & actionChance,
		 * gets called along valid Location intervals while travelling
		 *
		 * @param location Valid Location interval to run your code off
		 */
		void run(Location location);
	}

	private static final EnumSet<Biome> SNOWY_BIOMES = EnumSet.of(
		Biome.DEEP_FROZEN_OCEAN,
		Biome.FROZEN_OCEAN,
		Biome.FROZEN_RIVER,
		Biome.ICE_SPIKES,
		Biome.SNOWY_BEACH,
		Biome.SNOWY_TAIGA,
		Biome.SNOWY_PLAINS,
		Biome.SNOWY_SLOPES,
		Biome.FROZEN_PEAKS,
		Biome.JAGGED_PEAKS,
		Biome.OLD_GROWTH_PINE_TAIGA,
		Biome.OLD_GROWTH_SPRUCE_TAIGA,
		Biome.TAIGA
	);

	public static boolean isInSnowyBiome(Location loc) {
		return SNOWY_BIOMES.contains(loc.getBlock().getBiome());
	}

	public static double xzDistance(Location loc1, Location loc2) {
		Location flat1 = loc1.clone();
		flat1.setY(loc2.getY());
		return flat1.distance(loc2);
	}

	public static Location randomLocationInCircle(Location center, double radius) {
		double theta = FastUtils.randomDoubleInRange(0, 2 * Math.PI);
		double r = Math.sqrt(FastUtils.RANDOM.nextDouble()) * radius;
		return center.clone().add(r * FastUtils.cos(theta), 0, r * FastUtils.sin(theta));
	}

	public static Location fallToGround(Location loc, double minHeight) {
		Location clone = loc.clone();

		// If below minHeight, go up to it
		if (clone.getY() <= minHeight) {
			clone.setY(minHeight);
			return clone;
		}

		Block block = clone.getBlock();
		if (block.isSolid()) {
			// If inside a block, go to the top of the block
			clone.setY(Math.max(block.getBoundingBox().getMaxY(), minHeight));
			return clone;
		} else {
			// If not inside a block, go one block down and try again
			Block below = block.getRelative(BlockFace.DOWN);
			clone.setY(below.getY() + 0.5);
			return fallToGround(clone, minHeight);
		}
	}

	public static String getPoiNameFromLocation(Location location) {
		List<RespawningStructure> structures = StructuresPlugin.getInstance().mRespawnManager.getStructures(location.toVector(), false);

		for (RespawningStructure structure : structures) {
			return (String) structure.getConfig().get("name");
		}

		return null;
	}

}
