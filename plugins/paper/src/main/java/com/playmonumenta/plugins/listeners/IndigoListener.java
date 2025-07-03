package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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

		if (mat != Material.TORCH) {
			// Tesseract exceptions
			if (mat == Material.PURPLE_STAINED_GLASS
				|| mat == Material.CYAN_STAINED_GLASS
				|| mat == Material.MAGENTA_STAINED_GLASS) {
				Location loc = block.getLocation();
				// Remove eligibility if the block hasn't changed 1 tick later
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					if (loc.getBlock().getType() == mat) {
						revokeBlocklessEligibility(player);
					}
				}, 1);
				return;
			}
			revokeBlocklessEligibility(player);
		}
	}

	public static void revokeBlocklessEligibility(Player player) {
		player.getScoreboardTags().remove("IndigoBlockless");
	}
}
