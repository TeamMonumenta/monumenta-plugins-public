package pe.project.locations.quest;

import org.bukkit.Location;
import org.bukkit.World;

import pe.project.locations.LocationMarker;

public class NelfinesCurse extends Quest {
	public NelfinesCurse(World world) {
		super(world, "Nelfine's Curse", "Chat07");
		
		addMarker(new LocationMarker(this, new Location(world, -775, 255, -4), "Get a cryptic message from Nelfine, decipher where to go, and solve a puzzle in order to try to break her curse.", 5));
		addMarker(new LocationMarker(this, new Location(world, -775, 255, -4), "Now that you've broken that magic sigil, head back to Nelfine and see if the curse is lifted.", 8, 9));
	}
}
