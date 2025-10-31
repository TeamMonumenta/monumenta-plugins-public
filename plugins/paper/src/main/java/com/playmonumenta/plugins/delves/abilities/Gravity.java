package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.delves.DelvesModifier;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BlockUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.jetbrains.annotations.Nullable;


public class Gravity {

	public static final String DESCRIPTION = "Player placed blocks are affected by gravity";
	public static final String PLAYER_PLACED_METADATA_KEY = "monumenta:gravity";
	private static final Set<BlockFace> PLANAR_CARTESIAN_BLOCK_FACES = Set.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST);

	public static Component[] rankDescription(int level) {
		return new Component[]{
			Component.text("Blocks players place down fall if they are not supported in a + pattern below,"),
			Component.text("fall damage increases by 150%, and recoil velocity is decreased by 34%")
		};
	}

	public static void applyModifiers(Block block, @Nullable Block blockUnder, int level) {
		if (level == 0) {
			return;
		}
		if (blockUnder != null && !blockUnder.isPassable() && !block.isPassable() && (block.getType() == Material.NETHERITE_BLOCK || (!BlockUtils.VALUABLES.contains(block.getType()) && !BlockUtils.CONTAINERS.contains(block.getType())))) {
			block.setMetadata(PLAYER_PLACED_METADATA_KEY, new FixedMetadataValue(Plugin.getInstance(), true));
			checkForSupport(block.getLocation(), block.getBlockData(), level);
			return;
		}
		if (!block.isPassable() && (block.getType() == Material.NETHERITE_BLOCK || (!BlockUtils.VALUABLES.contains(block.getType()) && !BlockUtils.CONTAINERS.contains(block.getType())))) {
			BlockData blockData = block.getBlockData();
			block.setType(Material.AIR);
			FallingBlock fallingBlock = block.getWorld().spawn(block.getLocation().add(0.5, 0, 0.5), FallingBlock.class, (fallBlock) -> fallBlock.setBlockData(blockData));
			fallingBlock.setMetadata(PLAYER_PLACED_METADATA_KEY, new FixedMetadataValue(Plugin.getInstance(), true));
			fallingBlock.setDropItem(false);
			block.getWorld().playSound(block.getLocation(), "block.gravel.break", 1.0f, 1.0f);
			BlockBreakEvent event = new BlockBreakEvent(block, null);
			Bukkit.getPluginManager().callEvent(event);
		}
	}

	public static void gravityBlockBreakHandler(Block block, int level) {
		if (level == 0) {
			return;
		}
		List<Block> blocksToUpdate = new ArrayList<>();
		Location loc = block.getLocation().add(0, 1, 0);
		blocksToUpdate.add(loc.getBlock());
		PLANAR_CARTESIAN_BLOCK_FACES.forEach(face -> {
			blocksToUpdate.add(loc.clone().add(face.getDirection()).getBlock());
		});
		for (Block blockAbove : blocksToUpdate) {
			if (blockAbove.getType().isSolid() && blockAbove.hasMetadata(PLAYER_PLACED_METADATA_KEY)) {
				applyModifiers(blockAbove, null, DelvesUtils.getModifierLevel(blockAbove.getLocation(), DelvesModifier.GRAVITY));
				return;
			}
		}
	}

	public static void handleExplosion(List<Block> blocks, int level) {
		if (level == 0) {
			return;
		}
		Set<Block> blocksToUpdate = new HashSet<>();
		for (Block block : blocks) {
			Location loc = block.getLocation().add(0, 1, 0);
			blocksToUpdate.add(loc.getBlock());
			PLANAR_CARTESIAN_BLOCK_FACES.forEach(face -> {
				blocksToUpdate.add(loc.clone().add(face.getDirection()).getBlock());
			});
		}
		blocksToUpdate.removeAll(blocks);
		for (Block blockAbove : blocksToUpdate) {
			if (blockAbove.getType().isSolid() && blockAbove.hasMetadata(PLAYER_PLACED_METADATA_KEY)) {
				applyModifiers(blockAbove, null, level);
			}
		}
	}


	public static boolean checkForSupport(Location loc, BlockData blockData, int level) {
		if (level != 0) {
			ArrayList<Block> supportList = new ArrayList<>();
			Location downOne = loc.clone().add(0, -1, 0);
			supportList.add(downOne.getBlock());
			PLANAR_CARTESIAN_BLOCK_FACES.forEach(face -> {
				supportList.add(downOne.clone().add(face.getDirection()).getBlock());
			});
			for (Block support : supportList) {
				if (support.isEmpty() || support.isLiquid()) {
					FallingBlock fallingBlock = loc.getWorld().spawn(support.getLocation().add(0.5, 0, 0.5), FallingBlock.class, (fallBlock) -> fallBlock.setBlockData(blockData));
					fallingBlock.setMetadata(PLAYER_PLACED_METADATA_KEY, new FixedMetadataValue(Plugin.getInstance(), true));
					fallingBlock.setDropItem(false);
					loc.getBlock().setType(Material.AIR);
					loc.getWorld().playSound(loc, "block.gravel.break", 1.0f, 1.0f);
					return true;
				}
			}
			loc.getBlock().setMetadata(PLAYER_PLACED_METADATA_KEY, new FixedMetadataValue(Plugin.getInstance(), true));
		}
		return false;
	}

	public static void applyDamageModifiers(DamageEvent event, int level) {
		if (level == 0) {
			return;
		}
		if (event.getType() == DamageType.FALL) {
			event.setFlatDamage(event.getFlatDamage() * 2.5);
		}
	}
}
