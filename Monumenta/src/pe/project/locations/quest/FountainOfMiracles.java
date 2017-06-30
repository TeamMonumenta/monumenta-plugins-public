package pe.project.locations.quest;

import org.bukkit.Location;
import org.bukkit.World;

import pe.project.locations.LocationMarker;

public class FountainOfMiracles extends Quest {
	public FountainOfMiracles(World world) {
		super(world, "Fountain of Miracles", "Quest08");
		
		addMarker(new LocationMarker(this, new Location(world, 536, 255, 357), "Drake spoke of a mysterious fountain in the wilderness. Search the southern jungle for this legend.", 9));
		addMarker(new LocationMarker(this, new Location(world, 615, 255, 471), "The fountain has been corrupted! The island to the east of it must be purified by slaying the venomous spiders that dwell there.", 10, 17));
		addMarker(new LocationMarker(this, new Location(world, 503, 255, 433), "The beasts have been slain and the source purified! Return to the fountain in the caves for a blessing.", 19));
	}
}
