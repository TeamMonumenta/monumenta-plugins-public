package com.playmonumenta.plugins.listeners;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemUtils;

public class WorldListener implements Listener {
	Plugin mPlugin;
	World mWorld;

	public WorldListener(Plugin plugin, World world) {
		mPlugin = plugin;
		mWorld = world;
	}

	//  A Chunk Loaded.
	@EventHandler(priority = EventPriority.LOWEST)
	public void ChunkLoadEvent(ChunkLoadEvent event) {
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

	@EventHandler(priority = EventPriority.LOWEST)
	public void StructureGrowEvent(StructureGrowEvent event) {
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
		Queue<Location> locList = new LinkedList<>();
		// Start at the first block of the structure/tree (the sapling)
		locList.add(event.getLocation());

		// Starting block is reachable - prevent it from being re-checked
		event.getLocation().getBlock().removeMetadata(Constants.TREE_METAKEY, mPlugin);

		// Convenience list of offsets to get adjacent blocks
		List<Vector> adjacentOffsets = Arrays.asList(
		                                   new Vector(1, 0, 0),
		                                   new Vector(-1, 0, 0),
		                                   new Vector(0, 1, 0),
		                                   new Vector(0, 0, 1),
		                                   new Vector(0, 0, -1),

		                                   // Acacia fix
		                                   new Vector(-1, 1, 0),
		                                   new Vector(1, 1, 0),
		                                   new Vector(0, 1, -1),
		                                   new Vector(0, 1, 1)
		                               );

		while (!locList.isEmpty()) {
			Location loc = locList.remove();

			// Add adjacent blocks with metadata to the back of the queue
			for (Vector vec : adjacentOffsets) {
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
}
