package com.playmonumenta.plugins.listeners;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
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
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.LocationUtils.LocationType;

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

		if (!(entity instanceof Player)
		    || (vehicle instanceof Minecart
		        && (((Player)entity).getGameMode() != GameMode.SURVIVAL
					|| LocationUtils.getLocationType(mPlugin, entity) != LocationType.Capital))) {
			/*
			 * Vehicles are removed if they:
			 *
			 * Collide with non-player entities
			 * OR
			 * are minecarts that collide with anything outside of a safezone
			 */
			vehicle.remove();
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void VehicleEnterEvent(VehicleEnterEvent event) {
		Entity entity = event.getEntered();
		Vehicle vehicle = event.getVehicle();

		if (!(entity instanceof Player)
		    || (vehicle instanceof Minecart
		        && (((Player)entity).getGameMode() != GameMode.SURVIVAL
					|| LocationUtils.getLocationType(mPlugin, entity) != LocationType.Capital))) {

			/*
			 * Only players are allowed to enter vehicles
			 * If the vehicle is a minecart, can only enter in the capital
			 */
			Location loc = entity.getLocation();
			entity.teleport(loc);
			event.setCancelled(true);
		}
	}
}
