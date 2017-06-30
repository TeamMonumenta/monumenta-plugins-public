package pe.project.locations.quest;

import org.bukkit.Location;
import org.bukkit.World;

import pe.project.locations.LocationMarker;

public class RunawayPet extends Quest {
	public RunawayPet(World world) {
		super(world, "Runaway Pet", "Quest09");
		
		addMarker(new LocationMarker(this, new Location(world, 742, 255, 314), "Find 'Mr. Snuggles' in a tower near Farr and bring him back to Leonard.", 4));
	}
}
