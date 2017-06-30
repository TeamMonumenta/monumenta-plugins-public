package pe.project.locations.quest;

import org.bukkit.Location;
import org.bukkit.World;

import pe.project.locations.LocationMarker;

public class ThePlague extends Quest {
	public ThePlague(World world) {
		super(world, "The Plague", "Quest07");
		
		addMarker(new LocationMarker(this, new Location(world, 233, 255, 282), "Nyr is infected with a mysterious illness! Only a Tlaxan Shaman can help save the town.", 5));
		addMarker(new LocationMarker(this, new Location(world, 452, 255, 105), "C'Taz needs exactly 12 dark amber for the first ingredient of the medicine, hidden within a massive tree.", 6));
		addMarker(new LocationMarker(this, new Location(world, 426, 255, 249), "Next C'Taz needs precisely 16 spirit flowers from the hallowed pond north of his hut.", 8));
		addMarker(new LocationMarker(this, new Location(world, 311, 255, -76), "Finally, bring C'Taz 10 pieces of Haunted Earth from the dangerous ruins east of Nyr.", 10));
		addMarker(new LocationMarker(this, new Location(world, -105, 255, -114), "Hurry! Bring the medicine to Dr Laurie so that Nyr will be safe once more.", 16, 17));
	}
}
