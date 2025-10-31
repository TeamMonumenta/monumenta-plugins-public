package com.playmonumenta.plugins.utils;

import com.destroystokyo.paper.MaterialSetTag;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Fence;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.Wall;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BlockUtils {
	private static Set<Material> of(Material first, Material... materials) {
		return Collections.unmodifiableSet(EnumSet.of(first, materials));
	}

	private static final Set<Material> ALT_WATER_SOURCES = of(
		Material.BUBBLE_COLUMN,
		Material.KELP,
		Material.KELP_PLANT,
		Material.SEAGRASS,
		Material.TALL_SEAGRASS
	);

	private static final Set<Material> MECHANICAL_BLOCKS = of(
		Material.AIR,
		Material.CAVE_AIR,
		Material.VOID_AIR,
		Material.STRUCTURE_VOID,
		Material.STRUCTURE_BLOCK,
		Material.JIGSAW,
		Material.COMMAND_BLOCK,
		Material.CHAIN_COMMAND_BLOCK,
		Material.REPEATING_COMMAND_BLOCK,
		Material.BEDROCK,
		Material.BARRIER,
		Material.SPAWNER,
		Material.LIGHT,
		Material.END_PORTAL
	);

	private static final Set<Material> ENV_HAZARDS_FOR_MOBS = of(
		Material.COBWEB,
		Material.SLIME_BLOCK,
		Material.HONEY_BLOCK,
		Material.SOUL_SAND,
		Material.RAIL,
		Material.POWERED_RAIL,
		Material.DETECTOR_RAIL,
		Material.ACTIVATOR_RAIL,
		Material.POWDER_SNOW,
		Material.END_ROD
	);

	public static final Set<Material> VALUABLES = of(
		Material.SHULKER_BOX,
		Material.BLACK_SHULKER_BOX,
		Material.BLUE_SHULKER_BOX,
		Material.BROWN_SHULKER_BOX,
		Material.CYAN_SHULKER_BOX,
		Material.GREEN_SHULKER_BOX,
		Material.LIME_SHULKER_BOX,
		Material.LIGHT_BLUE_SHULKER_BOX,
		Material.LIGHT_GRAY_SHULKER_BOX,
		Material.MAGENTA_SHULKER_BOX,
		Material.ORANGE_SHULKER_BOX,
		Material.PINK_SHULKER_BOX,
		Material.PURPLE_SHULKER_BOX,
		Material.RED_SHULKER_BOX,
		Material.WHITE_SHULKER_BOX,
		Material.YELLOW_SHULKER_BOX,
		Material.GRAY_SHULKER_BOX,

		Material.CHEST,
		Material.TRAPPED_CHEST,

		Material.IRON_ORE,
		Material.IRON_BLOCK,
		Material.DEEPSLATE_IRON_ORE,
		Material.RAW_IRON,
		Material.RAW_IRON_BLOCK,

		Material.COPPER_ORE,
		Material.DEEPSLATE_COPPER_ORE,
		Material.RAW_COPPER,
		Material.RAW_COPPER_BLOCK,
		Material.COPPER_BLOCK,

		Material.GOLD_ORE,
		Material.DEEPSLATE_GOLD_ORE,
		Material.RAW_GOLD,
		Material.RAW_GOLD_BLOCK,
		Material.GOLD_BLOCK,
		Material.NETHER_GOLD_ORE,
		Material.GILDED_BLACKSTONE,

		Material.LAPIS_ORE,
		Material.LAPIS_BLOCK,

		Material.DEEPSLATE_LAPIS_ORE,
		Material.EMERALD_ORE,
		Material.DEEPSLATE_EMERALD_ORE,
		Material.EMERALD_BLOCK,

		Material.DIAMOND_ORE,
		Material.DEEPSLATE_DIAMOND_ORE,
		Material.DIAMOND_BLOCK,

		Material.NETHERITE_BLOCK,

		Material.ANVIL,
		Material.CHIPPED_ANVIL,
		Material.DAMAGED_ANVIL
	);

	public static final Set<Material> CONTAINERS = of(
		Material.SHULKER_BOX,
		Material.BLACK_SHULKER_BOX,
		Material.BLUE_SHULKER_BOX,
		Material.BROWN_SHULKER_BOX,
		Material.CYAN_SHULKER_BOX,
		Material.GREEN_SHULKER_BOX,
		Material.LIME_SHULKER_BOX,
		Material.LIGHT_BLUE_SHULKER_BOX,
		Material.LIGHT_GRAY_SHULKER_BOX,
		Material.MAGENTA_SHULKER_BOX,
		Material.ORANGE_SHULKER_BOX,
		Material.PINK_SHULKER_BOX,
		Material.PURPLE_SHULKER_BOX,
		Material.RED_SHULKER_BOX,
		Material.WHITE_SHULKER_BOX,
		Material.YELLOW_SHULKER_BOX,
		Material.GRAY_SHULKER_BOX,

		Material.CHEST,
		Material.TRAPPED_CHEST,
		Material.BARREL,

		Material.FURNACE,
		Material.SMOKER,
		Material.BLAST_FURNACE,

		Material.DISPENSER,
		Material.DROPPER,

		Material.BREWING_STAND,

		Material.JUKEBOX,
		Material.LECTERN
	);

	public static final Set<Material> TORCHES = of(
		Material.TORCH,
		Material.WALL_TORCH
	);

	/**
	 * Replaces {@link Material#isInteractable()} as it is deprecated.
	 * <p>
	 * TODO: maybe replace these with tags.
	 *
	 * @see Material#isInteractable()
	 */
	@SuppressWarnings({"deprecation", "UnstableApiUsage"})
	public static final Set<Material> INTERACTABLE = of(
		Material.ACACIA_BUTTON,
		Material.ACACIA_DOOR,
		Material.ACACIA_FENCE,
		Material.ACACIA_FENCE_GATE,
		Material.ACACIA_HANGING_SIGN,
		Material.ACACIA_SIGN,
		Material.ACACIA_TRAPDOOR,
		Material.ACACIA_WALL_HANGING_SIGN,
		Material.ACACIA_WALL_SIGN,
		Material.ANVIL,
		Material.BAMBOO_BUTTON,
		Material.BAMBOO_DOOR,
		Material.BAMBOO_FENCE,
		Material.BAMBOO_FENCE_GATE,
		Material.BAMBOO_HANGING_SIGN,
		Material.BAMBOO_SIGN,
		Material.BAMBOO_TRAPDOOR,
		Material.BAMBOO_WALL_HANGING_SIGN,
		Material.BAMBOO_WALL_SIGN,
		Material.BARREL,
		Material.BEACON,
		Material.BEEHIVE,
		Material.BEE_NEST,
		Material.BELL,
		Material.BIRCH_BUTTON,
		Material.BIRCH_DOOR,
		Material.BIRCH_FENCE,
		Material.BIRCH_FENCE_GATE,
		Material.BIRCH_HANGING_SIGN,
		Material.BIRCH_SIGN,
		Material.BIRCH_TRAPDOOR,
		Material.BIRCH_WALL_HANGING_SIGN,
		Material.BIRCH_WALL_SIGN,
		Material.BLACK_BED,
		Material.BLACK_CANDLE,
		Material.BLACK_CANDLE_CAKE,
		Material.BLACK_SHULKER_BOX,
		Material.BLAST_FURNACE,
		Material.BLUE_BED,
		Material.BLUE_CANDLE,
		Material.BLUE_CANDLE_CAKE,
		Material.BLUE_SHULKER_BOX,
		Material.BREWING_STAND,
		Material.BROWN_BED,
		Material.BROWN_CANDLE,
		Material.BROWN_CANDLE_CAKE,
		Material.BROWN_SHULKER_BOX,
		Material.CAKE,
		Material.CAMPFIRE,
		Material.CANDLE,
		Material.CANDLE_CAKE,
		Material.CARTOGRAPHY_TABLE,
		Material.CAULDRON,
		Material.CAVE_VINES,
		Material.CAVE_VINES_PLANT,
		Material.CHAIN_COMMAND_BLOCK,
		Material.CHERRY_BUTTON,
		Material.CHERRY_DOOR,
		Material.CHERRY_FENCE,
		Material.CHERRY_FENCE_GATE,
		Material.CHERRY_HANGING_SIGN,
		Material.CHERRY_SIGN,
		Material.CHERRY_TRAPDOOR,
		Material.CHERRY_WALL_HANGING_SIGN,
		Material.CHERRY_WALL_SIGN,
		Material.CHEST,
		Material.CHIPPED_ANVIL,
		Material.CHISELED_BOOKSHELF,
		Material.COMMAND_BLOCK,
		Material.COMPARATOR,
		Material.COMPOSTER,
		Material.COPPER_DOOR,
		Material.COPPER_TRAPDOOR,
		Material.CRAFTER,
		Material.CRAFTING_TABLE,
		Material.CRIMSON_BUTTON,
		Material.CRIMSON_DOOR,
		Material.CRIMSON_FENCE,
		Material.CRIMSON_FENCE_GATE,
		Material.CRIMSON_HANGING_SIGN,
		Material.CRIMSON_SIGN,
		Material.CRIMSON_TRAPDOOR,
		Material.CRIMSON_WALL_HANGING_SIGN,
		Material.CRIMSON_WALL_SIGN,
		Material.CYAN_BED,
		Material.CYAN_CANDLE,
		Material.CYAN_CANDLE_CAKE,
		Material.CYAN_SHULKER_BOX,
		Material.DAMAGED_ANVIL,
		Material.DARK_OAK_BUTTON,
		Material.DARK_OAK_DOOR,
		Material.DARK_OAK_FENCE,
		Material.DARK_OAK_FENCE_GATE,
		Material.DARK_OAK_HANGING_SIGN,
		Material.DARK_OAK_SIGN,
		Material.DARK_OAK_TRAPDOOR,
		Material.DARK_OAK_WALL_HANGING_SIGN,
		Material.DARK_OAK_WALL_SIGN,
		Material.DAYLIGHT_DETECTOR,
		Material.DECORATED_POT,
		Material.DEEPSLATE_REDSTONE_ORE,
		Material.DISPENSER,
		Material.DRAGON_EGG,
		Material.DROPPER,
		Material.ENCHANTING_TABLE,
		Material.ENDER_CHEST,
		Material.EXPOSED_COPPER_DOOR,
		Material.EXPOSED_COPPER_TRAPDOOR,
		Material.FLETCHING_TABLE,
		Material.FLOWER_POT,
		Material.FURNACE,
		Material.GRAY_BED,
		Material.GRAY_CANDLE,
		Material.GRAY_CANDLE_CAKE,
		Material.GRAY_SHULKER_BOX,
		Material.GREEN_BED,
		Material.GREEN_CANDLE,
		Material.GREEN_CANDLE_CAKE,
		Material.GREEN_SHULKER_BOX,
		Material.GRINDSTONE,
		Material.HOPPER,
		Material.IRON_DOOR,
		Material.IRON_TRAPDOOR,
		Material.JIGSAW,
		Material.JUKEBOX,
		Material.JUNGLE_BUTTON,
		Material.JUNGLE_DOOR,
		Material.JUNGLE_FENCE,
		Material.JUNGLE_FENCE_GATE,
		Material.JUNGLE_HANGING_SIGN,
		Material.JUNGLE_SIGN,
		Material.JUNGLE_TRAPDOOR,
		Material.JUNGLE_WALL_HANGING_SIGN,
		Material.JUNGLE_WALL_SIGN,
		Material.LAVA_CAULDRON,
		Material.LECTERN,
		Material.LEVER,
		Material.LIGHT,
		Material.LIGHT_BLUE_BED,
		Material.LIGHT_BLUE_CANDLE,
		Material.LIGHT_BLUE_CANDLE_CAKE,
		Material.LIGHT_BLUE_SHULKER_BOX,
		Material.LIGHT_GRAY_BED,
		Material.LIGHT_GRAY_CANDLE,
		Material.LIGHT_GRAY_CANDLE_CAKE,
		Material.LIGHT_GRAY_SHULKER_BOX,
		Material.LIME_BED,
		Material.LIME_CANDLE,
		Material.LIME_CANDLE_CAKE,
		Material.LIME_SHULKER_BOX,
		Material.LOOM,
		Material.MAGENTA_BED,
		Material.MAGENTA_CANDLE,
		Material.MAGENTA_CANDLE_CAKE,
		Material.MAGENTA_SHULKER_BOX,
		Material.MANGROVE_BUTTON,
		Material.MANGROVE_DOOR,
		Material.MANGROVE_FENCE,
		Material.MANGROVE_FENCE_GATE,
		Material.MANGROVE_HANGING_SIGN,
		Material.MANGROVE_SIGN,
		Material.MANGROVE_TRAPDOOR,
		Material.MANGROVE_WALL_HANGING_SIGN,
		Material.MANGROVE_WALL_SIGN,
		Material.MOVING_PISTON,
		Material.NETHER_BRICK_FENCE,
		Material.NOTE_BLOCK,
		Material.OAK_BUTTON,
		Material.OAK_DOOR,
		Material.OAK_FENCE,
		Material.OAK_FENCE_GATE,
		Material.OAK_HANGING_SIGN,
		Material.OAK_SIGN,
		Material.OAK_TRAPDOOR,
		Material.OAK_WALL_HANGING_SIGN,
		Material.OAK_WALL_SIGN,
		Material.ORANGE_BED,
		Material.ORANGE_CANDLE,
		Material.ORANGE_CANDLE_CAKE,
		Material.ORANGE_SHULKER_BOX,
		Material.OXIDIZED_COPPER_DOOR,
		Material.OXIDIZED_COPPER_TRAPDOOR,
		Material.PINK_BED,
		Material.PINK_CANDLE,
		Material.PINK_CANDLE_CAKE,
		Material.PINK_SHULKER_BOX,
		Material.POLISHED_BLACKSTONE_BUTTON,
		Material.POTTED_ACACIA_SAPLING,
		Material.POTTED_ALLIUM,
		Material.POTTED_AZALEA_BUSH,
		Material.POTTED_AZURE_BLUET,
		Material.POTTED_BAMBOO,
		Material.POTTED_BIRCH_SAPLING,
		Material.POTTED_BLUE_ORCHID,
		Material.POTTED_BROWN_MUSHROOM,
		Material.POTTED_CACTUS,
		Material.POTTED_CHERRY_SAPLING,
		Material.POTTED_CORNFLOWER,
		Material.POTTED_CRIMSON_FUNGUS,
		Material.POTTED_CRIMSON_ROOTS,
		Material.POTTED_DANDELION,
		Material.POTTED_DARK_OAK_SAPLING,
		Material.POTTED_DEAD_BUSH,
		Material.POTTED_FERN,
		Material.POTTED_FLOWERING_AZALEA_BUSH,
		Material.POTTED_JUNGLE_SAPLING,
		Material.POTTED_LILY_OF_THE_VALLEY,
		Material.POTTED_MANGROVE_PROPAGULE,
		Material.POTTED_OAK_SAPLING,
		Material.POTTED_ORANGE_TULIP,
		Material.POTTED_OXEYE_DAISY,
		Material.POTTED_PINK_TULIP,
		Material.POTTED_POPPY,
		Material.POTTED_RED_MUSHROOM,
		Material.POTTED_RED_TULIP,
		Material.POTTED_SPRUCE_SAPLING,
		Material.POTTED_TORCHFLOWER,
		Material.POTTED_WARPED_FUNGUS,
		Material.POTTED_WARPED_ROOTS,
		Material.POTTED_WHITE_TULIP,
		Material.POTTED_WITHER_ROSE,
		Material.POWDER_SNOW_CAULDRON,
		Material.PUMPKIN,
		Material.PURPLE_BED,
		Material.PURPLE_CANDLE,
		Material.PURPLE_CANDLE_CAKE,
		Material.PURPLE_SHULKER_BOX,
		Material.REDSTONE_ORE,
		Material.REDSTONE_WIRE,
		Material.RED_BED,
		Material.RED_CANDLE,
		Material.RED_CANDLE_CAKE,
		Material.RED_SHULKER_BOX,
		Material.REPEATER,
		Material.REPEATING_COMMAND_BLOCK,
		Material.RESPAWN_ANCHOR,
		Material.SHULKER_BOX,
		Material.SMITHING_TABLE,
		Material.SMOKER,
		Material.SOUL_CAMPFIRE,
		Material.SPRUCE_BUTTON,
		Material.SPRUCE_DOOR,
		Material.SPRUCE_FENCE,
		Material.SPRUCE_FENCE_GATE,
		Material.SPRUCE_HANGING_SIGN,
		Material.SPRUCE_SIGN,
		Material.SPRUCE_TRAPDOOR,
		Material.SPRUCE_WALL_HANGING_SIGN,
		Material.SPRUCE_WALL_SIGN,
		Material.STONECUTTER,
		Material.STONE_BUTTON,
		Material.STRUCTURE_BLOCK,
		Material.SWEET_BERRY_BUSH,
		Material.TNT,
		Material.TRAPPED_CHEST,
		Material.WARPED_BUTTON,
		Material.WARPED_DOOR,
		Material.WARPED_FENCE,
		Material.WARPED_FENCE_GATE,
		Material.WARPED_HANGING_SIGN,
		Material.WARPED_SIGN,
		Material.WARPED_TRAPDOOR,
		Material.WARPED_WALL_HANGING_SIGN,
		Material.WARPED_WALL_SIGN,
		Material.WATER_CAULDRON,
		Material.WAXED_COPPER_DOOR,
		Material.WAXED_COPPER_TRAPDOOR,
		Material.WAXED_EXPOSED_COPPER_DOOR,
		Material.WAXED_EXPOSED_COPPER_TRAPDOOR,
		Material.WAXED_OXIDIZED_COPPER_DOOR,
		Material.WAXED_OXIDIZED_COPPER_TRAPDOOR,
		Material.WAXED_WEATHERED_COPPER_DOOR,
		Material.WAXED_WEATHERED_COPPER_TRAPDOOR,
		Material.WEATHERED_COPPER_DOOR,
		Material.WEATHERED_COPPER_TRAPDOOR,
		Material.WHITE_BED,
		Material.WHITE_CANDLE,
		Material.WHITE_CANDLE_CAKE,
		Material.WHITE_SHULKER_BOX,
		Material.YELLOW_BED,
		Material.YELLOW_CANDLE,
		Material.YELLOW_CANDLE_CAKE,
		Material.YELLOW_SHULKER_BOX
	);

	public static final Set<BlockFace> CARTESIAN_BLOCK_FACES = Arrays.stream(BlockFace.values()).filter(BlockFace::isCartesian).collect(Collectors.toSet());

	public static boolean isLosBlockingBlock(Block block) {
		return isLosBlockingBlock(block.getType());
	}

	public static boolean isLosBlockingBlock(Material mat) {
		return mat.isOccluding();
	}

	public static boolean isPathBlockingBlock(Material mat) {
		return mat.isSolid() || mat.equals(Material.LAVA);
	}

	public static boolean isWaterlogged(Block block) {
		return isWaterlogged(block.getState());
	}

	public static boolean isWaterlogged(BlockState block) {
		BlockData data = block.getBlockData();
		if (data instanceof Waterlogged waterlogged) {
			return waterlogged.isWaterlogged();
		}
		return false;
	}

	public static boolean isMechanicalBlock(Material material) {
		return MECHANICAL_BLOCKS.contains(material);
	}

	public static boolean isEnvHazardForMobs(Material material) {
		return ENV_HAZARDS_FOR_MOBS.contains(material);
	}

	/**
	 * Special list of blocks that mobs either cannot jump or pathfind over
	 *
	 * @param blockData BlockData of a block to test
	 * @return True if a mob cannot jump or pathfind over the block
	 */
	public static boolean mobCannotPathfindOver(final BlockData blockData) {
		return blockData instanceof Fence || blockData instanceof Gate || blockData instanceof Wall;
	}

	public static boolean isValuableBlock(Material material) {
		return VALUABLES.contains(material);
	}

	public static boolean isContainer(Material material) {
		return CONTAINERS.contains(material);
	}

	public static boolean isNonEmptyContainer(Block block) {
		return CONTAINERS.contains(block.getType()) && block.getState() instanceof BlockInventoryHolder inventoryHolder && !inventoryHolder.getInventory().isEmpty();
	}

	public static boolean isWaterSource(Block block) {
		return isWaterSource(block.getState());
	}

	public static boolean isWaterSource(BlockState block) {
		if (isWaterlogged(block)) {
			return true;
		}
		if (ALT_WATER_SOURCES.contains(block.getType())) {
			return true;
		}
		if (block.getType().equals(Material.WATER) && block.getBlockData() instanceof Levelled levelled) {
			return levelled.getLevel() == 0;
		}
		return false;
	}

	public static boolean containsWater(Block block) {
		return containsWater(block.getState());
	}

	public static boolean containsWater(BlockState block) {
		if (isWaterlogged(block)) {
			return true;
		}
		Material mat = block.getType();
		return mat.equals(Material.WATER) || ALT_WATER_SOURCES.contains(mat);
	}

	public static boolean isRail(Block block) {
		return isRail(block.getState());
	}

	public static boolean isRail(BlockState block) {
		BlockData data = block.getBlockData();
		return data instanceof Rail;
	}

	public static boolean isTorch(Block block) {
		return TORCHES.contains(block.getType());
	}

	public static boolean isClimbable(Block block) {
		return isClimbable(block.getType());
	}

	public static boolean isClimbable(Material material) {
		return MaterialSetTag.CLIMBABLE.isTagged(material);
	}

	public static boolean isBouncy(Material material) {
		return MaterialSetTag.BEDS.isTagged(material) || Material.SLIME_BLOCK.equals(material);
	}

	public static boolean canBeBroken(Block block) {
		if (block.getType().getHardness() == -1.0F) {
			return false;
		}
		Location blockLoc = block.getLocation();
		if (ZoneUtils.hasZoneProperty(blockLoc, ZoneUtils.ZoneProperty.BLOCKBREAK_DISABLED)) {
			return false;
		}
		return !blockLoc.clone().subtract(0, 1, 0).getBlock().getType().equals(Material.BEDROCK);
	}

	public static BlockFace getCardinalBlockFace(Entity entity) {
		return getCardinalBlockFace(entity.getLocation().getDirection());
	}

	// Returns NORTH, EAST, SOUTH, or WEST closest to the direction given
	public static BlockFace getCardinalBlockFace(Vector direction) {
		Vector dir = direction.clone().setY(0).normalize();
		return Stream.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)
			.min(Comparator.comparingDouble(bf -> bf.getDirection().distance(dir))).orElse(BlockFace.NORTH);
	}

	public static Location getCenterBlockLocation(Block block) {
		return block.getLocation().add(0.5, 0.5, 0.5);
	}

	public static Location getCenteredBlockBaseLocation(Block block) {
		return block.getLocation().add(0.5, 0, 0.5);
	}

	public static int taxiCabDistance(Block block1, Block block2) {
		return Math.abs(block2.getX() - block1.getX()) + Math.abs(block2.getY() - block1.getY()) + Math.abs(block2.getZ() - block1.getZ());
	}

	public static boolean isExposed(Block block) {
		return !block.getRelative(BlockFace.UP).isSolid()
			|| !block.getRelative(BlockFace.DOWN).isSolid()
			|| !block.getRelative(BlockFace.NORTH).isSolid()
			|| !block.getRelative(BlockFace.SOUTH).isSolid()
			|| !block.getRelative(BlockFace.EAST).isSolid()
			|| !block.getRelative(BlockFace.WEST).isSolid();
	}

	private static class Node {
		private final Block mBlock;
		private int mDistanceTo;
		private final int mDistanceFrom;

		private Node(Block block, @Nullable Node parent, Block end) {
			mBlock = block;
			if (parent == null) {
				mDistanceTo = 0;
			} else {
				mDistanceTo = parent.mDistanceTo + 1;
			}
			mDistanceFrom = taxiCabDistance(block, end);
		}

		private int getHeuristic() {
			return mDistanceTo + mDistanceFrom;
		}

		private void updateParentIfImprovement(Node newParent) {
			if (newParent.mDistanceTo + 1 < mDistanceTo) {
				mDistanceTo = newParent.mDistanceTo + 1;
			}
		}
	}

	public static int findNonOccludingTaxicabDistance(Block start, Block end, int maxTaxiCabDistance) {
		Map<Block, Node> evaluatedNodes = new HashMap<>();
		evaluatedNodes.put(start, new Node(start, null, end));
		List<Block> visitedBlocks = new ArrayList<>();

		// In theory there can be many more, but it's unlikely we can't find a path in n^2 iterations if one exists
		for (int i = 0; i < maxTaxiCabDistance * maxTaxiCabDistance; i++) {
			if (evaluatedNodes.isEmpty()) {
				return -1;
			}
			Node current = evaluatedNodes.values().stream().min(Comparator.comparingInt(Node::getHeuristic)).get();
			if (current.mBlock.equals(end)) {
				return current.getHeuristic();
			}
			if (current.getHeuristic() > maxTaxiCabDistance) {
				// The best path is not short enough in the best case scenario
				return -1;
			}
			evaluatedNodes.remove(current.mBlock);
			visitedBlocks.add(current.mBlock);

			for (BlockFace bf : List.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN)) {
				Block check = current.mBlock.getRelative(bf);
				if (check.equals(end)) {
					// The heuristic at the end will always be equal to the penultimate block
					return current.getHeuristic();
				}
				if (isLosBlockingBlock(check) || visitedBlocks.contains(check)) {
					continue;
				}

				Node node = evaluatedNodes.get(check);
				if (node == null) {
					evaluatedNodes.put(check, new Node(check, current, end));
				} else {
					node.updateParentIfImprovement(current);
				}
			}
		}

		return -1;
	}

	public static List<Block> getBlocksInSphere(Location loc, double radius) {
		World world = loc.getWorld();
		double bx = loc.getX();
		double by = loc.getY();
		double bz = loc.getZ();
		List<Block> blocks = new ArrayList<>();
		for (double x = bx - radius; x <= bx + radius; x++) {
			for (double y = by - radius; y <= by + radius; y++) {
				for (double z = bz - radius; z <= bz + radius; z++) {
					Location check = new Location(world, x, y, z);
					if (check.distance(loc) <= radius) {
						blocks.add(check.getBlock());
					}
				}
			}
		}
		return blocks;
	}

	public static List<Block> getBlocksInCylinder(Location loc, double radius, double height) {
		World world = loc.getWorld();
		double bx = loc.getX();
		double by = loc.getY();
		double bz = loc.getZ();
		List<Block> blocks = new ArrayList<>();
		for (double x = bx - radius; x <= bx + radius; x++) {
			for (double z = bz - height; z <= bz + radius; z++) {
				Vector check = new Vector(x, by, z);
				if (check.distance(loc.toVector()) <= radius) {
					for (double y = by - height / 2; y <= by + height / 2; y++) {
						blocks.add(new Location(world, x, y, z).getBlock());
					}
				}
			}
		}
		return blocks;
	}

	/**
	 * Gets blocks in a cube around a central block
	 *
	 * @param loc    Center of cube
	 * @param radius Distance around center to check (it's not really a radius because cube but whatever)
	 * @return List of blocks around center
	 */
	public static List<Block> getBlocksInCube(Location loc, double radius) {
		World world = loc.getWorld();
		double bx = loc.getX();
		double by = loc.getY();
		double bz = loc.getZ();
		List<Block> blocks = new ArrayList<>();
		for (double x = bx - radius; x <= bx + radius; x++) {
			for (double y = by - radius; y <= by + radius; y++) {
				for (double z = bz - radius; z <= bz + radius; z++) {
					blocks.add(new Location(world, x, y, z).getBlock());
				}
			}
		}

		return blocks;
	}
}
