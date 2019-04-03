package com.playmonumenta.plugins.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Rail;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Entity;
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

	public static boolean isValidBoatLocation(Location loc) {
		Block block = loc.getBlock();
		if (block.isLiquid() || containsWater(block)) {
			return true;
		}

		block = loc.subtract(0, 1, 0).getBlock();
		if (block.isLiquid() || containsWater(block)) {
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
			if (block.isLiquid() || containsWater(block)) {
				return true;
			} else if (!block.isEmpty()) {
				return false;
			}
		}

		return false;
	}
}
