package pe.project.listeners;

import org.bukkit.GameMode;
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

import pe.project.Plugin;

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

		if (entity.getVehicle() != vehicle) {
			if (entity instanceof Player) {
				Player player = (Player)entity;
				if (player.getGameMode() == GameMode.ADVENTURE) {
					vehicle.remove();
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void VehicleEnterEvent(VehicleEnterEvent event) {
		Entity entity = event.getEntered();

		if (entity instanceof Player) {
			// Players are allowed to enter boats unless they are in adventure mode
			if (((Player)entity).getGameMode() == GameMode.ADVENTURE) {
				event.setCancelled(true);
			}
		} else {
			// Only players are allowed to enter vehicles
			Location loc = entity.getLocation();
			entity.teleport(loc);
			event.setCancelled(true);
		}
	}

}
