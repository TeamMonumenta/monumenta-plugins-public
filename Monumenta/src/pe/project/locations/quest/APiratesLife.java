package pe.project.locations.quest;

import org.bukkit.Location;
import org.bukkit.World;

import pe.project.locations.LocationMarker;

public class APiratesLife extends Quest {
	public APiratesLife(World world) {
		super(world, "A Pirate's Life", "Quest14");
		
		addMarker(new LocationMarker(this, new Location(world, -402, 255, 209), "Find a peg-leg for Levyn down in the old docks.", 4));
		addMarker(new LocationMarker(this, new Location(world, -841, 255, 427), "Look near the lighthouse for the Cave of Tears and find a parrot for Levyn.", 8));
	}
}
