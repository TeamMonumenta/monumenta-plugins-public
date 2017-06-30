package pe.project.locations.quest;

import org.bukkit.Location;
import org.bukkit.World;

import pe.project.locations.LocationMarker;

public class ACrownOfTopaz extends Quest {
	public ACrownOfTopaz(World world) {
		super(world, "A Crown of Topaz", "Quest01");
		
		addMarker(new LocationMarker(this, new Location(world, -807, 255, -372), "Search the northern mine for topaz to help Aimee.", 4));
	}
}
