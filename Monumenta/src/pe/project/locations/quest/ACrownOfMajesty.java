package pe.project.locations.quest;

import org.bukkit.Location;
import org.bukkit.World;

import pe.project.locations.LocationMarker;
import pe.project.quest.Quest;

public class ACrownOfMajesty extends Quest {
	public ACrownOfMajesty(World world) {
		super(world, "A Crown of Majesty", "Quest01");
		
		addMarker(new LocationMarker(this, new Location(world, -331, 255, -162), "Search the Eastern mine for garnet to help Aimee craft a new royal crown. Return to her once you have all three kinds of gems.", 11));
		addMarker(new LocationMarker(this, new Location(world, -356, 255, 271), "Search the Southeastern Mine for piece of malachite to help Aimee craft a new royal crown. Return to her once you have all three kinds of gems.", 11));
		addMarker(new LocationMarker(this, new Location(world, -533, 255, 460), "Search the Southern Mine for onyx to help Aimee craft a new royal crown. Return to her once you have all three kinds of gems.", 11));
	}
}
