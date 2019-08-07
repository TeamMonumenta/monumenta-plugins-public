package com.playmonumenta.plugins.utils;

import java.util.ArrayList;
import java.util.List;

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
import org.bukkit.block.data.type.Slab.Type;
import org.bukkit.block.data.type.Snow;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.Stairs.Shape;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Entity;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

public class LocationUtils {
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
		 * a non-air block is hit. If it's a liquid, this is allowed, otherwise it's not
		 */
		loc = loc.clone();
		for (int i = loc.getBlockY(); i > (Math.max(0, loc.getBlockY() - 50)); i--) {
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
		for (int i = loc.getBlockY(); i > (Math.max(0, loc.getBlockY() - 50)); i--) {
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

	public static boolean hasLosToLocation(Location fromLocation, Location toLocation) {
		int range = (int)fromLocation.distance(toLocation) + 1;
		Vector direction = toLocation.toVector().subtract(fromLocation.toVector()).normalize();

		BlockIterator bi = new BlockIterator(fromLocation.getWorld(), fromLocation.toVector(), direction, 0, range);

		while (bi.hasNext()) {
			Block b = bi.next();

			//  If we want to check Line of sight we want to make sure the the blocks are transparent.
			if (LocationUtils.isLosBlockingBlock(b.getType())) {
				return false;
			}
		}

		return true;
	}

	// Search a cuboid around a Location and return the first Location found with a block matching one of the given Materials
	public static Location getNearestBlock(Location center, int radius, Material... materials) {
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
		double X = (loc.getX() % 1 + 1) % 1;
		double Y = (loc.getY() % 1 + 1) % 1;
		double Z = (loc.getZ() % 1 + 1) % 1;

		switch (block.getType()) {

		// cases for individual blocks
		case SIGN:
			collides = false;
			break;
		case WALL_SIGN:
			collides = false;
			break;
		case TURTLE_EGG:
			collides = false;
			break;
		case SOUL_SAND:
			if (Y > (15.0 / 16.0)) {
				collides = false;
			}
			break;
		case GRASS_PATH:
			if (Y > (15.0 / 16.0)) {
				collides = false;
			}
			break;
		case FARMLAND:
			if (Y > (15.0 / 16.0)) {
				collides = false;
			}
			break;
		case END_PORTAL_FRAME:
			if (Y > (13.0 / 16.0)) {
				collides = false;
			}
			break;
		case CONDUIT:
			if (Y > (11.0 / 16.0) || Y < (5.0 / 16.0)
			    || X > (11.0 / 16.0) || X < (5.0 / 16.0)
			    || Z > (11.0 / 16.0) || Z < (5.0 / 16.0)) {
				collides = false;
			}
			break;
		case ENCHANTING_TABLE:
			if (Y > (12.0 / 16.0)) {
				collides = false;
			}
			break;
		case DAYLIGHT_DETECTOR:
			if (Y > (6.0 / 16.0)) {
				collides = false;
			}
			break;
		case BREWING_STAND:
			if (Y > (2.0 / 16.0)
			    && (X > (9.0 / 16.0) || X < (7.0 / 16.0))
			    && Z > (9.0 / 16.0) || Z < (7.0 / 16.0)) {
				collides = false;
			}
			break;
		case CHEST:
			if (Y > (15.0 / 16.0)
			    || X > (15.0 / 16.0) || X < ((1.0) / 16.0)
			    || Z > (15.0 / 16.0) || Z < (1.0 / 16.0)) {
				collides = false;
			}
			break;
		case TRAPPED_CHEST:
			if (Y > (15.0 / 16.0)
			    || X > (15.0 / 16.0) || X < ((1.0) / 16.0)
			    || Z > (15.0 / 16.0) || Z < (1.0 / 16.0)) {
				collides = false;
			}
			break;
		case ENDER_CHEST:
			if (Y > (15.0 / 16.0)
			    || X > (15.0 / 16.0) || X < ((1.0) / 16.0)
			    || Z > (15.0 / 16.0) || Z < (1.0 / 16.0)) {
				collides = false;
			}
			break;
		case DRAGON_EGG:
			if (X > (15.0 / 16.0) || X < (1.0 / 16.0)
			    || Z > (15.0 / 16.0) || Z < (1.0 / 16.0)) {
				collides = false;
			}
			break;
		case HOPPER:
			if (Y < (4.0 / 16.0) ||
			    (Y < (10.0 / 16.0) &&
			     (X > (12.0 / 16.0) || X < (4.0 / 16.0)
			      || Z > (12.0 / 16.0) || Z < (4.0 / 16.0)))) {
				collides = false;
			}
			break;
		case SNOW:
			int layers = ((Snow) block.getBlockData()).getLayers();
			if (Y > (2 * layers / 16.0)) {
				collides = false;
			}
			break;
		case CAKE:
			int bites = ((Cake) block.getBlockData()).getBites();
			if (Y > (7.0 / 16.0)
			    || X > (15.0 / 16.0) || X < ((1.0 + 2 * bites) / 16.0)
			    || Z > (15.0 / 16.0) || Z < (1.0 / 16.0)) {
				collides = false;
			}
			break;

		case PISTON_HEAD:
			BlockFace pistonface = ((Directional) block.getBlockData()).getFacing();
			if (pistonface == BlockFace.EAST) {
				if (X < (12.0 / 16.0) &&
				    (Y > (10.0 / 16.0) || Y < (6.0 / 16.0)
				     || Z > (10.0 / 16.0) || Z < (6.0 / 16.0))) {
					collides = false;
				}
			} else if (pistonface == BlockFace.WEST) {
				if (X > (4.0 / 16.0) &&
				    (Y > (10.0 / 16.0) || Y < (6.0 / 16.0)
				     || Z > (10.0 / 16.0) || Z < (6.0 / 16.0))) {
					collides = false;
				}
			} else if (pistonface == BlockFace.NORTH) {
				if (Z > (4.0 / 16.0) &&
				    (Y > (10.0 / 16.0) || Y < (6.0 / 16.0)
				     || X > (10.0 / 16.0) || X < (6.0 / 16.0))) {
					collides = false;
				}
			} else if (pistonface == BlockFace.SOUTH) {
				if (Z < (12.0 / 16.0) &&
				    (Y > (10.0 / 16.0) || Y < (6.0 / 16.0)
				     || X > (10.0 / 16.0) || X < (6.0 / 16.0))) {
					collides = false;
				}
			} else if (pistonface == BlockFace.UP) {
				if (Y < (12.0 / 16.0) &&
				    (Z > (10.0 / 16.0) || Z < (6.0 / 16.0)
				     || X > (10.0 / 16.0) || X < (6.0 / 16.0))) {
					collides = false;
				}
			} else if (pistonface == BlockFace.DOWN) {
				if (Y > (4.0 / 16.0) &&
				    (Z > (10.0 / 16.0) || Z < (6.0 / 16.0)
				     || X > (10.0 / 16.0) || X < (6.0 / 16.0))) {
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
				if (((Slab) block.getBlockData()).getType() == Type.BOTTOM) {
					if (Y > 0.5) {
						collides = false;
					}
				} else if (((Slab) block.getBlockData()).getType() == Type.TOP) {
					if (Y < 0.5) {
						collides = false;
					}
				}
			} else if (block.getBlockData() instanceof Stairs) {
				Stairs stair = ((Stairs) block.getBlockData());
				if (stair.getHalf() == Half.BOTTOM) {
					if (Y > 0.5) {
						collides = false;
					}
				} else if (stair.getHalf() == Half.TOP) {
					if (Y < 0.5) {
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
						if (Z < 0.5) {
							collides = true;
						}
					}
					if (stair.getFacing() == BlockFace.SOUTH
					    || (stair.getFacing() == BlockFace.EAST
					        && stair.getShape() == Shape.INNER_RIGHT)
					    || (stair.getFacing() == BlockFace.WEST
					        && stair.getShape() == Shape.INNER_LEFT)) {
						if (Z > 0.5) {
							collides = true;
						}
					}
					if (stair.getFacing() == BlockFace.WEST
					    || (stair.getFacing() == BlockFace.SOUTH
					        && stair.getShape() == Shape.INNER_RIGHT)
					    || (stair.getFacing() == BlockFace.NORTH
					        && stair.getShape() == Shape.INNER_LEFT)) {
						if (X < 0.5) {
							collides = true;
						}
					}
					if (stair.getFacing() == BlockFace.EAST
					    || (stair.getFacing() == BlockFace.NORTH
					        && stair.getShape() == Shape.INNER_RIGHT)
					    || (stair.getFacing() == BlockFace.SOUTH
					        && stair.getShape() == Shape.INNER_LEFT)) {
						if (X > 0.5) {
							collides = true;
						}
					}
				} else {
					if ((stair.getFacing() == BlockFace.NORTH
					     && stair.getShape() == Shape.OUTER_RIGHT)
					    || (stair.getFacing() == BlockFace.EAST
					        && stair.getShape() == Shape.OUTER_LEFT)) {
						if (Z < 0.5 && X > 0.5) {
							collides = true;
						}
					} else if ((stair.getFacing() == BlockFace.EAST
					            && stair.getShape() == Shape.OUTER_RIGHT)
					           || (stair.getFacing() == BlockFace.SOUTH
					               && stair.getShape() == Shape.OUTER_LEFT)) {
						if (Z > 0.5 && X > 0.5) {
							collides = true;
						}
					} else if ((stair.getFacing() == BlockFace.SOUTH
					            && stair.getShape() == Shape.OUTER_RIGHT)
					           || (stair.getFacing() == BlockFace.WEST
					               && stair.getShape() == Shape.OUTER_LEFT)) {
						if (Z > 0.5 && X < 0.5) {
							collides = true;
						}
					} else if ((stair.getFacing() == BlockFace.WEST
					            && stair.getShape() == Shape.OUTER_RIGHT)
					           || (stair.getFacing() == BlockFace.NORTH
					               && stair.getShape() == Shape.OUTER_LEFT)) {
						if (Z < 0.5 && X < 0.5) {
							collides = true;
						}
					}
				}
			} else if (block.getBlockData() instanceof GlassPane || block.getType() == Material.IRON_BARS) {
				MultipleFacing pane = ((MultipleFacing) block.getBlockData());
				if (Z > (9.0 / 16.0)
				    || Z < (7.0 / 16.0)
				    || X > (9.0 / 16.0)
				    || X < (7.0 / 16.0)) {
					collides = false;
				}
				if (!collides && pane.hasFace(BlockFace.NORTH)) {
					if (Z < (9.0 / 16.0)
					    && X > (7.0 / 16.0)
					    && X < (9.0 / 16.0)) {
						collides = true;
					}
				}
				if (!collides && pane.hasFace(BlockFace.SOUTH)) {
					if (Z > (7.0 / 16.0)
					    && X > (7.0 / 16.0)
					    && X < (9.0 / 16.0)) {
						collides = true;
					}
				}
				if (!collides && pane.hasFace(BlockFace.EAST)) {
					if (X > (7.0 / 16.0)
					    && Z > (7.0 / 16.0)
					    && Z < (9.0 / 16.0)) {
						collides = true;
					}
				}
				if (!collides && pane.hasFace(BlockFace.WEST)) {
					if (X < (9.0 / 16.0)
					    && Z > (7.0 / 16.0)
					    && Z < (9.0 / 16.0)) {
						collides = true;
					}
				}
			} else if (block.getBlockData() instanceof Fence) {
				int width = 4;
				if (block.getType() == Material.COBBLESTONE_WALL
				    || block.getType() == Material.COBBLESTONE_WALL) {
					width = 8;
				}
				Fence fence = ((Fence) block.getBlockData());
				if (Z > ((8.0 + width / 2) / 16.0)
				    || Z < ((8.0 - width / 2) / 16.0)
				    || X > ((8.0 + width / 2) / 16.0)
				    || X < ((8.0 - width / 2) / 16.0)) {
					collides = false;
				}
				if (!collides && fence.hasFace(BlockFace.NORTH)) {
					if (Z < ((8.0 + width / 2) / 16.0)
					    && X > ((8.0 - width / 2) / 16.0)
					    && X < ((8.0 + width / 2) / 16.0)) {
						collides = true;
					}
				}
				if (!collides && fence.hasFace(BlockFace.SOUTH)) {
					if (Z > ((8.0 - width / 2) / 16.0)
					    && X > ((8.0 - width / 2) / 16.0)
					    && X < ((8.0 + width / 2) / 16.0)) {
						collides = true;
					}
				}
				if (!collides && fence.hasFace(BlockFace.EAST)) {
					if (X > ((8.0 - width / 2) / 16.0)
					    && Z > ((8.0 - width / 2) / 16.0)
					    && Z < ((8.0 + width / 2) / 16.0)) {
						collides = true;
					}
				}
				if (!collides && fence.hasFace(BlockFace.EAST)) {
					if (X < ((8.0 + width / 2) / 16.0)
					    && Z > ((8.0 - width / 2) / 16.0)
					    && Z < ((8.0 + width / 2) / 16.0)) {
						collides = true;
					}
				}
			} else if (block.getBlockData() instanceof Gate) {
				Gate gate = ((Gate) block.getBlockData());
				boolean open = gate.isOpen();
				BlockFace face = gate.getFacing();
				if (Y < (5.0 / 16.0)) {
					collides = false;
				}
				if (!open) {
					if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
						if (Z > (9.0 / 16.0)
						    || Z < (7.0 / 16.0)) {
							collides = false;
						}
					} else {
						if (X > (9.0 / 16.0)
						    || X < (7.0 / 16.0)) {
							collides = false;
						}
					}
				} else {
					if (face == BlockFace.NORTH) {
						if (!(Z < (9.0 / 16.0)
						      && (X < (2.0 / 16.0)
						          || X > (14.0 / 16.0)))) {
							collides = false;
						}
					} else if (face == BlockFace.SOUTH) {
						if (!(Z > (7.0 / 16.0)
						      && (X < (2.0 / 16.0)
						          || X > (14.0 / 16.0)))) {
							collides = false;
						}
					} else if (face == BlockFace.EAST) {
						if (!(X > (7.0 / 16.0)
						      && (Z < (2.0 / 16.0)
						          || Z > (14.0 / 16.0)))) {
							collides = false;
						}
					} else if (face == BlockFace.WEST) {
						if (!(X < (9.0 / 16.0)
						      && (Z < (2.0 / 16.0)
						          || Z > (14.0 / 16.0)))) {
							collides = false;
						}
					}
				}
			} else if (block.getBlockData() instanceof TrapDoor) {
				TrapDoor trapdoor = ((TrapDoor) block.getBlockData());
				boolean open = trapdoor.isOpen();
				if (!open) {
					if (trapdoor.getHalf() == Half.TOP) {
						if (Y < (13.0 / 16.0)) {
							collides = false;
						}
					} else {
						if (Y > (3.0 / 16.0)) {
							collides = false;
						}
					}
				} else {
					if (trapdoor.getFacing() == BlockFace.NORTH) {
						if (Z < (13.0 / 16.0)) {
							collides = false;
						}
					} else if (trapdoor.getFacing() == BlockFace.SOUTH) {
						if (Z > (3.0 / 16.0)) {
							collides = false;
						}
					} else if (trapdoor.getFacing() == BlockFace.EAST) {
						if (X > (3.0 / 16.0)) {
							collides = false;
						}
					} else if (trapdoor.getFacing() == BlockFace.WEST) {
						if (X < (13.0 / 16.0)) {
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
					if (X > (3.0 / 16.0)) {
						collides = false;
					}
				} else if (face == BlockFace.WEST) {
					if (X < (13.0 / 16.0)) {
						collides = false;
					}
				} else if (face == BlockFace.NORTH) {
					if (Z < (13.0 / 16.0)) {
						collides = false;
					}
				} else if (face == BlockFace.SOUTH) {
					if (Z > (3.0 / 16.0)) {
						collides = false;
					}
				}
			} else if (block.getBlockData() instanceof Piston) {
				Piston piston = ((Piston) block.getBlockData());
				if (piston.isExtended()) {
					BlockFace face = piston.getFacing();
					if (face == BlockFace.EAST) {
						if (X > (12.0 / 16.0) &&
						    (Y > (10.0 / 16.0) || Y < (6.0 / 16.0)
						     || Z > (10.0 / 16.0) || Z < (6.0 / 16.0))) {
							collides = false;
						}
					} else if (face == BlockFace.WEST) {
						if (X < (4.0 / 16.0) &&
						    (Y > (10.0 / 16.0) || Y < (6.0 / 16.0)
						     || Z > (10.0 / 16.0) || Z < (6.0 / 16.0))) {
							collides = false;
						}
					} else if (face == BlockFace.NORTH) {
						if (Z < (4.0 / 16.0) &&
						    (Y > (10.0 / 16.0) || Y < (6.0 / 16.0)
						     || X > (10.0 / 16.0) || X < (6.0 / 16.0))) {
							collides = false;
						}
					} else if (face == BlockFace.SOUTH) {
						if (Z > (12.0 / 16.0) &&
						    (Y > (10.0 / 16.0) || Y < (6.0 / 16.0)
						     || X > (10.0 / 16.0) || X < (6.0 / 16.0))) {
							collides = false;
						}
					} else if (face == BlockFace.UP) {
						if (Y > (12.0 / 16.0) &&
						    (Z > (10.0 / 16.0) || Z < (6.0 / 16.0)
						     || X > (10.0 / 16.0) || X < (6.0 / 16.0))) {
							collides = false;
						}
					} else if (face == BlockFace.DOWN) {
						if (Y < (4.0 / 16.0) &&
						    (Z > (10.0 / 16.0) || Z < (6.0 / 16.0)
						     || X > (10.0 / 16.0) || X < (6.0 / 16.0))) {
							collides = false;
						}
					}
				}
			} else if (block.getBlockData() instanceof Bed) {
				if (Y > (9.0 / 16.0)) {
					collides = false;
				}
			} else if (block.getType() == Material.ANVIL
			           || block.getType() == Material.CHIPPED_ANVIL
			           || block.getType() == Material.DAMAGED_ANVIL) {
				BlockFace face = ((Directional) block.getBlockData()).getFacing();
				if (face == BlockFace.EAST || face == BlockFace.WEST) {
					if (Y < (4.0 / 16.0) &&
					    (Z > (14.0 / 16.0) || Z < (2.0 / 16.0)
					     || X > (14.0 / 16.0) || X < (2.0 / 16.0))) {
						collides = false;
					} else if ((Y >= (4.0 / 16.0) && Y < (5.0 / 16.0)) &&
					           (Z > (12.0 / 16.0) || Z < (4.0 / 16.0)
					            || X > (13.0 / 16.0) || X < (3.0 / 16.0))) {
						collides = false;
					} else if ((Y >= (5.0 / 16.0) && Y < (10.0 / 16.0)) &&
					           (Z > (10.0 / 16.0) || Z < (6.0 / 16.0)
					            || X > (12.0 / 16.0) || X < (4.0 / 16.0))) {
						collides = false;
					} else if ((Y >= (10.0 / 16.0)) &&
					           (Z > (13.0 / 16.0) || Z < (3.0 / 16.0))) {
						collides = false;
					}
				} else {
					if (Y < (4.0 / 16.0) &&
					    (X > (14.0 / 16.0) || X < (2.0 / 16.0)
					     || Z > (14.0 / 16.0) || Z < (2.0 / 16.0))) {
						collides = false;
					} else if ((Y >= (4.0 / 16.0) && Y < (5.0 / 16.0)) &&
					           (X > (12.0 / 16.0) || X < (4.0 / 16.0)
					            || Z > (13.0 / 16.0) || Z < (3.0 / 16.0))) {
						collides = false;
					} else if ((Y >= (5.0 / 16.0) && Y < (10.0 / 16.0)) &&
					           (X > (10.0 / 16.0) || X < (6.0 / 16.0)
					            || Z > (12.0 / 16.0) || Z < (4.0 / 16.0))) {
						collides = false;
					} else if ((Y >= (10.0 / 16.0)) &&
					           (X > (13.0 / 16.0) || X < (3.0 / 16.0))) {
						collides = false;
					}
				}
			}
		}
		return collides;
	}

}
