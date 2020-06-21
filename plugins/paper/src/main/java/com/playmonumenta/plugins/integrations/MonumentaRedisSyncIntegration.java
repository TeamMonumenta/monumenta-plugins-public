
package com.playmonumenta.plugins.integrations;

import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.redissync.event.PlayerServerTransferEvent;

public class MonumentaRedisSyncIntegration implements Listener {
	private final Logger mLogger;

	public MonumentaRedisSyncIntegration(Logger logger) {
		mLogger = logger;
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void playerServerTransferEvent(PlayerServerTransferEvent event) {
		Player player = event.getPlayer();
		mLogger.info("PlayerTransferEvent: Player: " + player + "   Target: " + event.getTarget());

		int dropped = InventoryUtils.removeSpecialItems(player, false);
		if (dropped == 1) {
			player.sendMessage(ChatColor.RED + "The dungeon key you were carrying was dropped!");
		} else if (dropped > 1) {
			player.sendMessage(ChatColor.RED + "The dungeon keys you were carrying were dropped!");
		}
	}
}
