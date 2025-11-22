package com.playmonumenta.plugins.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.chunk.ChunkPartialUnloadEvent;
import com.playmonumenta.plugins.utils.BlockUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

/**
 * Handles temporary block changes, usually from mob abilities.
 * These block changes are real, i.e. they actually change the block on the server and not just visually alter them for clients.
 */
public class TemporaryBlockChangeManager implements Listener {

	public static final TemporaryBlockChangeManager INSTANCE = new TemporaryBlockChangeManager();

	private final Map<World, Map<Block, ChangedBlock>> mChangedBlocks = new HashMap<>();

	public static class ChangedBlock {
		private final Material mTemporaryType;
		/**
		 * Old state of the block. Will always be the original state, even if there's more changes to this block ({@link #mPreviousChange}).
		 */
		public final BlockState mOldState;
		private int mExpiration;
		/**
		 * An older change to the same block that outlasts the current change, i.e. {@code mExpiration < mPreviousChange.mExpiration}
		 */
		private @Nullable ChangedBlock mPreviousChange = null;

		public ChangedBlock(Material temporaryType, BlockState oldState, int expiration) {
			this.mTemporaryType = temporaryType;
			this.mOldState = oldState;
			this.mExpiration = expiration;
		}
	}

	private @Nullable BukkitTask mExpirationTask = null;

	/**
	 * Temporarily changes a block. Will not change certain mechanical or valuable blocks.
	 *
	 * @param block         The block to change
	 * @param temporaryType The type to change the block to
	 * @param duration      How long to change the block for (in ticks)
	 * @return Whether the block has been changed (i.e. was a valid block or already of the correct type)
	 */
	public boolean changeBlock(Block block, Material temporaryType, int duration) {
		return changeBlock(block, temporaryType.createBlockData(), duration);
	}

	public boolean changeBlock(Block block, BlockData temporaryData, int duration) {
		return changeBlock(block, temporaryData, duration, false);
	}

	public boolean changeBlock(Block block, Material temporaryType, int duration, boolean forceReplace) {
		return changeBlock(block, temporaryType.createBlockData(), duration, forceReplace);
	}

	public boolean changeBlock(Block block, BlockData temporaryData, int duration, boolean forceReplace) {
		if (duration <= 0) {
			return false;
		}
		Material existingType = block.getType();
		if (temporaryData.getMaterial() == existingType) {
			return false;
		}
		if (!forceReplace && ((BlockUtils.isMechanicalBlock(existingType) && existingType != Material.AIR) || BlockUtils.isValuableBlock(existingType))) {
			return false;
		}

		int expiration = Bukkit.getCurrentTick() + duration;
		Map<Block, ChangedBlock> worldMap = mChangedBlocks.computeIfAbsent(block.getWorld(), key -> new HashMap<>());
		ChangedBlock existingChangedBlock = worldMap.get(block);
		if (existingChangedBlock != null && existingChangedBlock.mTemporaryType == existingType && existingChangedBlock.mExpiration > expiration) {
			ChangedBlock changedBlock = new ChangedBlock(temporaryData.getMaterial(), existingChangedBlock.mOldState, expiration);
			changedBlock.mPreviousChange = existingChangedBlock;
			worldMap.put(block, changedBlock);
		} else {
			worldMap.put(block, new ChangedBlock(temporaryData.getMaterial(), existingChangedBlock == null ? block.getState(true) : existingChangedBlock.mOldState, expiration));
		}
		block.setBlockData(temporaryData, false);
		if (mExpirationTask == null) {
			mExpirationTask = Bukkit.getScheduler().runTaskTimer(Plugin.getInstance(), this::handleExpiration, 1, 1);
		}
		return true;
	}

	/**
	 * Handles expired blocks and changes them back. Since we don't expect there to be more than a few hundred blocks changed at a time,
	 * simply iterating all active blocks every tick is sufficiently performant and there's no need to order them by expiration time.
	 */
	private void handleExpiration() {
		int tick = Bukkit.getCurrentTick();
		for (Iterator<Map<Block, ChangedBlock>> worldIter = mChangedBlocks.values().iterator(); worldIter.hasNext(); ) {
			Map<Block, ChangedBlock> worldMap = worldIter.next();
			for (Iterator<Map.Entry<Block, ChangedBlock>> blockIter = worldMap.entrySet().iterator(); blockIter.hasNext(); ) {
				Map.Entry<Block, ChangedBlock> entry = blockIter.next();
				ChangedBlock changedBlock = entry.getValue();
				if (tick >= changedBlock.mExpiration) {
					Block block = entry.getKey();
					if (!block.getLocation().isChunkLoaded()) {
						blockIter.remove();
					} else if (block.getType() == changedBlock.mTemporaryType) {
						if (changedBlock.mPreviousChange == null) {
							changedBlock.mOldState.update(true, false);
							blockIter.remove();
						} else {
							block.setType(changedBlock.mPreviousChange.mTemporaryType, false);
							entry.setValue(changedBlock.mPreviousChange);
						}
					} else {
						if (changedBlock.mPreviousChange == null) {
							blockIter.remove();
						} else {
							entry.setValue(changedBlock.mPreviousChange);
						}
					}
				}
			}
			if (worldMap.isEmpty()) {
				worldIter.remove();
			}
		}
		if (mExpirationTask != null && mChangedBlocks.isEmpty()) {
			mExpirationTask.cancel();
			mExpirationTask = null;
		}
	}

