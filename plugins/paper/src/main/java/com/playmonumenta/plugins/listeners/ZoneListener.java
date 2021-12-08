package com.playmonumenta.plugins.listeners;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFromToEvent;

import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.scriptedquests.zones.ZonePropertyChangeEvent;

public class ZoneListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
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

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockFromToEvent(BlockFromToEvent event) {
		if (event.getBlock().getType() == Material.DRAGON_EGG
			&& ZoneUtils.hasZoneProperty(event.getToBlock().getLocation(), ZoneUtils.ZoneProperty.ADVENTURE_MODE)) {
			event.setCancelled(true);
		}
	}

}
