package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CharmListener implements Listener {
	Plugin mPlugin;

	public CharmListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(ignoreCancelled = true)
	public void playerJoin(PlayerJoinEvent event) {
		//Load charm data from plugin data
		CharmManager.getInstance().onJoin(event.getPlayer());
	}

	@EventHandler(ignoreCancelled = true)
	public void playerQuit(PlayerQuitEvent event) {
		//Discard local data a few ticks later
		CharmManager.getInstance().onQuit(event.getPlayer());
	}

	@EventHandler(ignoreCancelled = true)
	public void playerSave(PlayerSaveEvent event) {
		//Save local data to charm plugin data
		CharmManager.getInstance().onSave(event);
	}
}
