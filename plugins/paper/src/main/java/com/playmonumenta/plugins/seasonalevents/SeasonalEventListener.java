package com.playmonumenta.plugins.seasonalevents;

import com.playmonumenta.plugins.delves.DelvesModifier;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.events.MonumentaEvent;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.redissync.utils.ScoreboardUtils;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class SeasonalEventListener implements Listener {

	public static final String ROD_WAVE_SCOREBOARD = "DRDFinished";

	// Handle all types of events that may award weekly mission progress

	/**
	 * Block break event for tracking spawner based missions
	 */
	@EventHandler(ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		Material blockType = event.getBlock().getType();
		Player p = event.getPlayer();

		if (p != null && blockType == Material.SPAWNER) {
			// Loop through weekly missions and update them if they apply
			int missionNumber = 1;
			for (WeeklyMission mission : SeasonalEventManager.getActiveMissions()) {
				if (mission.mType == WeeklyMissionType.SPAWNERS) {
					SeasonalEventManager.addWeeklyMissionProgress(p, mission, missionNumber, 1);
				}
				if (mission.mType == WeeklyMissionType.SPAWNERS_POI && isPOIContent()) {
					SeasonalEventManager.addWeeklyMissionProgress(p, mission, missionNumber, 1);
				}
				missionNumber++;
			}
		}
	}

	private static boolean isPOIContent() {
		String shard = ServerProperties.getShardName();
		// Add region three here eventually
		return (shard.contains("valley") || shard.contains("isles"));
	}

	/**
	 * Called from the depths manager, with rooms reached.
	 * Run through any potential depths missions and give credit
	 */
	public static void playerCompletedDepths(Player p, int roomNumber) {
		//Check for weekly mission type of depths rooms
		int missionNumber = 1;
		for (WeeklyMission mission : SeasonalEventManager.getActiveMissions()) {
			if (mission.mType == WeeklyMissionType.DEPTHS_ROOMS) {
				SeasonalEventManager.addWeeklyMissionProgress(p, mission, missionNumber, roomNumber);
			} else if (mission.mType == WeeklyMissionType.REGIONAL_CONTENT && 2 == mission.mRegion) {
				// Region matches up - award points
				SeasonalEventManager.addWeeklyMissionProgress(p, mission, missionNumber, 1);
			} else if (mission.mType == WeeklyMissionType.CONTENT && mission.mContent.contains(MonumentaContent.DEPTHS) && roomNumber >= 30) {
				// Content matches up - award points
				SeasonalEventManager.addWeeklyMissionProgress(p, mission, missionNumber, 1);
			}
			missionNumber++;
		}
	}

	/**
	 * Main event handler for tracking Monumenta content completion.
	 * Runs through most weekly mission types and checks for if
	 * credit should be awarded.
	 */
	@EventHandler(ignoreCancelled = true)
	public void playerCompletedContentEvent(MonumentaEvent event) {
		MonumentaContent content = MonumentaContent.getContentSelection(event.getEvent());
		Player p = event.getPlayer();
		if (p != null && content != null) {
			// Loop through weekly missions and update them if they apply
			int missionNumber = 1;
			for (WeeklyMission mission : SeasonalEventManager.getActiveMissions()) {
				if (mission.mType == WeeklyMissionType.CONTENT && mission.mContent.contains(content)) {
					// Content matches up - award points
					SeasonalEventManager.addWeeklyMissionProgress(p, mission, missionNumber, 1);
				} else if (mission.mType == WeeklyMissionType.DUNGEONS && content.getContentType() == ContentType.DUNGEON) {
					// Dungeon matches up - award points
					SeasonalEventManager.addWeeklyMissionProgress(p, mission, missionNumber, 1);
				} else if (mission.mType == WeeklyMissionType.STRIKES && content.getContentType() == ContentType.STRIKE) {
					// Strike matches up - award points
					SeasonalEventManager.addWeeklyMissionProgress(p, mission, missionNumber, 1);
				} else if (mission.mType == WeeklyMissionType.BOSSES && content.getContentType() == ContentType.BOSS) {
					// Boss matches up - award points
					SeasonalEventManager.addWeeklyMissionProgress(p, mission, missionNumber, 1);
				} else if ((mission.mType == WeeklyMissionType.DELVE_MODIFIER || mission.mType == WeeklyMissionType.DELVE_POINTS) && DelvesUtils.SHARD_SCOREBOARD_PREFIX_MAPPINGS.containsKey(content.getLabel())) {
					// Content is eligible for delves- get scores and check for modifier
					if (mission.mType == WeeklyMissionType.DELVE_POINTS && DelvesUtils.getPlayerTotalDelvePoint(null, p, content.getLabel()) >= mission.mDelvePoints) {
						if (mission.mContent == null || mission.mContent.contains(content)) {
							SeasonalEventManager.addWeeklyMissionProgress(p, mission, missionNumber, 1);
						}
					} else if (mission.mType == WeeklyMissionType.DELVE_MODIFIER) {
						boolean modsActive = true;
						for (DelvesModifier modifier : mission.mDelveModifiers) {
							if (DelvesUtils.getDelveModLevel(p, content.getLabel(), modifier) < mission.mModifierRank) {
								modsActive = false;
							}
						}

						if ((mission.mContent == null || mission.mContent.contains(content)) && modsActive) {
							SeasonalEventManager.addWeeklyMissionProgress(p, mission, missionNumber, 1);
						}
					}
				} else if (mission.mType == WeeklyMissionType.REGIONAL_CONTENT && content.getRegion() == mission.mRegion) {
					// Region matches up - award points
					SeasonalEventManager.addWeeklyMissionProgress(p, mission, missionNumber, 1);
				} else if (mission.mType == WeeklyMissionType.ROD_WAVES && content == MonumentaContent.RUSH) {
					// Cleared rod- add number of waves cleared
					int waves = ScoreboardUtils.getScoreboardValue(p.getName(), ROD_WAVE_SCOREBOARD);
					// Subtract the 20 wave checkpoint if player has certain tags
					Set<String> tags = p.getScoreboardTags();
					if (tags.contains("rod_checkpoint_start") || tags.contains("Primary") || tags.contains("Partner")) {
						waves -= 20;
					}

					if (waves > 0) {
						SeasonalEventManager.addWeeklyMissionProgress(p, mission, missionNumber, waves);
					}
				} else if (mission.mType == WeeklyMissionType.DAILY_BOUNTY && (content == MonumentaContent.KINGS_BOUNTY || content == MonumentaContent.CELSIAN_BOUNTY || content == MonumentaContent.RING_BOUNTY)) {
					SeasonalEventManager.addWeeklyMissionProgress(p, mission, missionNumber, 1);
				} else if (mission.mType == WeeklyMissionType.DELVE_BOUNTY && content == MonumentaContent.DELVE_BOUNTY) {
					SeasonalEventManager.addWeeklyMissionProgress(p, mission, missionNumber, 1);
				}
				missionNumber++;
			}
		}
	}
}

