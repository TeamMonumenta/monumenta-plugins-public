package com.playmonumenta.plugins.listeners;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.bergerkiller.bukkit.common.wrappers.LongHashMap;
import com.playmonumenta.plugins.server.properties.ServerProperties;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Snowable;
import org.bukkit.block.data.type.Snow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;



/*
 * if ServerProperties.getRepairExplosions() == true, this listener will:
 *
 * Catch block & entity explosions, and make note of all simple blocks that were removed
 * When the relevant chunks unload and those blocks are still air, the blocks are restored
 * If a player dies, pending restore blocks near that player will be removed and not restored on chunk unload
 */
public class RepairExplosionsListener implements Listener {
	private static final Set<Material> NO_REPAIR_MATS = EnumSet.of(
		// [Gems]

		// Ores
		Material.COAL_ORE,
		Material.IRON_ORE,
		Material.GOLD_ORE,
		Material.GILDED_BLACKSTONE,
		Material.NETHER_GOLD_ORE,
		Material.REDSTONE_ORE,
		Material.LAPIS_ORE,
		Material.NETHER_QUARTZ_ORE,
		Material.DIAMOND_ORE,
		Material.EMERALD_ORE,
		Material.ANCIENT_DEBRIS,

		// Blocks
		Material.COAL_BLOCK,
		Material.IRON_BLOCK,
		Material.GOLD_BLOCK,
		Material.REDSTONE_BLOCK,
		Material.LAPIS_BLOCK,
		Material.DIAMOND_BLOCK,
		Material.EMERALD_BLOCK,
		Material.NETHERITE_BLOCK,

		// [Useful]
		Material.BONE_BLOCK,
		Material.DRAGON_EGG,
		Material.DRIED_KELP_BLOCK,
		Material.HAY_BLOCK,
		Material.SLIME_BLOCK,

		// Special
		Material.ANVIL,
		Material.CHIPPED_ANVIL,
		Material.DAMAGED_ANVIL,

		Material.SPAWNER,

		// Improbable
		Material.CRYING_OBSIDIAN,
		Material.ENCHANTING_TABLE,
		Material.ENDER_CHEST,
		Material.OBSIDIAN,
		Material.RESPAWN_ANCHOR,
		Material.TNT,

		// Weird
		Material.LADDER,
		Material.SCAFFOLDING,
		Material.TWISTING_VINES,
		Material.TWISTING_VINES_PLANT,
		Material.VINE,
		Material.WEEPING_VINES,
		Material.WEEPING_VINES_PLANT,

		Material.CONDUIT,

		// Heads
		Material.PLAYER_HEAD,
		Material.CREEPER_HEAD,
		Material.DRAGON_HEAD,
		Material.SKELETON_SKULL,
		Material.WITHER_SKELETON_SKULL,
		Material.ZOMBIE_HEAD,

		Material.PLAYER_WALL_HEAD,
		Material.CREEPER_WALL_HEAD,
		Material.DRAGON_WALL_HEAD,
		Material.SKELETON_WALL_SKULL,
		Material.WITHER_SKELETON_WALL_SKULL,
		Material.ZOMBIE_WALL_HEAD,

		// [#banned List]
		Material.BEACON,
		Material.HOPPER
	);

	/* A hash table that stores the list of blocks to restore for each chunk */
	private final LongHashMap<List<BlockState>> mBlocksToRepair = new LongHashMap<>();

	private final Plugin mPlugin;

	public RepairExplosionsListener(Plugin plugin) {
		mPlugin = plugin;
	}

