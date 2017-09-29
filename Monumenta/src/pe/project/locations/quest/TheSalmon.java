package pe.project.locations.quest;

import org.bukkit.Location;
import org.bukkit.World;

import pe.project.locations.LocationMarker;

public class TheSalmon extends Quest {
	public TheSalmon(World world) {
		super(world, "The Salmon", "Quest16");
		
		addMarker(new LocationMarker(this, new Location(world, 728, 255, 455), "Talk to Quentin in the cafe and convince him to tell you about the Regal Salmon.", 6));
		addMarker(new LocationMarker(this, new Location(world, 728, 255, 455), "Find an Axtan Ale and bring it to Quentin so he'll tell you the salmon's secret!", 8));
		addMarker(new LocationMarker(this, new Location(world, 612, 255, 358), "Quentin said you'll need to be unlucky and go fish beneath a 'big cat thing' to find the Regal Salmon.", 10));
		addMarker(new LocationMarker(this, new Location(world, 691, 255, 460), "Bring the Regal Salmon to the dockmaster so you can win this contest!", 7));
	}
}
