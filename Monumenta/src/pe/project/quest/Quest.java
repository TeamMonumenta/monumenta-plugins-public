package pe.project.quest;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;
import org.bukkit.entity.Player;

import pe.project.locations.LocationMarker;
import pe.project.utils.ScoreboardUtils;

public class Quest {
	protected World mWorld;
	protected String mQuestName;
	protected String mScoreboard;
	List<LocationMarker> mMarkers = new ArrayList<LocationMarker>();
	
	public Quest(World world, String questName, String scoreboard) {
		mWorld = world;
		mQuestName = questName;
		mScoreboard = scoreboard;
	}
	
	public List<LocationMarker> getMarkers(Player player) {
		int questScore = ScoreboardUtils.getScoreboardValue(player, mScoreboard);
		List<LocationMarker> availableMarkers = new ArrayList<LocationMarker>();
		
		for (LocationMarker marker : mMarkers) {
			if (marker.isActiveMarker(questScore)) {
				availableMarkers.add(marker);
			}
		}
		
		return availableMarkers;
	}
	
	public String getQuestName() {
		return mQuestName;
	}
	
	protected void addMarker(LocationMarker marker) {
		mMarkers.add(marker);
	}
}
