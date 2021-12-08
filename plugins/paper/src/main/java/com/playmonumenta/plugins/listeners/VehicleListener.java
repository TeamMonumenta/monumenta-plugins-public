package com.playmonumenta.plugins.listeners;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Boat;
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
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

public class VehicleListener implements Listener {
	Plugin mPlugin;

	public VehicleListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void vehicleCreateEvent(VehicleCreateEvent event) {

		// prevent boats from being placed near barrier blocks
		// anti barrier-passthrough glitch
		if (antiBoatBarrierPasstrough(event)) {
			event.setCancelled(true);
			return;
		}

		mPlugin.mTrackingManager.addEntity(event.getVehicle());
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void vehicleDestroyEvent(VehicleDestroyEvent event) {
		mPlugin.mTrackingManager.removeEntity(event.getVehicle());
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void vehicleEntityCollisionEvent(VehicleEntityCollisionEvent event) {
		Entity entity = event.getEntity();
		Vehicle vehicle = event.getVehicle();

		if (!(entity instanceof Player) && ZoneUtils.hasZoneProperty(vehicle, ZoneProperty.NO_VEHICLES)
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

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void vehicleEnterEvent(VehicleEnterEvent event) {
		Entity entity = event.getEntered();
		Vehicle vehicle = event.getVehicle();

		if (!(vehicle instanceof AbstractHorse) && !(entity instanceof Player) && ZoneUtils.hasZoneProperty(vehicle, ZoneProperty.NO_VEHICLES)) {
			/*
			 * Vehicles cannot be entered if:
			 *
			 * The vehicle is not a horse
			 * AND
			 * The entity entering is not a player
			 * AND
			 * the vehicle is inside of a safezone
			 */
			Location loc = entity.getLocation();
			entity.teleport(loc);
			event.setCancelled(true);
		}
	}

	// prevent boats from being placed near barrier blocks
	// anti barrier-passthrough glitch
	private boolean antiBoatBarrierPasstrough(VehicleCreateEvent event) {

		Vehicle vehicle = event.getVehicle();
		if (vehicle instanceof Boat) {
			// check a 5x5 box around the player.
			// if one of those block is a no-passthrough block, cancel the spawn.
			World world = vehicle.getLocation().getWorld();
			int x = vehicle.getLocation().getBlockX() - 2;
			int y = vehicle.getLocation().getBlockY() - 2;
			int z = vehicle.getLocation().getBlockZ() - 2;

			for (int xi = 0; xi < 5; xi++) {
				for (int yi = 0; yi < 5; yi++) {
					for (int zi = 0; zi < 5; zi++) {
						if (ItemUtils.noPassthrough.contains(world.getBlockAt(x + xi, y + yi, z + zi).getType())) {
							return true;
						}
					}
				}
			}
		}

		return false;
	}
}
