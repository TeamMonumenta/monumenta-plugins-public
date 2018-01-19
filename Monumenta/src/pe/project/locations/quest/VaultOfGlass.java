package pe.project.locations.quest;

import org.bukkit.Location;
import org.bukkit.World;

import pe.project.locations.LocationMarker;

public class VaultOfGlass extends Quest {
	public VaultOfGlass(World world) {
		super(world, "A Vault of Glass", "Quest18");
		
		addMarker(new LocationMarker(this, new Location(world, -689, 141, -3), "The urgent letter said to climb the tower in Sierhaven with the purple and black banners.", 1));
		addMarker(new LocationMarker(this, new Location(world, 581, 99, 171), "Look around Farr for clues to where Carlton fled to.", 3));
		addMarker(new LocationMarker(this, new Location(world, 629, 97, 208), "Find your way through the maze beneath the Five Axes Trading Co warehouse.", 6))
		addMarker(new LocationMarker(this, new Location(world, 629, 97, 208), "Find your way through the maze beneath the Five Axes Trading Co warehouse.", 7));
		addMarker(new LocationMarker(this, new Location(world, -689, 141, -3), "Grab the ledger and head back to Argent! He needs to see it ASAP.", 8));
		addMarker(new LocationMarker(this, new Location(world, -689, 141, -3), "The ledger is gone. Tell Argent what happened.", 9));
  	}
}
