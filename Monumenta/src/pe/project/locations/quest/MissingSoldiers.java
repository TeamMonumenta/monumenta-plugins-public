package pe.project.locations.quest;

import org.bukkit.Location;
import org.bukkit.World;

import pe.project.locations.LocationMarker;
import pe.project.quest.Quest;

public class MissingSoldiers extends Quest {
	public MissingSoldiers(World world) {
		super(world, "Missing Soldiers", "Quest06");
		
		addMarker(new LocationMarker(this, new Location(world, -4, 255, 124), "Two scouts went missing while watching the Southern Jaguar Camp. See if you can find them... or what's left of them.", 4));
		addMarker(new LocationMarker(this, new Location(world, 268, 255, 7), "Sergeant Kim was observing the Northern Jaguar Camp. Find out what happened to her.", 9));
	}
}
