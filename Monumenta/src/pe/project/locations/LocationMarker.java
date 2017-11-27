package pe.project.locations;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import pe.project.locations.quest.Quest;

public class LocationMarker {
	private Quest mQuestOwner;
	private Location mLoc;
	private String mDescription;
	private int mMinRange;
	private int mMaxRange;

	public LocationMarker(Quest owner, Location loc, String description, int value) {
		mQuestOwner = owner;
		mLoc = loc;
		mDescription = description;
		mMinRange = value;
		mMaxRange = value;
	}

	public LocationMarker(Quest owner, Location loc, String description, int minRange, int maxRange) {
		mQuestOwner = owner;
		mLoc = loc;
		mDescription = description;
		mMinRange = minRange;
		mMaxRange = maxRange;
	}

	public Location getLocation() {
		return mLoc;
	}

	public String getMarkerDescription(Player player) {
		List<LocationMarker> markers = mQuestOwner.getMarkers(player);

		//	Loop through and count how many markers are with out range and also find out which number we are.
		int count = 0;
		int index = 0;
		for (int i = 0; i < markers.size(); i++) {
			LocationMarker currMarker = markers.get(i);

			if (isWithinRange(currMarker.mMinRange, currMarker.mMaxRange)) {
				count++;
			}

			if (this == currMarker) {
				index = i;
			}
		}

		String questNumber = "";
		if (count > 1) {
			questNumber = "[" +  (index+1) + "/" + count + "]";
		}

		String questName = (mQuestOwner != null) ? mQuestOwner.getQuestName() : "";
		String message = ChatColor.AQUA + "" + ChatColor.BOLD + questName + ChatColor.RESET + " " + ChatColor.AQUA + questNumber + ": " + mDescription;

		return message;
	}

	public boolean isWithinRange(int min, int max) {
		return mMinRange >= min && mMaxRange <= max;
	}

	public boolean isActiveMarker(int value) {
		return value >= mMinRange && value <= mMaxRange;
	}
}
