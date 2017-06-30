package pe.project.locations.quest;

import org.bukkit.Location;
import org.bukkit.World;

import pe.project.locations.LocationMarker;

public class SonsOfTheForest extends Quest {
	public SonsOfTheForest(World world) {
		super(world, "Sons of the Forest", "Quest12");
		
		addMarker(new LocationMarker(this, new Location(world, 759, 255, -87), "A small fortified camp northeast of Farr is the Sons of the Forest's base. Attack it and see what you can find.", 6));
		addMarker(new LocationMarker(this, new Location(world, -735, 255, 119), "A dark curse has been placed upon you and assassins are tracking your every move! Hurry to the capital, Vargos can remove the spell!", 7));
	}
}
