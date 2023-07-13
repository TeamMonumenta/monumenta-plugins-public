package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.SpawnerUtils;
import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class TorchListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockPlaceEvent(BlockPlaceEvent event) {
		Block block = event.getBlock();
		if (BlockUtils.isTorch(block)) {
			SpawnerUtils.addTorch(block);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (BlockUtils.isTorch(block)) {
			SpawnerUtils.removeTorch(block);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockExplodeEvent(BlockExplodeEvent event) {
		handleExplodeEvent(event.blockList());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityExplodeEvent(EntityExplodeEvent event) {
		handleExplodeEvent(event.blockList());
	}

	private void handleExplodeEvent(List<Block> blockList) {
		blockList.stream().filter(BlockUtils::isTorch).forEach(SpawnerUtils::removeTorch);
	}
}
