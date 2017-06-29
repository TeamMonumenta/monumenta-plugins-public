package pe.project.managers;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;
import org.bukkit.entity.Player;

import pe.project.Main;
import pe.project.locations.LocationMarker;
import pe.project.locations.quest.*;
import pe.project.quest.Quest;
import pe.project.utils.MessagingUtils;
import pe.project.utils.ScoreboardUtils;

public class QuestManager {
	Main mPlugin;
	World mWorld;
	List<Quest> mQuest = new ArrayList<Quest>();
	
	public QuestManager(Main plugin, World world) {
		mPlugin = plugin;
		mWorld = world;
		
		_loadQuestMarkers();
	}
	
	private void _loadQuestMarkers() {
		//	Tutorial
		_registerQuest(new Tutorial(mWorld));
			
		//	Capital
		_registerQuest(new ACrownOfTopaz(mWorld));
		_registerQuest(new ACrownOfMajesty(mWorld));
		_registerQuest(new BanditTroubles(mWorld));
		_registerQuest(new MagesLegacy(mWorld));
		_registerQuest(new APiratesLife(mWorld));
			
		//	Nyr
		_registerQuest(new BuriedBlade(mWorld));
		_registerQuest(new MissingSoldiers(mWorld));
		_registerQuest(new ThePlague(mWorld));
			
		//	Farr
		_registerQuest(new FountainOfMiracles(mWorld));
		_registerQuest(new RunawayPet(mWorld));
		_registerQuest(new Pyromania(mWorld));
		_registerQuest(new OfMonksAndMagic(mWorld));
		_registerQuest(new SonsOfTheForest(mWorld));
		_registerQuest(new StarryNight(mWorld));
	}
	
	private void _registerQuest(Quest quest) {
		mQuest.add(quest);
	}
	
	public void showCurrentQuest(Player player) {
		int index = ScoreboardUtils.getScoreboardValue(player, "locationIndex");
		
		List<LocationMarker> markers = new ArrayList<LocationMarker>();
		for (Quest quest : mQuest) {
			markers.addAll(quest.getMarkers(player));
		}
		
		if (index >= markers.size()) {
			index = 0;
		}
		
		if (markers.size() == 0) {
			MessagingUtils.sendAbilityTriggeredMessage(mPlugin, player, "You have no active quest.");
		} else {
			LocationMarker currentMarker = markers.get(index);
			String description = currentMarker.getMarkerDescription(player);
			
			player.sendMessage(description);
			player.setCompassTarget(currentMarker.getLocation());
		}
	}
	
	public void cycleQuestTracker(Player player) {
		int index = ScoreboardUtils.getScoreboardValue(player, "locationIndex");
		index++;
		
		List<LocationMarker> markers = new ArrayList<LocationMarker>();
		for (Quest quest : mQuest) {
			markers.addAll(quest.getMarkers(player));
		}
		
		if (index >= markers.size()) {
			index = 0;
		}
		
		if (markers.size() == 0) {
			MessagingUtils.sendAbilityTriggeredMessage(mPlugin, player, "You have no active quest.");
		} else {
			LocationMarker currentMarker = markers.get(index);
			String description = currentMarker.getMarkerDescription(player);
			
			player.sendMessage(description);
			player.setCompassTarget(currentMarker.getLocation());
		}
		
		ScoreboardUtils.setScoreboardValue(player, "locationIndex", index);
	}
}
