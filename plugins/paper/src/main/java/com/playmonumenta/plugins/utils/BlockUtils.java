package com.playmonumenta.plugins.utils;

import java.util.Set;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.Waterlogged;

public class BlockUtils {
	private static Set<Material> ALT_WATER_SOURCES = Set.of(
		Material.BUBBLE_COLUMN,
		Material.KELP,
		Material.KELP_PLANT,
		Material.SEAGRASS,
		Material.TALL_SEAGRASS
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
			return ((Waterlogged)data).isWaterlogged();
		}
		return false;
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
