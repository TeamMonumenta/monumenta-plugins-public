package pe.project.locations.quest;

import org.bukkit.Location;
import org.bukkit.World;

import pe.project.locations.LocationMarker;

public class SiegeOfHighwatch extends Quest {
	public SiegeOfHighwatch(World world) {
		super(world, "Siege Of Highwatch", "Quest13");
		
		addMarker(new LocationMarker(this, new Location(world, 1196, 255, -106), "Highwatch is under attack! Grab your bow, report to Commander Haynes, and get out there!", 4));
		addMarker(new LocationMarker(this, new Location(world, 1196, 255, -106), "The ghasts maybe down but the battle isn't won! Report to Haynes and destroy the Sons of the Forest's summoning stone!", 5));
		addMarker(new LocationMarker(this, new Location(world, 1196, 255, -106), "Commander Haynes has some good news, rest up after that battle and then report to her office.", 6));
		addMarker(new LocationMarker(this, new Location(world, 1190, 255, -128), "During the battle the Highwatch Scouts captured a Warden of the Forest! Head to the cells and see if you can get any information out of him.", 7));
		addMarker(new LocationMarker(this, new Location(world, 240, 255, 312), "The only way you're going to get information out of that prisoner is with a truth serum. Find a Tlaxan Shaman and see if he'll help out.", 8));
		addMarker(new LocationMarker(this, new Location(world, 240, 255, 312), "C'Taz says you'll need a fish from a 'sunken pool in the jungle' and items from his workshop to make a truth potion. Search carefully for both the ingredients and the recipe.", 9));
		addMarker(new LocationMarker(this, new Location(world, 1190, 255, -128), "With truth potion in hand, head back to Highwatch and interrogate the captured Warden. See if you can get him to tell you how to get into their inner sanctum.", 10, 12));
	}
}
