package com.playmonumenta.plugins.utils;

import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.Waterlogged;

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
}
