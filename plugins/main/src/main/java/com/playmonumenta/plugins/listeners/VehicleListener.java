package com.playmonumenta.plugins.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.safezone.SafeZoneManager.LocationType;

public class VehicleListener implements Listener {
	Plugin mPlugin;

	public VehicleListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void VehicleCreateEvent(VehicleCreateEvent event) {
		mPlugin.mTrackingManager.addEntity(event.getVehicle());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void VehicleDestroyEvent(VehicleDestroyEvent event) {
		mPlugin.mTrackingManager.removeEntity(event.getVehicle());
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void VehicleEntityCollisionEvent(VehicleEntityCollisionEvent event) {
		Entity entity = event.getEntity();
		Vehicle vehicle = event.getVehicle();

		if (!(entity instanceof Player) && mPlugin.mSafeZoneManager.getLocationType(vehicle) != LocationType.None
			&& !vehicle.getType().equals(entity.getType())) {
			/*
			 * Vehicles are removed if they:
			 *
			 * Collide with a non-player entity
			 * AND
			 * are inside a safezone
			 */
			vehicle.remove();
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void VehicleEnterEvent(VehicleEnterEvent event) {
		Entity entity = event.getEntered();
		Vehicle vehicle = event.getVehicle();

		if (!(entity instanceof Player) && mPlugin.mSafeZoneManager.getLocationType(vehicle) != LocationType.None) {
			/*
			 * Vehicles cannot be entered if:
			 *
			 * The entity entering is not a player
			 * AND
			 * the vehicle is inside of a safezone
			 */
			Location loc = entity.getLocation();
			entity.teleport(loc);
			event.setCancelled(true);
		}
	}
}
