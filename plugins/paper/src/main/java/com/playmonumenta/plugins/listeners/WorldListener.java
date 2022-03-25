package com.playmonumenta.plugins.listeners;

import com.destroystokyo.paper.event.block.TNTPrimeEvent;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

public class WorldListener implements Listener {
	Plugin mPlugin;

	public WorldListener(Plugin plugin) {
		mPlugin = plugin;
	}

	//  A Chunk Loaded.
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void chunkLoadEvent(ChunkLoadEvent event) {
		Entity[] entities = event.getChunk().getEntities();

		for (Entity entity : entities) {
			mPlugin.mTrackingManager.addEntity(entity);

			if (entity instanceof Monster) {
				Monster mob = (Monster)entity;

				int timer = mPlugin.mCombatLoggingTimers.getTimer(entity.getUniqueId());
				if (timer >= 0) {
					Set<String> tags = mob.getScoreboardTags();
					if (!tags.contains("Elite") && !tags.contains("Boss")) {
						mob.setRemoveWhenFarAway(false);
					}

					mPlugin.mCombatLoggingTimers.removeTimer(entity.getUniqueId());
				}
			}
		}
	}

	// Convenience list of offsets to get adjacent blocks
	private static final List<Vector> ADJACENT_OFFSETS = Arrays.asList(
	                                                         new Vector(0, 0, 1),
	                                                         new Vector(0, 0, -1),
	                                                         new Vector(0, 1, 0),
	                                                         new Vector(0, 1, 1),
	                                                         new Vector(0, 1, -1),
	                                                         new Vector(0, -1, 0),
	                                                         new Vector(0, -1, 1),
	                                                         new Vector(0, -1, -1),
	                                                         new Vector(1, 0, 0),
	                                                         new Vector(1, 0, 1),
	                                                         new Vector(1, 0, -1),
	                                                         new Vector(1, 1, 0),
	                                                         new Vector(1, 1, 1),
	                                                         new Vector(1, 1, -1),
	                                                         new Vector(1, -1, 0),
	                                                         new Vector(1, -1, 1),
	                                                         new Vector(1, -1, -1),
	                                                         new Vector(-1, 0, 0),
	                                                         new Vector(-1, 0, 1),
	                                                         new Vector(-1, 0, -1),
	                                                         new Vector(-1, 1, 0),
	                                                         new Vector(-1, 1, 1),
	                                                         new Vector(-1, 1, -1),
	                                                         new Vector(-1, -1, 0),
	                                                         new Vector(-1, -1, 1),
	                                                         new Vector(-1, -1, -1)
	                                                     );

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void structureGrowEvent(StructureGrowEvent event) {
		List<BlockState> blockStates = event.getBlocks();

		ListIterator<BlockState> iter = blockStates.listIterator();
		while (iter.hasNext()) {
			BlockState bs = iter.next();

			if (ItemUtils.isAllowedTreeReplace(bs.getBlock().getType())) {
				// Mark every potentially allowed block with metadata
				bs.getBlock().setMetadata(Constants.TREE_METAKEY, new FixedMetadataValue(mPlugin, true));
			} else {
				// Remove blocks that replace other materials than the allowed ones
				iter.remove();
			}
		}

		/*
		 * Create a new FIFO queue which will be used to walk the list
		 * of all elements that grew as a result of this event
		 * (those that have the above metadata)
		 *
		 * Locations that still have the metadata are added to the back of the queue
		 * as they are encountered, and items are pulled off the queue to process them.
		 *
		 * When a block is processed, each neighboring block is checked for the
		 * metadata. If it has it, it is added to the back of the queue.
		 *
		 * Once a block is done processing, it's metadata is removed.
		 *
		 * This performs effectively a breadth-first search which removes the metadata
		 * from all reachable blocks. Any remaining blocks after this operation
		 * completes should be removed as they are not reachable from the structure's
		 * origin
		 */
		Queue<Location> locList = new ArrayDeque<>();
		// Start at the first block of the structure/tree (the sapling)
		locList.add(event.getLocation());

		// Starting block is reachable - prevent it from being re-checked
		event.getLocation().getBlock().removeMetadata(Constants.TREE_METAKEY, mPlugin);

		while (!locList.isEmpty()) {
			Location loc = locList.remove();

			// Add adjacent blocks with metadata to the back of the queue
			for (Vector vec : ADJACENT_OFFSETS) {
				Location tmpLoc = loc.clone().add(vec);
				Block blk = tmpLoc.getBlock();
				if (blk.hasMetadata(Constants.TREE_METAKEY)) {
					locList.add(tmpLoc);

					// This block is reachable - prevent it from being checked again
					blk.removeMetadata(Constants.TREE_METAKEY, mPlugin);
				}
			}
		}

		/*
		 * Iterate over the list of blocks one last time and remove any that
		 * still have the metadata (they are not attached to a path that
		 * reaches the sapling where the tree grew)
		 */
		iter = blockStates.listIterator();
		while (iter.hasNext()) {
			BlockState bs = iter.next();
			Block blk = bs.getBlock();

			if (blk.hasMetadata(Constants.TREE_METAKEY)) {
				blk.removeMetadata(Constants.TREE_METAKEY, mPlugin);
				iter.remove();

				// Debug to visualize the removed blocks
				// blk.setType(Material.REDSTONE_BLOCK);
			}
		}
	}

