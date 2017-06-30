package pe.project.locations.quest;

import org.bukkit.Location;
import org.bukkit.World;

import pe.project.locations.LocationMarker;

public class BuriedBlade extends Quest {
	public BuriedBlade(World world) {
		super(world, "Buried Blade", "Quest05");
		
		addMarker(new LocationMarker(this, new Location(world, 159, 255, -23), "Blackjack Stan lost a sword to a cave collapse. Take his key and look behind the gravel for it in the overgrown cave.", 5));
	}
}
