package pe.project.locations.quest;

import org.bukkit.Location;
import org.bukkit.World;

import pe.project.locations.LocationMarker;

public class Tutorial extends Quest {
	public Tutorial(World world) {
		super(world, "Prologue", "Tutorial");
		
		addMarker(new LocationMarker(this, new Location(world, -713, 255, -11), "Gather your wits and head outside into Sierhaven.", 0, 20));
		addMarker(new LocationMarker(this, new Location(world, -739, 255, -96), "Head to Captain Murano on the third floor of the Castle, he wants to talk to you.", 21));
		addMarker(new LocationMarker(this, new Location(world, -744, 255, 110), "Talk to Sybil in the Academy, reading her information and choosing your class. When done, she will give you a skill point.", 25, 33));
	}
}
