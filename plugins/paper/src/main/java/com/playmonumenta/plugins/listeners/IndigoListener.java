package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class IndigoListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void blockPlaceEventMonitor(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlock();
		Material mat = block.getType();

		if (event.isCancelled()) {
			// Exceptions for cancelled events
			if (ItemUtils.getPlainName(event.getItemInHand()).contains("Tesseract of Balance")) {
				revokeBlocklessEligibility(player, "Pink Tesseract used");
			}
		} else {
			// Some actions, such as tilling dirt is technically "placing" a block.
			// We're only interested in cases where a player replaces a non-collidable block with a collidable one.
			if (event.getBlockReplacedState().getType().isSolid() || !mat.isCollidable()) {
				return;
			}
			revokeBlocklessEligibility(player, String.format("Block placed: %s; location: %d, %d, %d", mat, block.getX(), block.getY(), block.getZ()));
		}
	}

	public static void revokeBlocklessEligibility(Player player, String message) {
		if (player.getScoreboardTags().contains("IndigoBlockless")) {
			Plugin.getInstance().getLogger().info("IndigoListener: Player " + player.getName() + " lost eligibility for the blockless advancement:");
			Plugin.getInstance().getLogger().info("IndigoListener: " + message);
		}
		player.getScoreboardTags().remove("IndigoBlockless");
	}
}