	/**
	 * Checks if a given block is currently changed.
	 *
	 * @param block        The block to check
	 * @param expectedType The expected temporary type of the block. If changed but not of this type, will return false.
	 */
	public boolean isChangedBlock(Block block, Material expectedType) {
		return getChangedBlock(block, expectedType) != null;
	}

	/**
	 * Increases the duration of the temporary block. The new expiration will be calculated by adding the given duration with
	 * the current server tick. The old expiration will not have any effect on the new expiration. Will only increase if the
	 * new expiration is later than the old.
	 *
	 * @param block    The block's duration to change
	 * @param duration The duration to add to the current server tick for the new expiration
	 * @return True if the duration was increased, false if not
	 */
	public boolean increaseDuration(Block block, int duration) {
		if (duration <= 0) {
			return false;
		}
		Map<Block, ChangedBlock> worldMap = mChangedBlocks.computeIfAbsent(block.getWorld(), key -> new HashMap<>());
		if (worldMap.containsKey(block)) {
			ChangedBlock changedBlock = worldMap.get(block);
			int oldExpiration = changedBlock.mExpiration;
			int newExpiration = Bukkit.getCurrentTick() + duration;
			if (oldExpiration >= newExpiration) {
				return false;
			}
			changedBlock.mExpiration = newExpiration;
			return true;
		}
		return false;
	}

	/**
	 * Gets the ChangedBlock if active. Null if the block is not changed or not of the expected type.
	 *
	 * @param block        The block to check
	 * @param expectedType The expected temporary type of the block. If changed but not of this type, will return null.
	 */
	public @Nullable ChangedBlock getChangedBlock(Block block, Material expectedType) {
		Map<Block, ChangedBlock> worldMap = mChangedBlocks.get(block.getWorld());
		if (worldMap == null) {
			return null;
		}
		ChangedBlock changedBlock = worldMap.get(block);
		if (changedBlock == null || changedBlock.mTemporaryType != expectedType || !block.getLocation().isChunkLoaded() || block.getType() != expectedType) {
			return null;
		}
		return changedBlock;
	}

	/**
	 * Instantly reverts a changed block.
	 *
	 * @param block        The block to check
	 * @param expectedType The expected temporary type of the block. If changed but not of this type, will not revert and return false.
	 * @return Whether the block has been changed back (can be false if already changed back, or another change happened in the meantime)
	 */
	public boolean revertChangedBlock(Block block, Material expectedType) {
		Map<Block, ChangedBlock> worldMap = mChangedBlocks.get(block.getWorld());
		if (worldMap == null) {
			return false;
		}
		ChangedBlock parent = null;
		ChangedBlock changedBlock = worldMap.get(block);
		while (changedBlock != null) {
			if (changedBlock.mTemporaryType == expectedType) {
				if (parent != null) {
					parent.mPreviousChange = changedBlock.mPreviousChange;
				} else if (changedBlock.mPreviousChange != null) {
					worldMap.put(block, changedBlock.mPreviousChange);
				} else {
					worldMap.remove(block);
				}
				if (block.getLocation().isChunkLoaded() && block.getType() == expectedType) {
					if (changedBlock.mPreviousChange == null) {
						changedBlock.mOldState.update(true, false);
					} else {
						block.setType(changedBlock.mPreviousChange.mTemporaryType, false);
					}
					return true;
				}
				return false;
			}
			parent = changedBlock;
			changedBlock = changedBlock.mPreviousChange;
		}
		return false;
	}

	public void revertChangedBlocks(Collection<Block> blocks, Material expectedType) {
		for (Block block : blocks) {
			revertChangedBlock(block, expectedType);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void chunkPartialUnloadEvent(ChunkPartialUnloadEvent event) {
		Map<Block, ChangedBlock> worldBlocks = mChangedBlocks.get(event.getWorld());
		if (worldBlocks == null) {
			return;
		}
		long chunkKey = event.getChunk().getChunkKey();
		for (Iterator<Map.Entry<Block, ChangedBlock>> iterator = worldBlocks.entrySet().iterator(); iterator.hasNext(); ) {
			Map.Entry<Block, ChangedBlock> entry = iterator.next();
			if (chunkKey == Chunk.getChunkKey(entry.getKey().getLocation())) {
				if (entry.getKey().getType() == entry.getValue().mTemporaryType) {
					entry.getValue().mOldState.update(true, false);
				}
				iterator.remove();
			}
		}
	}

}
