package pe.project.locations.quest;

import org.bukkit.Location;
import org.bukkit.World;

import pe.project.locations.LocationMarker;

public class StarryNight extends Quest {
	public StarryNight(World world) {
		super(world, "Starry Night", "Quest15");
		
		addMarker(new LocationMarker(this, new Location(world, -135, 255, -1), "Mayleen's telescope lens was taken, but a man named Ed might know a way to get it back. look for him on the south side of Nyr.", 5));
		addMarker(new LocationMarker(this, new Location(world, -120, 255, -15), "Mayleen's telescope just needs to be turned now. Align all the lights in her tower to fix the telescope.", 7));
	}
}
