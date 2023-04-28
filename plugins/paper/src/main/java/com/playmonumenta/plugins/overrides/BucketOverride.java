package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fish;
import org.bukkit.entity.Goat;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.Nullable;

public class BucketOverride extends BaseOverride {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, @Nullable Block block) {
		if (player == null || player.getGameMode() == GameMode.CREATIVE) {
			return true;
		} else if (player.getGameMode() == GameMode.SURVIVAL && ZoneUtils.isInPlot(player)) {
			return true;
		}

		// Cancelled bucket fill events leave behind a ghost air block, so force a block update to fill it with water/lava again on the client
		if (item.getType() == Material.BUCKET) {
			// The event block is not the actual fluid block picked up - it is the furthest block, while the picked up block is the closest one
			RayTraceResult realBlockTrace = player.getWorld().rayTraceBlocks(player.getEyeLocation(), NmsUtils.getVersionAdapter().getActualDirection(player), 10, FluidCollisionMode.SOURCE_ONLY, false);
			Block realBlock = realBlockTrace != null ? realBlockTrace.getHitBlock() : null;
			if (realBlock != null) {
				Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
					// Also send blocks nearby because the client doesn't send the proper direction that is uses to remove the block, but one delayed by a tick, so it can update the wrong block
					for (BlockFace face : new BlockFace[] {BlockFace.SELF, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN}) {
						Block b = realBlock.getRelative(face);
						player.sendBlockChange(b.getLocation(), b.getBlockData());
					}
				});
			}
		}
		return false;
	}

	@Override
	public boolean rightClickEntityInteraction(Plugin plugin, Player player, Entity clickedEntity,
	                                           ItemStack itemInHand) {
		if (clickedEntity == null) {
			return true;
		} else if (clickedEntity instanceof Cow || clickedEntity instanceof Goat) {
			return false;
		} else if ((clickedEntity instanceof Fish || clickedEntity instanceof Axolotl) && (clickedEntity.isInvulnerable() || !ZoneUtils.isInPlot(clickedEntity))) {
			return false;
		}

		return true;
	}

	@Override
	public boolean blockDispenseInteraction(Plugin plugin, Block block, ItemStack dispensed) {
		Material blockType = (block != null) ? block.getType() : Material.AIR;
		if (blockType.equals(Material.AIR) || dispensed == null) {
			return false;
		} else if (blockType.equals(Material.DISPENSER)) {
			return ZoneUtils.isInPlot(block.getLocation());
		} else if (blockType.equals(Material.DROPPER)) {
			return true;
		}

		return false;
	}
}