	// Block Dispense Event
	// Cancel dispensers/droppers dropping specific items
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockDispenseEvent(BlockDispenseEvent event) {
		Block block = event.getBlock();
		ItemStack dispensed = event.getItem();
		if (!mPlugin.mItemOverrides.blockDispenseInteraction(mPlugin, block, dispensed)) {
			event.setCancelled(true);
		}
	}

	// Block Dispense Armor Event
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void blockDispenseArmorEvent(BlockDispenseArmorEvent event) {
		if (event.getTargetEntity() instanceof Player) {
			InventoryUtils.scheduleDelayedEquipmentCheck(mPlugin, (Player) event.getTargetEntity(), event);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockFormEvent(BlockFormEvent event) {
		Material blockType = event.getNewState().getType();
		if (blockType == null) {
			blockType = Material.AIR;
		}

		if (blockType.equals(Material.SNOW) || blockType.equals(Material.ICE)) {
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void tntPrimeEvent(TNTPrimeEvent event) {
		Location loc = event.getBlock().getLocation();
		if (ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.NO_EXPLOSIONS)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockPistonExtendEvent(BlockPistonExtendEvent event) {
		Block piston = event.getBlock();
		BlockFace direction = event.getDirection();
		for (Block block : event.getBlocks()) {
			if (shouldCancelPiston(piston, direction, block)) {
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockPistonRetractEvent(BlockPistonRetractEvent event) {
		Block piston = event.getBlock();
		BlockFace direction = event.getDirection();
		for (Block block : event.getBlocks()) {
			if (shouldCancelPiston(piston, direction, block)) {
				event.setCancelled(true);
				return;
			}
		}
	}

	public boolean shouldCancelPiston(Block piston, BlockFace direction, Block affectedBlock) {
		int pistonTestValue = pistonZonePropertyTest(piston.getLocation());

		Location oldLoc = affectedBlock.getLocation();
		if (pistonTestValue != pistonZonePropertyTest(oldLoc)) {
			return true;
		}

		Location newLoc = new Location(oldLoc.getWorld(),
		                               oldLoc.getX() + direction.getModX(),
		                               oldLoc.getY() + direction.getModY(),
		                               oldLoc.getZ() + direction.getModZ());
		if (pistonTestValue != pistonZonePropertyTest(newLoc)) {
			return true;
		}

		return false;
	}

	private int pistonZonePropertyTest(Location loc) {
		int testValue = 0;
		if (ZoneUtils.hasZoneProperty(loc, ZoneUtils.ZoneProperty.ADVENTURE_MODE)) {
			testValue |= 1;
		}
		if (ZoneUtils.isInPlot(loc)) {
			testValue |= 2;
		}
		return testValue;
	}
}
