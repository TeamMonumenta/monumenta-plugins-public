package com.playmonumenta.plugins.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.data.Bisected.Half;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Cake;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Door.Hinge;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.GlassPane;
import org.bukkit.block.data.type.Piston;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Snow;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.Stairs.Shape;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

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

	public static boolean isLosBlockingBlock(Material mat) {
		return mat.isOccluding();
	}

	public static boolean isPathBlockingBlock(Material mat) {
		return mat.isSolid() || mat.equals(Material.LAVA);
	}

	public static boolean isWaterlogged(Block block) {
		BlockData data = block.getBlockData();
		if (data != null && data instanceof Waterlogged) {
			return ((Waterlogged)data).isWaterlogged();
		}
		return false;
	}

	public static boolean containsWater(Block block) {
		if (isWaterlogged(block)) {
			return true;
		}
		Material mat = block.getType();
		if (mat.equals(Material.BUBBLE_COLUMN) ||
		    mat.equals(Material.KELP) ||
		    mat.equals(Material.KELP_PLANT) ||
		    mat.equals(Material.SEAGRASS) ||
		    mat.equals(Material.TALL_SEAGRASS)) {
			return true;
		}
		return false;
	}

	public static boolean isRail(Block block) {
		BlockData data = block.getBlockData();
		if (data != null && data instanceof Rail) {
			return true;
		}
		return false;
	}

	public static boolean isValidMinecartLocation(Location loc) {
		Block block = loc.getBlock();
		if (isRail(block)) {
			return true;
		}

		block = loc.subtract(0, 1, 0).getBlock();
		if (isRail(block)) {
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
			if (isRail(block)) {
				return true;
			} else if (!block.isEmpty()) {
				return false;
			}
		}

		return false;
	}

	public static boolean isLocationInWater(Location loc) {
		Block block = loc.getBlock();
		if (block.isLiquid() || containsWater(block) || block.getType() == Material.ICE || block.getType() == Material.BLUE_ICE || block.getType() == Material.PACKED_ICE) {
			return true;
		}

		block = loc.subtract(0, 1, 0).getBlock();
		if (block.isLiquid() || containsWater(block) || block.getType() == Material.ICE || block.getType() == Material.BLUE_ICE || block.getType() == Material.PACKED_ICE) {
			return true;
		}

		return false;
	}

	public static boolean isValidBoatLocation(Location loc) {
		if (isLocationInWater(loc)) {
			return true;
		}

		/*
		 * Check up to 50 blocks underneath the location. Stop when
		 * a non-air block is hit. If it's a liquid, this is allowed, otherwise it's not
		 */
		loc = loc.clone();
		for (int i = loc.getBlockY(); i > Math.max(0, loc.getBlockY() - 50); i--) {
			loc.setY(i);
			Block block = loc.getBlock();
			if (block.isLiquid() || containsWater(block) || block.getType() == Material.ICE || block.getType() == Material.BLUE_ICE || block.getType() == Material.PACKED_ICE) {
				return true;
			} else if (!block.isEmpty()) {
				return false;
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

				// If block is occluding (shouldn't include transparent blocks, liquids etc),
				// line of sight is broken, return false
				if (LocationUtils.isLosBlockingBlock(b.getType())) {
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
		List<Chest> chests = new ArrayList<Chest>();

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
	 * @param block the Block to test
	 * @param loc the given location
	 * @return true if the location is inside the hitbox of the block, false if not
	 */
	public static boolean collidesWithSolid(Location loc, Block block) {
		if (loc.getBlockX() != block.getX()
		    || loc.getBlockY() != block.getY()
		    || loc.getBlockZ() != block.getZ()) {
			return false;
		}
		if (!block.getType().isSolid()
		    && !(block.getBlockData() instanceof Snow)
		    && !(block.getBlockData() instanceof Bed)) {
			return false;
		}

		// The block is a Solid or SemiSolid at this point, some of it is solid and some of it is air.
		// collides: Determines if the location is located in a solid part.
		boolean collides = true;

		// Coördinates inside the block, adjusted for negative values
		double x = (loc.getX() % 1 + 1) % 1;
		double y = (loc.getY() % 1 + 1) % 1;
		double z = (loc.getZ() % 1 + 1) % 1;

		switch (block.getType()) {

		// cases for individual blocks
		case OAK_SIGN:
			collides = false;
			break;
		case OAK_WALL_SIGN:
			collides = false;
			break;
		case BIRCH_SIGN:
			collides = false;
			break;
		case BIRCH_WALL_SIGN:
			collides = false;
			break;
		case SPRUCE_SIGN:
			collides = false;
			break;
		case SPRUCE_WALL_SIGN:
			collides = false;
			break;
		case DARK_OAK_SIGN:
			collides = false;
			break;
		case DARK_OAK_WALL_SIGN:
			collides = false;
			break;
		case ACACIA_SIGN:
			collides = false;
			break;
		case ACACIA_WALL_SIGN:
			collides = false;
			break;
		case TURTLE_EGG:
			collides = false;
			break;
		case SOUL_SAND:
			if (y > (15.0 / 16.0)) {
				collides = false;
			}
			break;
		case GRASS_PATH:
			if (y > (15.0 / 16.0)) {
				collides = false;
			}
			break;
		case FARMLAND:
			if (y > (15.0 / 16.0)) {
				collides = false;
			}
			break;
		case END_PORTAL_FRAME:
			if (y > (13.0 / 16.0)) {
				collides = false;
			}
			break;
		case CONDUIT:
			if (y > (11.0 / 16.0) || y < (5.0 / 16.0)
			    || x > (11.0 / 16.0) || x < (5.0 / 16.0)
			    || z > (11.0 / 16.0) || z < (5.0 / 16.0)) {
				collides = false;
			}
			break;
		case ENCHANTING_TABLE:
			if (y > (12.0 / 16.0)) {
				collides = false;
			}
			break;
		case DAYLIGHT_DETECTOR:
			if (y > (6.0 / 16.0)) {
				collides = false;
			}
			break;
		case BREWING_STAND:
			if (y > (2.0 / 16.0)
			    && (x > (9.0 / 16.0) || x < (7.0 / 16.0))
			    && (z > (9.0 / 16.0) || z < (7.0 / 16.0))) {
				collides = false;
			}
			break;
		case CHEST:
			if (y > (15.0 / 16.0)
			    || x > (15.0 / 16.0) || x < (1.0 / 16.0)
			    || z > (15.0 / 16.0) || z < (1.0 / 16.0)) {
				collides = false;
			}
			break;
		case TRAPPED_CHEST:
			if (y > (15.0 / 16.0)
			    || x > (15.0 / 16.0) || x < (1.0 / 16.0)
			    || z > (15.0 / 16.0) || z < (1.0 / 16.0)) {
				collides = false;
			}
			break;
		case ENDER_CHEST:
			if (y > (15.0 / 16.0)
			    || x > (15.0 / 16.0) || x < (1.0 / 16.0)
			    || z > (15.0 / 16.0) || z < (1.0 / 16.0)) {
				collides = false;
			}
			break;
		case DRAGON_EGG:
			if (x > (15.0 / 16.0) || x < (1.0 / 16.0)
			    || z > (15.0 / 16.0) || z < (1.0 / 16.0)) {
				collides = false;
			}
			break;
		case HOPPER:
			if (y < (4.0 / 16.0) ||
			    (y < (10.0 / 16.0) &&
			     (x > (12.0 / 16.0) || x < (4.0 / 16.0)
			      || z > (12.0 / 16.0) || z < (4.0 / 16.0)))) {
				collides = false;
			}
			break;
		case SNOW:
			int layers = ((Snow) block.getBlockData()).getLayers();
			if (y > (2 * layers / 16.0)) {
				collides = false;
			}
			break;
		case CAKE:
			int bites = ((Cake) block.getBlockData()).getBites();
			if (y > (7.0 / 16.0)
			    || x > (15.0 / 16.0) || x < ((1.0 + 2 * bites) / 16.0)
			    || z > (15.0 / 16.0) || z < (1.0 / 16.0)) {
				collides = false;
			}
			break;

		case PISTON_HEAD:
			BlockFace pistonface = ((Directional) block.getBlockData()).getFacing();
			if (pistonface == BlockFace.EAST) {
				if (x < (12.0 / 16.0) &&
				    (y > (10.0 / 16.0) || y < (6.0 / 16.0)
				     || z > (10.0 / 16.0) || z < (6.0 / 16.0))) {
					collides = false;
				}
			} else if (pistonface == BlockFace.WEST) {
				if (x > (4.0 / 16.0) &&
				    (y > (10.0 / 16.0) || y < (6.0 / 16.0)
				     || z > (10.0 / 16.0) || z < (6.0 / 16.0))) {
					collides = false;
				}
			} else if (pistonface == BlockFace.NORTH) {
				if (z > (4.0 / 16.0) &&
				    (y > (10.0 / 16.0) || y < (6.0 / 16.0)
				     || x > (10.0 / 16.0) || x < (6.0 / 16.0))) {
					collides = false;
				}
			} else if (pistonface == BlockFace.SOUTH) {
				if (z < (12.0 / 16.0) &&
				    (y > (10.0 / 16.0) || y < (6.0 / 16.0)
				     || x > (10.0 / 16.0) || x < (6.0 / 16.0))) {
					collides = false;
				}
			} else if (pistonface == BlockFace.UP) {
				if (y < (12.0 / 16.0) &&
				    (z > (10.0 / 16.0) || z < (6.0 / 16.0)
				     || x > (10.0 / 16.0) || x < (6.0 / 16.0))) {
					collides = false;
				}
			} else if (pistonface == BlockFace.DOWN) {
				if (y > (4.0 / 16.0) &&
				    (z > (10.0 / 16.0) || z < (6.0 / 16.0)
				     || x > (10.0 / 16.0) || x < (6.0 / 16.0))) {
					collides = false;
				}
			}
			break;

		// ignore all banners
		case WHITE_BANNER:
			collides = false;
			break;
		case ORANGE_BANNER:
			collides = false;
			break;
		case MAGENTA_BANNER:
			collides = false;
			break;
		case LIGHT_BLUE_BANNER:
			collides = false;
			break;
		case YELLOW_BANNER:
			collides = false;
			break;
		case LIME_BANNER:
			collides = false;
			break;
		case PINK_BANNER:
			collides = false;
			break;
		case GRAY_BANNER:
			collides = false;
			break;
		case LIGHT_GRAY_BANNER:
			collides = false;
			break;
		case CYAN_BANNER:
			collides = false;
			break;
		case PURPLE_BANNER:
			collides = false;
			break;
		case BLUE_BANNER:
			collides = false;
			break;
		case BROWN_BANNER:
			collides = false;
			break;
		case GREEN_BANNER:
			collides = false;
			break;
		case RED_BANNER:
			collides = false;
			break;
		case BLACK_BANNER:
			collides = false;
			break;
		case WHITE_WALL_BANNER:
			collides = false;
			break;
		case ORANGE_WALL_BANNER:
			collides = false;
			break;
		case MAGENTA_WALL_BANNER:
			collides = false;
			break;
		case LIGHT_BLUE_WALL_BANNER:
			collides = false;
			break;
		case YELLOW_WALL_BANNER:
			collides = false;
			break;
		case LIME_WALL_BANNER:
			collides = false;
			break;
		case PINK_WALL_BANNER:
			collides = false;
			break;
		case GRAY_WALL_BANNER:
			collides = false;
			break;
		case LIGHT_GRAY_WALL_BANNER:
			collides = false;
			break;
		case CYAN_WALL_BANNER:
			collides = false;
			break;
		case PURPLE_WALL_BANNER:
			collides = false;
			break;
		case BLUE_WALL_BANNER:
			collides = false;
			break;
		case BROWN_WALL_BANNER:
			collides = false;
			break;
		case GREEN_WALL_BANNER:
			collides = false;
			break;
		case RED_WALL_BANNER:
			collides = false;
			break;
		case BLACK_WALL_BANNER:
			collides = false;
			break;

		// ignore all pressure plates
		case STONE_PRESSURE_PLATE:
			collides = false;
			break;
		case LIGHT_WEIGHTED_PRESSURE_PLATE:
			collides = false;
			break;
		case HEAVY_WEIGHTED_PRESSURE_PLATE:
			collides = false;
			break;
		case ACACIA_PRESSURE_PLATE:
			collides = false;
			break;
		case BIRCH_PRESSURE_PLATE:
			collides = false;
			break;
		case DARK_OAK_PRESSURE_PLATE:
			collides = false;
			break;
		case JUNGLE_PRESSURE_PLATE:
			collides = false;
			break;
		case OAK_PRESSURE_PLATE:
			collides = false;
			break;
		case SPRUCE_PRESSURE_PLATE:
			collides = false;
			break;

		// ignore all dead coral
		case DEAD_BRAIN_CORAL:
			collides = false;
			break;
		case DEAD_BRAIN_CORAL_FAN:
			collides = false;
			break;
		case DEAD_BRAIN_CORAL_WALL_FAN:
			collides = false;
			break;
		case DEAD_BUBBLE_CORAL:
			collides = false;
			break;
		case DEAD_BUBBLE_CORAL_FAN:
			collides = false;
			break;
		case DEAD_BUBBLE_CORAL_WALL_FAN:
			collides = false;
			break;
		case DEAD_FIRE_CORAL:
			collides = false;
			break;
		case DEAD_FIRE_CORAL_FAN:
			collides = false;
			break;
		case DEAD_FIRE_CORAL_WALL_FAN:
			collides = false;
			break;
		case DEAD_HORN_CORAL:
			collides = false;
			break;
		case DEAD_HORN_CORAL_FAN:
			collides = false;
			break;
		case DEAD_HORN_CORAL_WALL_FAN:
			collides = false;
			break;
		case DEAD_TUBE_CORAL:
			collides = false;
			break;
		case DEAD_TUBE_CORAL_FAN:
			collides = false;
			break;
		case DEAD_TUBE_CORAL_WALL_FAN:
			collides = false;
			break;

		// cases for block data type groups
		default:
			if (block.getBlockData() instanceof Slab) {
				if (((Slab) block.getBlockData()).getType() == Slab.Type.BOTTOM) {
					if (y > 0.5) {
						collides = false;
					}
				} else if (((Slab) block.getBlockData()).getType() == Slab.Type.TOP) {
					if (y < 0.5) {
						collides = false;
					}
				}
			} else if (block.getBlockData() instanceof Stairs) {
				Stairs stair = ((Stairs) block.getBlockData());
				if (stair.getHalf() == Half.BOTTOM) {
					if (y > 0.5) {
						collides = false;
					}
				} else if (stair.getHalf() == Half.TOP) {
					if (y < 0.5) {
						collides = false;
					}
				}
				if (stair.getShape() == Shape.STRAIGHT
				    || stair.getShape() == Shape.INNER_LEFT
				    || stair.getShape() == Shape.INNER_RIGHT) {
					if (stair.getFacing() == BlockFace.NORTH
					    || (stair.getFacing() == BlockFace.WEST
					        && stair.getShape() == Shape.INNER_RIGHT)
					    || (stair.getFacing() == BlockFace.EAST
					        && stair.getShape() == Shape.INNER_LEFT)) {
						if (z < 0.5) {
							collides = true;
						}
					}
					if (stair.getFacing() == BlockFace.SOUTH
					    || (stair.getFacing() == BlockFace.EAST
					        && stair.getShape() == Shape.INNER_RIGHT)
					    || (stair.getFacing() == BlockFace.WEST
					        && stair.getShape() == Shape.INNER_LEFT)) {
						if (z > 0.5) {
							collides = true;
						}
					}
					if (stair.getFacing() == BlockFace.WEST
					    || (stair.getFacing() == BlockFace.SOUTH
					        && stair.getShape() == Shape.INNER_RIGHT)
					    || (stair.getFacing() == BlockFace.NORTH
					        && stair.getShape() == Shape.INNER_LEFT)) {
						if (x < 0.5) {
							collides = true;
						}
					}
					if (stair.getFacing() == BlockFace.EAST
					    || (stair.getFacing() == BlockFace.NORTH
					        && stair.getShape() == Shape.INNER_RIGHT)
					    || (stair.getFacing() == BlockFace.SOUTH
					        && stair.getShape() == Shape.INNER_LEFT)) {
						if (x > 0.5) {
							collides = true;
						}
					}
				} else {
					if ((stair.getFacing() == BlockFace.NORTH
					     && stair.getShape() == Shape.OUTER_RIGHT)
					    || (stair.getFacing() == BlockFace.EAST
					        && stair.getShape() == Shape.OUTER_LEFT)) {
						if (z < 0.5 && x > 0.5) {
							collides = true;
						}
					} else if ((stair.getFacing() == BlockFace.EAST
					            && stair.getShape() == Shape.OUTER_RIGHT)
					           || (stair.getFacing() == BlockFace.SOUTH
					               && stair.getShape() == Shape.OUTER_LEFT)) {
						if (z > 0.5 && x > 0.5) {
							collides = true;
						}
					} else if ((stair.getFacing() == BlockFace.SOUTH
					            && stair.getShape() == Shape.OUTER_RIGHT)
					           || (stair.getFacing() == BlockFace.WEST
					               && stair.getShape() == Shape.OUTER_LEFT)) {
						if (z > 0.5 && x < 0.5) {
							collides = true;
						}
					} else if ((stair.getFacing() == BlockFace.WEST
					            && stair.getShape() == Shape.OUTER_RIGHT)
					           || (stair.getFacing() == BlockFace.NORTH
					               && stair.getShape() == Shape.OUTER_LEFT)) {
						if (z < 0.5 && x < 0.5) {
							collides = true;
						}
					}
				}
			} else if (block.getBlockData() instanceof GlassPane || block.getType() == Material.IRON_BARS) {
				MultipleFacing pane = ((MultipleFacing) block.getBlockData());
				if (z > (9.0 / 16.0)
				    || z < (7.0 / 16.0)
				    || x > (9.0 / 16.0)
				    || x < (7.0 / 16.0)) {
					collides = false;
				}
				if (!collides && pane.hasFace(BlockFace.NORTH)) {
					if (z < (9.0 / 16.0)
					    && x > (7.0 / 16.0)
					    && x < (9.0 / 16.0)) {
						collides = true;
					}
				}
				if (!collides && pane.hasFace(BlockFace.SOUTH)) {
					if (z > (7.0 / 16.0)
					    && x > (7.0 / 16.0)
					    && x < (9.0 / 16.0)) {
						collides = true;
					}
				}
				if (!collides && pane.hasFace(BlockFace.EAST)) {
					if (x > (7.0 / 16.0)
					    && z > (7.0 / 16.0)
					    && z < (9.0 / 16.0)) {
						collides = true;
					}
				}
				if (!collides && pane.hasFace(BlockFace.WEST)) {
					if (x < (9.0 / 16.0)
					    && z > (7.0 / 16.0)
					    && z < (9.0 / 16.0)) {
						collides = true;
					}
				}
			} else if (block.getBlockData() instanceof Fence) {
				int width = 4;
				if (block.getType() == Material.COBBLESTONE_WALL
				    || block.getType() == Material.MOSSY_COBBLESTONE_WALL) {
					width = 8;
				}
				Fence fence = ((Fence) block.getBlockData());
				if (z > ((8.0 + width / 2) / 16.0)
				    || z < ((8.0 - width / 2) / 16.0)
				    || x > ((8.0 + width / 2) / 16.0)
				    || x < ((8.0 - width / 2) / 16.0)) {
					collides = false;
				}
				if (!collides && fence.hasFace(BlockFace.NORTH)) {
					if (z < ((8.0 + width / 2) / 16.0)
					    && x > ((8.0 - width / 2) / 16.0)
					    && x < ((8.0 + width / 2) / 16.0)) {
						collides = true;
					}
				}
				if (!collides && fence.hasFace(BlockFace.SOUTH)) {
					if (z > ((8.0 - width / 2) / 16.0)
					    && x > ((8.0 - width / 2) / 16.0)
					    && x < ((8.0 + width / 2) / 16.0)) {
						collides = true;
					}
				}
				if (!collides && fence.hasFace(BlockFace.EAST)) {
					if (x > ((8.0 - width / 2) / 16.0)
					    && z > ((8.0 - width / 2) / 16.0)
					    && z < ((8.0 + width / 2) / 16.0)) {
						collides = true;
					}
				}
				if (!collides && fence.hasFace(BlockFace.EAST)) {
					if (x < ((8.0 + width / 2) / 16.0)
					    && z > ((8.0 - width / 2) / 16.0)
					    && z < ((8.0 + width / 2) / 16.0)) {
						collides = true;
					}
				}
			} else if (block.getBlockData() instanceof Gate) {
				Gate gate = ((Gate) block.getBlockData());
				boolean open = gate.isOpen();
				BlockFace face = gate.getFacing();
				if (y < (5.0 / 16.0)) {
					collides = false;
				}
				if (!open) {
					if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
						if (z > (9.0 / 16.0)
						    || z < (7.0 / 16.0)) {
							collides = false;
						}
					} else {
						if (x > (9.0 / 16.0)
						    || x < (7.0 / 16.0)) {
							collides = false;
						}
					}
				} else {
					if (face == BlockFace.NORTH) {
						if (!(z < (9.0 / 16.0)
						      && (x < (2.0 / 16.0)
						          || x > (14.0 / 16.0)))) {
							collides = false;
						}
					} else if (face == BlockFace.SOUTH) {
						if (!(z > (7.0 / 16.0)
						      && (x < (2.0 / 16.0)
						          || x > (14.0 / 16.0)))) {
							collides = false;
						}
					} else if (face == BlockFace.EAST) {
						if (!(x > (7.0 / 16.0)
						      && (z < (2.0 / 16.0)
						          || z > (14.0 / 16.0)))) {
							collides = false;
						}
					} else if (face == BlockFace.WEST) {
						if (!(x < (9.0 / 16.0)
						      && (z < (2.0 / 16.0)
						          || z > (14.0 / 16.0)))) {
							collides = false;
						}
					}
				}
			} else if (block.getBlockData() instanceof TrapDoor) {
				TrapDoor trapdoor = ((TrapDoor) block.getBlockData());
				boolean open = trapdoor.isOpen();
				if (!open) {
					if (trapdoor.getHalf() == Half.TOP) {
						if (y < (13.0 / 16.0)) {
							collides = false;
						}
					} else {
						if (y > (3.0 / 16.0)) {
							collides = false;
						}
					}
				} else {
					if (trapdoor.getFacing() == BlockFace.NORTH) {
						if (z < (13.0 / 16.0)) {
							collides = false;
						}
					} else if (trapdoor.getFacing() == BlockFace.SOUTH) {
						if (z > (3.0 / 16.0)) {
							collides = false;
						}
					} else if (trapdoor.getFacing() == BlockFace.EAST) {
						if (x > (3.0 / 16.0)) {
							collides = false;
						}
					} else if (trapdoor.getFacing() == BlockFace.WEST) {
						if (x < (13.0 / 16.0)) {
							collides = false;
						}
					}
				}
			} else if (block.getBlockData() instanceof Door) {
				Door door = ((Door) block.getBlockData());
				Hinge hinge = door.getHinge();
				BlockFace face = door.getFacing();
				boolean open = door.isOpen();
				if ((open && face == BlockFace.WEST && hinge == Hinge.LEFT)
				    || (open && face == BlockFace.EAST && hinge == Hinge.RIGHT)) {
					face = BlockFace.NORTH;
				} else if ((open && face == BlockFace.NORTH && hinge == Hinge.LEFT)
				           || (open && face == BlockFace.SOUTH && hinge == Hinge.RIGHT)) {
					face = BlockFace.EAST;
				} else if ((open && face == BlockFace.EAST && hinge == Hinge.LEFT)
				           || (open && face == BlockFace.WEST && hinge == Hinge.RIGHT)) {
					face = BlockFace.SOUTH;
				} else if ((open && face == BlockFace.SOUTH && hinge == Hinge.LEFT)
				           || (open && face == BlockFace.NORTH && hinge == Hinge.RIGHT)) {
					face = BlockFace.WEST;
				}
				if (face == BlockFace.EAST) {
					if (x > (3.0 / 16.0)) {
						collides = false;
					}
				} else if (face == BlockFace.WEST) {
					if (x < (13.0 / 16.0)) {
						collides = false;
					}
				} else if (face == BlockFace.NORTH) {
					if (z < (13.0 / 16.0)) {
						collides = false;
					}
				} else if (face == BlockFace.SOUTH) {
					if (z > (3.0 / 16.0)) {
						collides = false;
					}
				}
			} else if (block.getBlockData() instanceof Piston) {
				Piston piston = ((Piston) block.getBlockData());
				if (piston.isExtended()) {
					BlockFace face = piston.getFacing();
					if (face == BlockFace.EAST) {
						if (x > (12.0 / 16.0) &&
						    (y > (10.0 / 16.0) || y < (6.0 / 16.0)
						     || z > (10.0 / 16.0) || z < (6.0 / 16.0))) {
							collides = false;
						}
					} else if (face == BlockFace.WEST) {
						if (x < (4.0 / 16.0) &&
						    (y > (10.0 / 16.0) || y < (6.0 / 16.0)
						     || z > (10.0 / 16.0) || z < (6.0 / 16.0))) {
							collides = false;
						}
					} else if (face == BlockFace.NORTH) {
						if (z < (4.0 / 16.0) &&
						    (y > (10.0 / 16.0) || y < (6.0 / 16.0)
						     || x > (10.0 / 16.0) || x < (6.0 / 16.0))) {
							collides = false;
						}
					} else if (face == BlockFace.SOUTH) {
						if (z > (12.0 / 16.0) &&
						    (y > (10.0 / 16.0) || y < (6.0 / 16.0)
						     || x > (10.0 / 16.0) || x < (6.0 / 16.0))) {
							collides = false;
						}
					} else if (face == BlockFace.UP) {
						if (y > (12.0 / 16.0) &&
						    (z > (10.0 / 16.0) || z < (6.0 / 16.0)
						     || x > (10.0 / 16.0) || x < (6.0 / 16.0))) {
							collides = false;
						}
					} else if (face == BlockFace.DOWN) {
						if (y < (4.0 / 16.0) &&
						    (z > (10.0 / 16.0) || z < (6.0 / 16.0)
						     || x > (10.0 / 16.0) || x < (6.0 / 16.0))) {
							collides = false;
						}
					}
				}
			} else if (block.getBlockData() instanceof Bed) {
				if (y > (9.0 / 16.0)) {
					collides = false;
				}
			} else if (block.getType() == Material.ANVIL
			           || block.getType() == Material.CHIPPED_ANVIL
			           || block.getType() == Material.DAMAGED_ANVIL) {
				BlockFace face = ((Directional) block.getBlockData()).getFacing();
				if (face == BlockFace.EAST || face == BlockFace.WEST) {
					if (y < (4.0 / 16.0) &&
					    (z > (14.0 / 16.0) || z < (2.0 / 16.0)
					     || x > (14.0 / 16.0) || x < (2.0 / 16.0))) {
						collides = false;
					} else if ((y >= (4.0 / 16.0) && y < (5.0 / 16.0)) &&
					           (z > (12.0 / 16.0) || z < (4.0 / 16.0)
					            || x > (13.0 / 16.0) || x < (3.0 / 16.0))) {
						collides = false;
					} else if ((y >= (5.0 / 16.0) && y < (10.0 / 16.0)) &&
					           (z > (10.0 / 16.0) || z < (6.0 / 16.0)
					            || x > (12.0 / 16.0) || x < (4.0 / 16.0))) {
						collides = false;
					} else if ((y >= (10.0 / 16.0)) &&
					           (z > (13.0 / 16.0) || z < (3.0 / 16.0))) {
						collides = false;
					}
				} else {
					if (y < (4.0 / 16.0) &&
					    (x > (14.0 / 16.0) || x < (2.0 / 16.0)
					     || z > (14.0 / 16.0) || z < (2.0 / 16.0))) {
						collides = false;
					} else if ((y >= (4.0 / 16.0) && y < (5.0 / 16.0)) &&
					           (x > (12.0 / 16.0) || x < (4.0 / 16.0)
					            || z > (13.0 / 16.0) || z < (3.0 / 16.0))) {
						collides = false;
					} else if ((y >= (5.0 / 16.0) && y < (10.0 / 16.0)) &&
					           (x > (10.0 / 16.0) || x < (6.0 / 16.0)
					            || z > (12.0 / 16.0) || z < (4.0 / 16.0))) {
						collides = false;
					} else if ((x >= (10.0 / 16.0)) &&
					           (x > (13.0 / 16.0) || x < (3.0 / 16.0))) {
						collides = false;
					}
				}
			}
		}
		return collides;
	}

	/* Note:
	 * loc1 must be the location with a lesser x, y, and z coordinate than loc2.
	 * loc2 must be the location with a greater x, y, and z coordinate than loc1.
	 */
	public static List<Block> getEdge(Location loc1, Location loc2) {
		List<Block> blocks = new ArrayList<Block>();
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
		ArrayList<Block> blocks = new ArrayList<Block>();
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
		ArrayList<Location> locationsTouching = new ArrayList<Location>();
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

	public static boolean travelTillObstructed(
		World world,
		BoundingBox movingBoundingBox,
		double maxDistance,
		Vector vector,
		double increment
	) {
		return travelTillObstructed(world, movingBoundingBox, maxDistance, vector, increment, null, -7050, 1);
	}

	public static boolean travelTillObstructed(
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
		Vector reverseVectorIncrement = vectorIncrement.clone().multiply(-1);
		int frequencyTracker = actionFrequency; // For deciding whether to run travelAction for this interval

		double maxIterations = maxDistance / increment * 1.1;
		for (int i = 0; i < maxIterations; i++) {
			movingBoundingBox.shift(vectorIncrement);
			Vector potentialBoxCentre = movingBoundingBox.getCenter();

			if (start.distanceSquared(potentialBoxCentre) > maxDistance * maxDistance) {
				// Gone too far
				movingBoundingBox.shift(reverseVectorIncrement);
				return false;
			}

			ArrayList<Location> locationsTouching = LocationUtils.getLocationsTouching(movingBoundingBox, world);
			for (Location location : locationsTouching) {
				Block block = location.getBlock();
				BoundingBox blockBoxEstimate = block.getBoundingBox();
				Material blockMaterial = block.getType();
				if (blockBoxEstimate.overlaps(movingBoundingBox)) { // Seems liquids have empty bounding boxes similar to air, so they won't count as overlapping
					if (blockMaterial.isSolid()) { // Allow passing through non-solids like signs, grass, vines etc
						// Obstructed by solid block
						movingBoundingBox.shift(reverseVectorIncrement);
						return true;
					}
				}
			}

			// The central location of the bounding box is valid;
			// it was not reverse shifted. Run travelAction if frequency is right
			if (travelAction != null) {
				if (frequencyTracker >= actionFrequency) {
					actionFrequency = 1;

					if (FastUtils.RANDOM.nextInt(actionChance) == 0) {
						travelAction.run(potentialBoxCentre.toLocation(world));
					}
				} else {
					actionFrequency++;
				}
			}
		}

		// Neither went too far nor got obstructed (this should not happen)
		return false;
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
			// Can't fill blocks between two difefrent worlds
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
}
