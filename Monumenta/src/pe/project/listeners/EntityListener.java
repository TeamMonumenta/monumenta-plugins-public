package pe.project.listeners;

import java.util.Iterator;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

import pe.project.Plugin;
import pe.project.utils.LocationUtils;
import pe.project.utils.LocationUtils.LocationType;

public class EntityListener implements Listener {
	Plugin mPlugin;

	public EntityListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void EntitySpawnEvent(EntitySpawnEvent event) {
		Entity entity = event.getEntity();
		mPlugin.mTrackingManager.addEntity(entity);
	}

	//	Entity Explode Event
	//	Cancel explosions in safezones
	@EventHandler(priority = EventPriority.LOWEST)
	public void EntityExplodeEvent(EntityExplodeEvent event) {
		// Cancel the event immediately if within a safezone
		LocationType zone = LocationUtils.getLocationType(mPlugin, event.getLocation());
		if (zone != LocationType.None) {
			event.setCancelled(true);
			return;
		}

		Iterator<Block> iter = event.blockList().iterator();
		while (iter.hasNext()) {
			Block block = iter.next();

			// If any block damaged by an explosion is with a safezone, cancel the explosion
			if (LocationUtils.getLocationType(mPlugin, block.getLocation()) != LocationType.None) {
				event.setCancelled(true);
				return;
			}
		}
	}
}
