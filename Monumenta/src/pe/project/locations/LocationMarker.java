package pe.project.locations;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.World;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.npcs.quest.QuestPrerequisites;

public class LocationMarker {
	private QuestPrerequisites mPrerequisites;
	private Location mLoc;
	private String mMessage;

	public LocationMarker(World world, JsonElement element) throws Exception {
		JsonObject object = element.getAsJsonObject();
		if (object == null) {
			throw new Exception("locations value is not an object!");
		}

		// Reuse the same pre-requisites logic as the scripted quests
		JsonElement prereq = object.get("prerequisites");
		if (prereq == null) {
			throw new Exception("Failed to parse location prerequisites!");
		}
		mPrerequisites = new QuestPrerequisites(prereq);

		// Read x coordinate
		JsonElement xElement = object.get("x");
		int x = 0;
		if (xElement == null) {
			throw new Exception("Failed to parse location x value!");
		}
		x = xElement.getAsInt();

		// Read z coordinate
		JsonElement zElement = object.get("z");
		int z = 0;
		if (zElement == null) {
			throw new Exception("Failed to parse location z value!");
		}
		z = zElement.getAsInt();

		// Read message
		JsonElement msgElement = object.get("message");
		if (msgElement == null) {
			throw new Exception("Failed to parse location message!");
		}
		mMessage = msgElement.getAsString();
		if (mMessage == null) {
			throw new Exception("Failed to parse location message as string!");
		}

		// Compute location
		mLoc = new Location(world, x, 255, z);
	}

	public Location getLocation() {
		return mLoc;
	}

	public String getMessage() {
		return mMessage;
	}

	public boolean prerequisitesMet(Player player) {
		return mPrerequisites.prerequisitesMet(player);
	}
}
