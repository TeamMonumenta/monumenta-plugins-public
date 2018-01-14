package pe.project.locations.quest;

import org.bukkit.Location;
import org.bukkit.World;

import pe.project.locations.LocationMarker;

public class BanditTroubles extends Quest {
	public BanditTroubles(World world) {
		super(world, "Bandit Troubles", "Quest02");

		addMarker(new LocationMarker(this, new Location(world, -831, 255, -396), "Slay a Bandit Leader in their northwestern stronghold for Octavius. Get evidence of your victory and head to the next camp.", 4));
		addMarker(new LocationMarker(this, new Location(world, -398, 255, -373), "Slay a Bandit Leader in a hilltop fortress for Octavius. If you have proof of both kills, return to Octavius.", 4));
		addMarker(new LocationMarker(this, new Location(world, -673, 255, 71), "Talk to Octavius and decide what to do about the evidence you retrieved from the bandit leaders", 5));
		addMarker(new LocationMarker(this, new Location(world, -738, 255, -95), "Bring the suspicious note to Captain Murano", 9));
	}
}
