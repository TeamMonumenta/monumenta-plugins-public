package pe.project.locations.quest;

import org.bukkit.Location;
import org.bukkit.World;

import pe.project.locations.LocationMarker;
import pe.project.quest.Quest;

public class OfMonksAndMagic extends Quest {
	public OfMonksAndMagic(World world) {
		super(world, "Of Monks and Magic", "Quest11");
		
		addMarker(new LocationMarker(this, new Location(world, 671, 255, -268), "The Axtan Monks haven't been heard from lately. Captain Tobias asks that you see if they're alright.", 8));
		addMarker(new LocationMarker(this, new Location(world, 721, 255, -269), "The Monks are under attack by some shadowy druids! Kill them before they can finish their plan!", 10, 18));
		addMarker(new LocationMarker(this, new Location(world, 565, 255, 126), "The Monks are safe for now. Return to Captain Tobias and warn him of this new threat, the Sons of the Forest.", 20));
	}
}
