package pe.project.locations.quest;

import org.bukkit.Location;
import org.bukkit.World;

import pe.project.locations.LocationMarker;

public class MagesLegacy extends Quest {
	public MagesLegacy(World world) {
		super(world, "Mage's Legacy", "Quest03");

		addMarker(new LocationMarker(this, new Location(world, -523, 255, -499), "Scale Ezariah's Tower for some old research notes. Be careful, his apprentices were well-trained.", 9));
		addMarker(new LocationMarker(this, new Location(world, -713, 255, 123), "A magical puzzle on the Academy's roof can translate the notes. Search the library for clues.", 11));
		addMarker(new LocationMarker(this, new Location(world, -320, 255, 343), "Find a reclusive translator in the southern swamp to help. He may have some... odd requests.", 13));
		addMarker(new LocationMarker(this, new Location(world, -735, 255, 119), "Bring the translated books back to Vargos in the academy and he'll tell you his story.", 15));
	}
}
