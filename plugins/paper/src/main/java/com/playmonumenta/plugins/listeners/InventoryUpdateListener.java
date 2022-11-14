package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.scheduler.BukkitRunnable;

// This class exists to work around weird behavior in 1.18 where picking up items doesn't update your inventory
public class InventoryUpdateListener implements Listener {
	private Map<UUID, BukkitRunnable> mUpdateRunnables = new HashMap<>();

	// An entity picked up an item, monitor status runs after everything else
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityPickupItemEvent(EntityPickupItemEvent event) {
		if (event.getEntity() instanceof Player player) {
			// Schedule an inventory update if one isn't already scheduled.
			// This avoids scheduling more than once if multiple items were picked up in the
			// same tick
			mUpdateRunnables.computeIfAbsent(player.getUniqueId(), (uuid) -> {
				BukkitRunnable runnable = new BukkitRunnable() {
					@Override
					public void run() {
						// Update the player's inventory (resend packets) and remove the scheduled task
						// from the map
						mUpdateRunnables.remove(player.getUniqueId());
						player.updateInventory();
					}
				};

				runnable.runTaskLater(Plugin.getInstance(), 2);
				return runnable;
			});
		}
	}
}