	/*
	 * For each explosion, create a list for each chunk affected by the explosion, and add
	 * all simple blocks removed to that list. Store that list in mBlocksToRepair to be consumed
	 * on chunk unload
	 */
	private void commonExplosionHandler(List<Block> blocks) {
		/* Create a copy of the blocks list so we don't modify the original */
		blocks = new ArrayList<Block>(blocks);

		/* Remove all complex blocks */
		blocks.removeIf((block) -> NO_REPAIR_MATS.contains(block.getType()) || block.getState() instanceof TileState);

		if (blocks.isEmpty()) {
			return;
		}

		/*
		 * Sort the blocks by what chunk they are in. This way, the chunk won't change
		 * as much while iterating, leading to fewer hashmap lookups
		 */
		blocks.sort((b1, b2) -> {
			if (b1.getChunk().getChunkKey() == b2.getChunk().getChunkKey()) {
				return Double.compare(b1.getLocation().getY(), b2.getLocation().getY());
			}
			return Long.compare(b1.getChunk().getChunkKey(), b2.getChunk().getChunkKey());
		});

		/* Initialize the tracking variables for the first block */
		Chunk currentChunk = blocks.get(0).getChunk();
		long currentChunkKey = currentChunk.getChunkKey();
		List<BlockState> chunkBlocks = mBlocksToRepair.get(currentChunkKey);
		if (chunkBlocks == null) {
			chunkBlocks = new ArrayList<BlockState>(blocks.size());
			mBlocksToRepair.put(currentChunkKey, chunkBlocks);
		}

		for (Block block : blocks) {
			BlockState state = block.getState();
			Chunk chunk = block.getChunk();

			/* Check that our tracking variables are still valid for this chunk */
			if (chunk.getChunkKey() != currentChunkKey) {
				/* We've moved into a new chunk in the list, update variables */
				currentChunk = chunk;
				currentChunkKey = currentChunk.getChunkKey();
				chunkBlocks = mBlocksToRepair.get(currentChunkKey);
				if (chunkBlocks == null) {
					chunkBlocks = new ArrayList<BlockState>(blocks.size());
					mBlocksToRepair.put(currentChunkKey, chunkBlocks);
				}
			}

			mPlugin.getLogger().fine("Marking block " + state.getType().toString() + " at " + state.getLocation().toString() + " for repair");
			chunkBlocks.add(state);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void entityExplodeEvent(EntityExplodeEvent event) {
		if (event.isCancelled() || !ServerProperties.getRepairExplosions()) {
			return;
		}

		commonExplosionHandler(event.blockList());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void blockExplodeEvent(BlockExplodeEvent event) {
		if (event.isCancelled() || !ServerProperties.getRepairExplosions()) {
			return;
		}

		commonExplosionHandler(event.blockList());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void chunkUnloadEvent(ChunkUnloadEvent event) {
		if (!ServerProperties.getRepairExplosions()) {
			return;
		}

		/*
		 * When a chunk is about to unload, check if there were blocks that exploded in it
		 * If so, restore each of those blocks if they are still currently air
		 */
		boolean needsSave = false;
		List<BlockState> chunkBlocks = mBlocksToRepair.remove(event.getChunk().getChunkKey());
		if (chunkBlocks != null) {
			for (BlockState state : chunkBlocks) {
				if (state.getLocation().getBlock().getType().isAir()) {
					mPlugin.getLogger().fine("Repairing block " + state.getType().toString() + " at " + state.getLocation().toString());
					needsSave = true;
					state.update(true, false);

					if (state.getType().equals(Material.GRASS_BLOCK) && state.getBlockData() instanceof Snowable && ((Snowable)state.getBlockData()).isSnowy()) {
						Block blockAbove = state.getLocation().add(0, 1, 0).getBlock();
						if (blockAbove.getType().isAir()) {
							blockAbove.setType(Material.SNOW);
							Snow snow = (Snow)Material.SNOW.createBlockData();
							snow.setLayers(snow.getMinimumLayers());
							blockAbove.setBlockData(snow);
						}
					} else if (state.getType().equals(Material.SNOW)) {
						Block blockBelow = state.getLocation().subtract(0, 1, 0).getBlock();
						if (!blockBelow.getType().equals(Material.GRASS_BLOCK)) {
							BlockData blockBelowData = blockBelow.getBlockData();
							if (blockBelowData instanceof Snowable) {
								((Snowable)blockBelowData).setSnowy(true);
								blockBelow.setBlockData(blockBelowData);
							}
						}
					}
				}
			}
		}
		if (needsSave) {
			event.setSaveChunk(true);
		}
	}

	/*
	 * Remove any repair pending blocks from the specified chunk that are near the
	 * deathLoc
	 */
	private void removeRepairBlocksNear(Chunk chunk, Location deathLoc) {
		List<BlockState> chunkBlocks = mBlocksToRepair.get(chunk.getChunkKey());
		if (chunkBlocks != null) {
			Iterator<BlockState> iter = chunkBlocks.iterator();
			while (iter.hasNext()) {
				BlockState state = iter.next();
				if (state.getLocation().distanceSquared(deathLoc) <= 144) {
					mPlugin.getLogger().fine("Removing repair block " + state.getType().toString() + " at " + state.getLocation().toString() + " due to death");
					iter.remove();
				}
			}
			if (chunkBlocks.isEmpty()) {
				mBlocksToRepair.remove(chunk.getChunkKey());
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void playerDeathEvent(PlayerDeathEvent event) {
		if (event.isCancelled() || !ServerProperties.getRepairExplosions()) {
			return;
		}

		/* Run this soon but not right now - need to process any explosions after the player died */
		Bukkit.getScheduler().runTask(mPlugin, () -> {
			/*
			 * When a player dies, remove all the explosion-restore blocks near them.
			 * This will require potentially grabbing up to four chunks
			 * Be lazy and just grab four chunks, re-iterating them isn't going to take that much time anyway
			 */
			Location deathLoc = event.getEntity().getLocation();
			mPlugin.getLogger().fine("Player died at: " + deathLoc.toString());

			removeRepairBlocksNear(deathLoc.clone().add(7, 0, -7).getChunk(), deathLoc);
			removeRepairBlocksNear(deathLoc.clone().add(7, 0, 7).getChunk(), deathLoc);
			removeRepairBlocksNear(deathLoc.clone().add(-7, 0, 7).getChunk(), deathLoc);
			removeRepairBlocksNear(deathLoc.clone().add(-7, 0, -7).getChunk(), deathLoc);
		});
	}
}
