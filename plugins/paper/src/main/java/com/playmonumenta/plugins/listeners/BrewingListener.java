package com.playmonumenta.plugins.listeners;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;

public class BrewingListener implements Listener {
	@EventHandler(ignoreCancelled = true)
	public void brewEvent(BrewEvent brewEvent) {
		brewEvent.setCancelled(true);
		Location loc = brewEvent.getBlock().getLocation();
		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_SPLASH_POTION_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
	}
}
