package pe.project.managers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import pe.project.Constants;
import pe.project.Plugin;
import pe.project.locations.LocationMarker;
import pe.project.locations.quest.*;
import pe.project.utils.FileUtils;
import pe.project.utils.MessagingUtils;
import pe.project.utils.ScoreboardUtils;

public class QuestCompassManager {
	Plugin mPlugin;
	List<QuestCompass> mQuests = new ArrayList<QuestCompass>();

	public QuestCompassManager(Plugin plugin) {
		mPlugin = plugin;

		reload(plugin, null);
	}

	/*
	 * If sender is non-null, it will be sent debugging information
	 */
	public void reload(Plugin plugin, CommandSender sender) {
		if (!Constants.QUEST_MANAGER_ENABLED) {
			return;
		}

		mQuests = new ArrayList<QuestCompass>();

		ArrayList<File> listOfFiles;
		ArrayList<String> listOfQuests = new ArrayList<String>();
		int numQuestLocations = 0;
		int numFiles = 0;

		// Attempt to load all JSON files in subdirectories of "quest_compass"
		try {
			String questCompassLocation = plugin.getDataFolder() + File.separator +  "quest_compass";
			listOfFiles = FileUtils.getFilesInDirectory(questCompassLocation, ".json");
		} catch (IOException e) {
			plugin.getLogger().severe("Caught exception trying to reload quest compass: " + e);
			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Caught exception trying to reload quest compass: " + e);
			}
			return;
		}

		for (File file : listOfFiles) {
			try {
				// Load this file into an NpcQuest object
				QuestCompass quest = new QuestCompass(plugin.mWorld, file.getPath());

				// Keep track of statistics for pretty printing later
				int newLocations = quest.getMarkers().size();
				numQuestLocations += newLocations;
				numFiles++;
				listOfQuests.add(quest.getQuestName() + ":" + Integer.toString(newLocations));
				mQuests.add(quest);
			} catch (Exception e) {
				plugin.getLogger().severe("Caught exception: " + e);
				e.printStackTrace();

				if (sender != null) {
					sender.sendMessage(ChatColor.RED + "Failed to load quest file '" + file.getPath() + "'");
					MessagingUtils.sendStackTrace(sender, e);
				}
			}
		}

		if (sender != null) {
			sender.sendMessage(ChatColor.GOLD + "Loaded " +
			                   Integer.toString(numQuestLocations) +
			                   " quest compass locations from " + Integer.toString(numFiles) + " files");

			Collections.sort(listOfQuests);
			String outMsg = "";
			for (String npc : listOfQuests) {
				if (outMsg.isEmpty()) {
					outMsg = npc;
				} else {
					outMsg = outMsg + ", " + npc;
				}

				if (outMsg.length() > 1000) {
					sender.sendMessage(ChatColor.GOLD + outMsg);
					outMsg = "";
				}
			}

			if (!outMsg.isEmpty()) {
				sender.sendMessage(ChatColor.GOLD + outMsg);
			}
		}
	}

	private int _showCurrentQuest(Player player, int index) {
		List<LocationMarker> markers = new ArrayList<LocationMarker>();
		List<String> markerTitles = new ArrayList<String>();
		for (QuestCompass quest : mQuests) {
			List <LocationMarker> questMarkers = quest.getMarkers(player);

			// Add all the valid markers to a list, and add their titles to another list
			markers.addAll(quest.getMarkers(player));
			for (int i = 0; i < questMarkers.size(); i++) {
				String title = ChatColor.AQUA + "" + ChatColor.BOLD + quest.getQuestName()
				               + ChatColor.RESET + "" + ChatColor.AQUA;

				if (questMarkers.size() > 1) {
					title += " [" + (i + 1) + "/" + questMarkers.size() + "]";
				}

				markerTitles.add(title);
			}
		}

		if (index >= markers.size()) {
			index = 0;
		}

		if (markers.size() == 0) {
			MessagingUtils.sendAbilityTriggeredMessage(mPlugin, player, "You have no active quest.");
		} else {
			LocationMarker currentMarker = markers.get(index);

			MessagingUtils.sendRawMessage(player, markerTitles.get(index) + ": " + currentMarker.getMessage());

			player.setCompassTarget(currentMarker.getLocation());
		}

		return index;
	}

	public void showCurrentQuest(Player player) {
		if (!Constants.QUEST_MANAGER_ENABLED) {
			return;
		}

		int index = ScoreboardUtils.getScoreboardValue(player, "locationIndex");

		_showCurrentQuest(player, index);
	}

	public void cycleQuestTracker(Player player) {
		if (!Constants.QUEST_MANAGER_ENABLED) {
			return;
		}

		int index = ScoreboardUtils.getScoreboardValue(player, "locationIndex") + 1;

		index = _showCurrentQuest(player, index);

		ScoreboardUtils.setScoreboardValue(player, "locationIndex", index);
	}
}
