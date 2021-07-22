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
			if (block.isSolid() || block.getType() == Material.WATER) {
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
				DepthsUtils.iceExposedBlock(b, ICE_TICKS[mRarity - 1]);
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

