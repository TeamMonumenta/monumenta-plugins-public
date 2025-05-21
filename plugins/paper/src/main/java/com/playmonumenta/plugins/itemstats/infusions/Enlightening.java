package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.utils.AdvancementUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class Enlightening implements Infusion {
	private static final List<BlockFace> ORDERED_CARTESIAN_BLOCK_FACES = List.of(
			BlockFace.DOWN,
			BlockFace.NORTH,
			BlockFace.EAST,
			BlockFace.SOUTH,
			BlockFace.WEST
	);

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.ENLIGHTENING;
	}

	@Override
	public String getName() {
		return "Enlightening";
	}

	@Override
	public void onBlockBreak(Plugin plugin, Player player, double value, BlockBreakEvent event) {
		if (player.isSneaking()) {
			// Assuming sneaking players are trying not to activate this, like with Excavator
			return;
		}

		if (!AdvancementUtils.checkAdvancement(player, "monumenta:quests/r2/primevalcreations013")) {
			// The player has not completed Primeval Creations; act like a normal shovel
			return;
		}

		Block block = event.getBlock();
		Location brokenLoc = block.getLocation();
		Material mat = block.getType();

		Location upperLoc = brokenLoc.clone().add(0.0, 1.0, 0.0);
		Block upperBlock = upperLoc.getBlock();
		Material upperMat = upperBlock.getType();

		if (!upperMat.hasGravity()) {
			// Nothing to do
			return;
		}

		// The block above is going to fall, prepare to place a torch

		if (Material.TORCH.equals(mat)) {
			// Avoid rapidly spawning infinite torches; we're replacing this block soon
			event.setDropItems(false);
		}

		// The block usually drops 3 ticks after the block below is removed; I'm not hard-coding that in case it changes
		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (!brokenLoc.isChunkLoaded()) {
					// Chunk unloaded? Not sure how that can happen so fast.
					cancel();
					return;
				}

				Block currentBrokenBlock = brokenLoc.getBlock();
				if (!Material.AIR.equals(currentBrokenBlock.getType())) {
					// Block was replaced, abort! Don't interfere!
					cancel();
					return;
				}

				Block currentUpperBlock = upperLoc.getBlock();
				Material currentUpperMat = currentUpperBlock.getType();

				if (!upperMat.equals(currentUpperMat)) {
					// The block probably fell, or has otherwise been interfered with; either way, we're done here

					// Place a torch on a face that can support it if possible
					for (BlockFace blockFace : ORDERED_CARTESIAN_BLOCK_FACES) {
						Location testLoc = brokenLoc.clone().add(blockFace.getDirection());
						Block testBlock = testLoc.getBlock();
						if (testBlock.isSolid()) {
							if (BlockFace.DOWN.equals(blockFace)) {
								currentBrokenBlock.setType(Material.TORCH);
							} else {
								currentBrokenBlock.setType(Material.WALL_TORCH);
								BlockData blockData = currentBrokenBlock.getBlockData();
								if (blockData instanceof Directional directional) {
									directional.setFacing(blockFace.getOppositeFace());
								}
								currentBrokenBlock.setBlockData(blockData);
							}
							break;
						}
					}

					cancel();
				}
			}
		};
		runnable.runTaskTimer(plugin, 1L, 1L);
	}
}
