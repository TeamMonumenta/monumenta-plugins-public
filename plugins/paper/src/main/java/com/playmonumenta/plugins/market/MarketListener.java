package com.playmonumenta.plugins.market;

import com.playmonumenta.redissync.event.PlayerSaveEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MarketListener implements Listener {

	@EventHandler(ignoreCancelled = true)
	public void playerQuit(PlayerQuitEvent event) {
		MarketManager.getInstance().onLogout(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerSaveEvent(PlayerSaveEvent event) {
		MarketManager.getInstance().playerSaveEvent(event);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		MarketManager.getInstance().playerJoinEvent(event);
	}

}

