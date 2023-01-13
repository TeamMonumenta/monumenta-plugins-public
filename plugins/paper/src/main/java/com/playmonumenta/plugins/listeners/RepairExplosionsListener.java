package com.playmonumenta.plugins.listeners;

import com.bergerkiller.bukkit.common.wrappers.LongHashMap;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Snowable;
import org.bukkit.block.data.type.Snow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;


/*
 * if ServerProperties.getRepairExplosions() == true, this listener will:
 *
 * Catch block & entity explosions, and make note of all simple blocks that were removed
 * When the relevant chunks unload and those blocks are still air, the blocks are restored
 * If a player dies, pending restore blocks near that player will be removed and not restored on chunk unload
 */
public class RepairExplosionsListener implements Listener {

	/**
	 * Set of materials not to repair (to prevent duplication).
	 * Includes all materials from {@link BlockUtils#VALUABLES}.
	 */
	private static final Set<Material> NO_REPAIR_MATS = EnumSet.of(
		// Non-valuable ores
		Material.REDSTONE_ORE,
		Material.DEEPSLATE_REDSTONE_ORE,
		Material.NETHER_QUARTZ_ORE,
		Material.ANCIENT_DEBRIS,

		// Non-valuable blocks
		Material.REDSTONE_BLOCK,

		// [Useful]
		Material.BONE_BLOCK,
		Material.DRAGON_EGG,
		Material.DRIED_KELP_BLOCK,
		Material.HAY_BLOCK,
		Material.SLIME_BLOCK,

		// Special
		Material.SPAWNER,

		// Improbable
		Material.CRYING_OBSIDIAN,
		Material.ENCHANTING_TABLE,
		Material.ENDER_CHEST,
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

	static {
		NO_REPAIR_MATS.addAll(BlockUtils.VALUABLES);
	}

	/* A hash table that stores the list of blocks to restore for each chunk */
	private final Map<UUID, LongHashMap<List<BlockState>>> mBlocksToRepair = new HashMap<>();

	private final Plugin mPlugin;

	public RepairExplosionsListener(Plugin plugin) {
		mPlugin = plugin;
	}

	private void commonExplosionHandler(List<Block> blocks) {
		commonExplosionHandlerStates(blocks.stream().map(Block::getState).toList());
	}

	/*
	 * For each explosion, create a list for each chunk affected by the explosion, and add
	 * all simple blocks removed to that list. Store that list in mBlocksToRepair to be consumed
	 * on chunk unload
	 */
	private void commonExplosionHandlerStates(List<BlockState> blocks) {
		/* Create a copy of the blocks list, so we don't modify the original */
		blocks = new ArrayList<>(blocks);

		/* Remove all complex blocks */
		blocks.removeIf((block) -> NO_REPAIR_MATS.contains(block.getType()) || block instanceof TileState);

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
		LongHashMap<List<BlockState>> worldBlocks = mBlocksToRepair.computeIfAbsent(blocks.get(0).getWorld().getUID(), key -> new LongHashMap<>());
		List<BlockState> chunkBlocks = worldBlocks.get(currentChunkKey);
		if (chunkBlocks == null) {
			chunkBlocks = new ArrayList<>(blocks.size());
			worldBlocks.put(currentChunkKey, chunkBlocks);
		}

		for (BlockState state : blocks) {
			Chunk chunk = state.getChunk();

			/* Check that our tracking variables are still valid for this chunk */
			if (chunk.getChunkKey() != currentChunkKey) {
				/* We've moved into a new chunk in the list, update variables */
				currentChunk = chunk;
				currentChunkKey = currentChunk.getChunkKey();
				chunkBlocks = worldBlocks.get(currentChunkKey);
				if (chunkBlocks == null) {
					chunkBlocks = new ArrayList<>(blocks.size());
					worldBlocks.put(currentChunkKey, chunkBlocks);
				}
			}

			// already marked this block for repair, keep the older state
			if (chunkBlocks.stream().anyMatch(b -> b.getBlock().equals(state.getBlock()))) {
				continue;
			}

			mPlugin.getLogger().fine("Marking block " + state.getType() + " at " + state.getLocation() + " for repair");
			chunkBlocks.add(state);
		}
	}

	private void forgetBlock(Block block) {
		List<BlockState> chunkBlockList = getChunkBlockList(block.getChunk());
		if (chunkBlockList != null) {
			for (Iterator<BlockState> iterator = chunkBlockList.iterator(); iterator.hasNext(); ) {
				BlockState state = iterator.next();
				if (state.getBlock().equals(block)) {
					iterator.remove();
					return;
				}
			}
		}
	}

	private @Nullable List<BlockState> getChunkBlockList(Chunk chunk) {
		long chunkKey = chunk.getChunkKey();
		LongHashMap<List<BlockState>> worldBlocks = mBlocksToRepair.get(chunk.getWorld().getUID());
		if (worldBlocks == null) {
			return null;
		}
		return worldBlocks.get(chunkKey);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityExplodeEvent(EntityExplodeEvent event) {
		if (!isEnabled(event.getLocation().getWorld())) {
			return;
		}

		commonExplosionHandler(event.blockList());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockExplodeEvent(BlockExplodeEvent event) {
		if (!isEnabled(event.getBlock().getWorld())) {
			return;
		}

		commonExplosionHandler(event.blockList());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void structureGrowEvent(StructureGrowEvent event) {
		if (!isEnabled(event.getWorld())) {
			return;
		}

		commonExplosionHandler(event.getBlocks().stream().map(BlockState::getBlock).toList());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockPlaceEvent(BlockPlaceEvent event) {
		if (!isEnabled(event.getBlock().getWorld()) || !ZoneUtils.hasZoneProperty(event.getBlock().getLocation(), ZoneUtils.ZoneProperty.OVERWORLD_BLOCK_RESET)) {
			return;
		}

		// creative mode players bypass the repair, and also clear any pending repair at that block
		if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
			forgetBlock(event.getBlock());
			return;
		}

		commonExplosionHandlerStates(List.of(event.getBlockReplacedState()));
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		if (!isEnabled(event.getBlock().getWorld()) || !ZoneUtils.hasZoneProperty(event.getBlock().getLocation(), ZoneUtils.ZoneProperty.OVERWORLD_BLOCK_RESET)) {
			return;
		}

		// creative mode players bypass the repair, and also clear any pending repair at that block
		if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
			forgetBlock(event.getBlock());
			return;
		}

		commonExplosionHandler(List.of(event.getBlock()));
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void chunkUnloadEvent(ChunkUnloadEvent event) {
		if (!isEnabled(event.getWorld())) {
			return;
		}

		/*
		 * When a chunk is about to unload, check if there were blocks that exploded in it
		 * If so, restore each of those blocks if they are still currently air
		 */
		LongHashMap<List<BlockState>> worldBlocks = mBlocksToRepair.get(event.getChunk().getWorld().getUID());
		if (worldBlocks == null) {
			return;
		}
		List<BlockState> chunkBlocks = worldBlocks.remove(event.getChunk().getChunkKey());
		if (chunkBlocks == null) {
			return;
		}
		if (worldBlocks.size() == 0) {
			mBlocksToRepair.remove(event.getChunk().getWorld().getUID());
		}
		boolean needsSave = false;
		for (BlockState state : chunkBlocks) {
			if (BlockUtils.isValuableBlock(state.getBlock().getType())) {
				// Don't repair if a valuable block has been placed in its place (e.g. a chest or shulker box)
				continue;
			}
			mPlugin.getLogger().fine("Repairing block " + state.getType() + " at " + state.getLocation());
			needsSave = true;
			state.update(true, false);
			if (state.getType().equals(Material.GRASS_BLOCK) && state.getBlockData() instanceof Snowable && ((Snowable) state.getBlockData()).isSnowy()) {
				Block blockAbove = state.getLocation().add(0, 1, 0).getBlock();
				if (blockAbove.getType().isAir()) {
					blockAbove.setType(Material.SNOW);
					Snow snow = (Snow) Material.SNOW.createBlockData();
					snow.setLayers(snow.getMinimumLayers());
					blockAbove.setBlockData(snow);
				}
			} else if (state.getType().equals(Material.SNOW)) {
				Block blockBelow = state.getLocation().subtract(0, 1, 0).getBlock();
				if (!blockBelow.getType().equals(Material.GRASS_BLOCK)) {
					BlockData blockBelowData = blockBelow.getBlockData();
					if (blockBelowData instanceof Snowable) {
						((Snowable) blockBelowData).setSnowy(true);
						blockBelow.setBlockData(blockBelowData);
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
		LongHashMap<List<BlockState>> worldBlocks = mBlocksToRepair.get(chunk.getWorld().getUID());
		if (worldBlocks == null) {
			return;
		}
		List<BlockState> chunkBlocks = worldBlocks.get(chunk.getChunkKey());
		if (chunkBlocks == null) {
			return;
		}
		Iterator<BlockState> iter = chunkBlocks.iterator();
		while (iter.hasNext()) {
			BlockState state = iter.next();
			if (state.getLocation().distanceSquared(deathLoc) <= 144) {
				mPlugin.getLogger().fine("Removing repair block " + state.getType() + " at " + state.getLocation() + " due to death");
				iter.remove();
			}
		}
		if (chunkBlocks.isEmpty()) {
			worldBlocks.remove(chunk.getChunkKey());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerDeathEvent(PlayerDeathEvent event) {
		if (!isEnabled(event.getPlayer().getWorld())) {
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
			mPlugin.getLogger().fine("Player died at: " + deathLoc);

			removeRepairBlocksNear(deathLoc.clone().add(7, 0, -7).getChunk(), deathLoc);
			removeRepairBlocksNear(deathLoc.clone().add(7, 0, 7).getChunk(), deathLoc);
			removeRepairBlocksNear(deathLoc.clone().add(-7, 0, 7).getChunk(), deathLoc);
			removeRepairBlocksNear(deathLoc.clone().add(-7, 0, -7).getChunk(), deathLoc);
		});
	}

	private static boolean isEnabled(World world) {
		Pattern repairExplosionsWorldPattern = ServerProperties.getRepairExplosionsWorldPattern();
		return ServerProperties.getRepairExplosions()
			       && (repairExplosionsWorldPattern == null || repairExplosionsWorldPattern.matcher(world.getName()).matches());
	}

}
