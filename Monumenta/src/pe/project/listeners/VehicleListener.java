package pe.project.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleEnterEvent;

public class VehicleListener implements Listener {
	@EventHandler(priority = EventPriority.LOWEST)
	public void VehicleEnterEvent(VehicleEnterEvent event) {
		Entity entity = event.getEntered();

		if (entity instanceof Player) {
		} else {
			// Only players are allowed to enter vehicles
			event.setCancelled(true);
		}
	}
}
