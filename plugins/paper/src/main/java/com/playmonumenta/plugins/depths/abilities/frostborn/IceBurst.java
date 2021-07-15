package com.playmonumenta.plugins.depths.abilities.frostborn;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.InventoryUtils;

import net.md_5.bungee.api.ChatColor;

public class IceBurst extends DepthsAbility {

	public static final String ABILITY_NAME = "Permafrost";
	public static final int[] ICE_TICKS = {8 * 20, 11 * 20, 14 * 20, 17 * 20, 20 * 20};

	public static String tree;

	public IceBurst(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.QUARTZ;
		mTree = DepthsTree.FROSTBORN;
	}

	@Override
	public boolean blockBreakEvent(BlockBreakEvent event) {
		//If we break a spawner with a pickaxe
		if (InventoryUtils.isPickaxeItem(event.getPlayer().getInventory().getItemInMainHand()) && event.getBlock().getType() == Material.SPAWNER) {
			ArrayList<Block> blocksToIce = new ArrayList<>();
			Block block = event.getBlock().getRelative(BlockFace.DOWN);
			if (block.isSolid()) {
				DepthsUtils.spawnIceTerrain(block.getLocation(), ICE_TICKS[mRarity - 1]);
			}
			blocksToIce.add(block.getRelative(BlockFace.NORTH));
			blocksToIce.add(block.getRelative(BlockFace.EAST));
			blocksToIce.add(block.getRelative(BlockFace.SOUTH));
			blocksToIce.add(block.getRelative(BlockFace.WEST));
			blocksToIce.add(block.getRelative(BlockFace.NORTH).getRelative(BlockFace.NORTH));
			blocksToIce.add(block.getRelative(BlockFace.WEST).getRelative(BlockFace.WEST));
			blocksToIce.add(block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.SOUTH));
			blocksToIce.add(block.getRelative(BlockFace.EAST).getRelative(BlockFace.EAST));
			blocksToIce.add(block.getRelative(BlockFace.NORTH).getRelative(BlockFace.WEST));
			blocksToIce.add(block.getRelative(BlockFace.NORTH).getRelative(BlockFace.EAST));
			blocksToIce.add(block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.WEST));
			blocksToIce.add(block.getRelative(BlockFace.SOUTH).getRelative(BlockFace.EAST));

			for (Block b : blocksToIce) {
				//Check above block first and see if it is exposed to air
				if (b.getRelative(BlockFace.UP).isSolid() && !b.getRelative(BlockFace.UP).getRelative(BlockFace.UP).isSolid()) {
					DepthsUtils.spawnIceTerrain(b.getRelative(BlockFace.UP).getLocation(), ICE_TICKS[mRarity - 1]);
				} else if (b.isSolid()) {
					DepthsUtils.spawnIceTerrain(b.getLocation(), ICE_TICKS[mRarity - 1]);
				} else if (b.getRelative(BlockFace.DOWN).isSolid()) {
					DepthsUtils.spawnIceTerrain(b.getRelative(BlockFace.DOWN).getLocation(), ICE_TICKS[mRarity - 1]);
				}
			}
		}
		return true;
	}

	@Override
	public String getDescription(int rarity) {
		return "Breaking a spawner spawns ice around it that lasts for " + DepthsUtils.getRarityColor(rarity) + ICE_TICKS[rarity - 1] / 20 + ChatColor.WHITE + " seconds.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.FROSTBORN;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.SPAWNER;
	}
}

