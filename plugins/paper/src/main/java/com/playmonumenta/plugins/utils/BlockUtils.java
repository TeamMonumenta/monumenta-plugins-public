package com.playmonumenta.plugins.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BlockUtils {
	private static final Set<Material> ALT_WATER_SOURCES = Set.of(
		Material.BUBBLE_COLUMN,
		Material.KELP,
		Material.KELP_PLANT,
		Material.SEAGRASS,
		Material.TALL_SEAGRASS
	);

	private static final EnumSet<Material> MECHANICAL_BLOCKS = EnumSet.of(
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
		Material.LIGHT
	);

	public static final EnumSet<Material> VALUABLES = EnumSet.of(
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

	public static final EnumSet<Material> CONTAINERS = EnumSet.of(
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

	public static final EnumSet<Material> TORCHES = EnumSet.of(
		Material.TORCH,
		Material.WALL_TORCH
	);

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
		if (data instanceof Waterlogged) {
			return ((Waterlogged) data).isWaterlogged();
		}
		return false;
	}

	public static boolean isMechanicalBlock(Material material) {
		return MECHANICAL_BLOCKS.contains(material);
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

}
