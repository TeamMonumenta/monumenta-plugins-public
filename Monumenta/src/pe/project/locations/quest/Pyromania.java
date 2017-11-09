package pe.project.locations.quest;

import org.bukkit.Location;
import org.bukkit.World;

import pe.project.locations.LocationMarker;

public class Pyromania extends Quest {
	public Pyromania(World world) {
		super(world, "Pyromania", "Quest10");
		
		addMarker(new LocationMarker(this, new Location(world, 935, 255, -145), "Carbocius needs magma samples. Retrieve one from the volcano in the Highlands, and return when you have samples from 3 locations.", 3));
		addMarker(new LocationMarker(this, new Location(world, 570, 255, -190), "Carbocius needs magma samples. Retrieve one from the fire cave, west of the Monastery, and return when you have samples from 3 locations.", 3));
		addMarker(new LocationMarker(this, new Location(world, 744, 255, 88), "Carbocius needs magma samples. Retrieve one from the molten chasm, east of Farr, and return when you have samples from 3 locations.", 3));
	}
}
