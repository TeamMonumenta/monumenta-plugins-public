package com.playmonumenta.plugins.listeners;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.playmonumenta.scriptedquests.zones.ZonePropertyChangeEvent;

public class ZonePropertyListener implements Listener {
	@EventHandler(priority = EventPriority.LOWEST)
	public void zonePropertyChangeEvent(ZonePropertyChangeEvent event) {
		Player player = event.getPlayer();
		String layer = event.getLayer();
		String property = event.getProperty();

		GameMode mode = player.getGameMode();

		if (layer.equals("default")) {
			switch (property) {
			case "Adventure Mode":
				if (mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR) {
					player.setGameMode(GameMode.ADVENTURE);
				}
				break;
			case "!Adventure Mode":
				if (mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR) {
					player.setGameMode(GameMode.SURVIVAL);
				}
				break;
			default:
				// Do nothing
			}
		}
	}
}
