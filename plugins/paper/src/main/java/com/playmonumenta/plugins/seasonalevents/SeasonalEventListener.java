package com.playmonumenta.plugins.seasonalevents;

import com.playmonumenta.plugins.delves.DelvesManager;
import com.playmonumenta.plugins.delves.DelvesModifier;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.events.MonumentaEvent;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.redissync.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.List;
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

		if (blockType == Material.SPAWNER) {
			// Loop through weekly missions and update them if they apply
			int missionNumber = 1;
			for (WeeklyMission mission : SeasonalEventManager.getActiveMissions()) {
				if (mission.mType == WeeklyMissionType.SPAWNERS) {
					SeasonalEventManager.addWeeklyMissionProgress(p, missionNumber, 1);
				}
				if (mission.mType == WeeklyMissionType.SPAWNERS_POI && isPOIContent()) {
					SeasonalEventManager.addWeeklyMissionProgress(p, missionNumber, 1);
				}
				missionNumber++;
			}
		}
	}

	private static boolean isPOIContent() {
		String shard = ServerProperties.getShardName();
		return (shard.contains("valley") || shard.contains("isles") || shard.contains("ring"));
	}

	/**
	 * Called from DepthsParty, with rooms reached.
	 * Run through any potential depths missions and give credit
	 */
	public static void playerCompletedDepths(Player p, int roomNumber) {
		//Check for weekly mission type of depths rooms
		int missionNumber = 1;
		for (WeeklyMission mission : SeasonalEventManager.getActiveMissions()) {
			if (mission.mType == WeeklyMissionType.DEPTHS_ROOMS) {
				SeasonalEventManager.addWeeklyMissionProgress(p, missionNumber, roomNumber);
			} else if (mission.mType == WeeklyMissionType.REGIONAL_CONTENT && 2 == mission.mRegion) {
				// Region matches up - award points
				SeasonalEventManager.addWeeklyMissionProgress(p, missionNumber, 1);
			} else if (mission.mType == WeeklyMissionType.CONTENT && mission.mContent != null && mission.mContent.contains(MonumentaContent.DEPTHS) && roomNumber >= 30) {
				// Content matches up - award points
				SeasonalEventManager.addWeeklyMissionProgress(p, missionNumber, 1);
			}
			missionNumber++;
		}
	}

	/**
	 * Called from DepthsParty, with rooms reached.
	 * Run through any potential depths missions and give credit
	 */
	public static void playerCompletedZenith(Player p, int roomNumber, int ascension) {
		int missionNumber = 1;
		for (WeeklyMission mission : SeasonalEventManager.getActiveMissions()) {
			if (mission.mType == WeeklyMissionType.ZENITH_ROOMS) {
				SeasonalEventManager.addWeeklyMissionProgress(p, missionNumber, roomNumber);
			} else if (mission.mType == WeeklyMissionType.REGIONAL_CONTENT && 3 == mission.mRegion) {
				// Region matches up - award points
				SeasonalEventManager.addWeeklyMissionProgress(p, missionNumber, 1);
			} else if (mission.mType == WeeklyMissionType.CONTENT && mission.mContent != null && mission.mContent.contains(MonumentaContent.ZENITH) && roomNumber >= 30) {
				// Content matches up - award points
				SeasonalEventManager.addWeeklyMissionProgress(p, missionNumber, 1);
			} else if (mission.mType == WeeklyMissionType.ZENITH_ASCENSION && ascension >= mission.mAmount) {
				SeasonalEventManager.addWeeklyMissionProgress(p, missionNumber, ascension);
			}
			missionNumber++;
		}
	}

	/**
	 * Called from the gallery game.
	 * Run through any potential gallery missions and give credit
	 */
	public static void playerGalleryWave(Player p) {
		//Check for weekly mission type of gallery rounds
		int missionNumber = 1;
		for (WeeklyMission mission : SeasonalEventManager.getActiveMissions()) {
			if (mission.mType == WeeklyMissionType.CONTENT && mission.mContent != null && mission.mContent.contains(MonumentaContent.GALLERY_ROUND)) {
				// Content matches up - award points
				SeasonalEventManager.addWeeklyMissionProgress(p, missionNumber, 1);
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
				if (mission.mType == WeeklyMissionType.CONTENT && mission.mContent != null && mission.mContent.contains(content)) {
					// Content matches up - award points
					SeasonalEventManager.addWeeklyMissionProgress(p, missionNumber, 1);
				} else if (mission.mType == WeeklyMissionType.DUNGEONS && content.getContentType() == ContentType.DUNGEON) {
					// Dungeon matches up - award points
					SeasonalEventManager.addWeeklyMissionProgress(p, missionNumber, 1);
				} else if (mission.mType == WeeklyMissionType.STRIKES && content.getContentType() == ContentType.STRIKE) {
					// Strike matches up - award points
					SeasonalEventManager.addWeeklyMissionProgress(p, missionNumber, 1);
				} else if (mission.mType == WeeklyMissionType.BOSSES && content.getContentType() == ContentType.BOSS) {
					// Boss matches up - award points
					SeasonalEventManager.addWeeklyMissionProgress(p, missionNumber, 1);
				} else if ((mission.mType == WeeklyMissionType.DELVE_MODIFIER || mission.mType == WeeklyMissionType.DELVE_POINTS) && DelvesUtils.SHARD_SCOREBOARD_PREFIX_MAPPINGS.containsKey(content.getLabel())) {
					// Content is eligible for delves - get scores and check for modifier
					if (mission.mType == WeeklyMissionType.DELVE_POINTS && DelvesUtils.getPlayerTotalDelvePoint(null, p, content.getLabel()) >= mission.mDelvePoints) {
						if (mission.mContent == null || mission.mContent.contains(content)) {
							SeasonalEventManager.addWeeklyMissionProgress(p, missionNumber, 1);
						}
					} else if (mission.mType == WeeklyMissionType.DELVE_MODIFIER) {
						boolean modsActive = true;
						List<DelvesModifier> modifiers = mission.mDelveModifiers;
						if (modifiers == null) {
							modifiers = new ArrayList<>();
						}
						for (DelvesModifier modifier : modifiers) {
							if (DelvesUtils.getDelveModLevel(p, content.getLabel(), modifier) < mission.mModifierRank) {
								modsActive = false;
							}
						}
						if (mission.mRotatingModifiersAmount > 0) {
							int rotatingPoints = 0;
							for (DelvesModifier rotating : DelvesModifier.rotatingDelveModifiers()) {
								rotatingPoints += DelvesUtils.getDelveModLevel(p, content.getLabel(), rotating);
							}
							modsActive = modsActive && (rotatingPoints >= mission.mRotatingModifiersAmount);
						}
						if ((mission.mContent == null || mission.mContent.contains(content)) && modsActive) {
							SeasonalEventManager.addWeeklyMissionProgress(p, missionNumber, 1);
						}
					}
				} else if (mission.mType == WeeklyMissionType.CHALLENGE_DELVE && DelvesUtils.SHARD_SCOREBOARD_PREFIX_MAPPINGS.containsKey(content.getLabel())) {
					if (DelvesManager.validateDelvePreset(p, content.getLabel())) {
						SeasonalEventManager.addWeeklyMissionProgress(p, missionNumber, 1);
					}
				} else if (mission.mType == WeeklyMissionType.REGIONAL_CONTENT && content.getRegion() == mission.mRegion) {
					// Region matches up - award points
					SeasonalEventManager.addWeeklyMissionProgress(p, missionNumber, 1);
				} else if (mission.mType == WeeklyMissionType.ROD_WAVES && content == MonumentaContent.RUSH) {
					// Cleared rod - add number of waves cleared
					int waves = ScoreboardUtils.getScoreboardValue(p.getName(), ROD_WAVE_SCOREBOARD);
					// Subtract the 20 wave checkpoint if player has certain tags
					Set<String> tags = p.getScoreboardTags();
					if (tags.contains("rod_checkpoint_start") || tags.contains("Primary") || tags.contains("Partner")) {
						waves -= 20;
					}

					if (waves > 0) {
						SeasonalEventManager.addWeeklyMissionProgress(p, missionNumber, waves);
					}
				} else if (mission.mType == WeeklyMissionType.DAILY_BOUNTY && (content == MonumentaContent.KINGS_BOUNTY || content == MonumentaContent.CELSIAN_BOUNTY || content == MonumentaContent.RING_BOUNTY)) {
					SeasonalEventManager.addWeeklyMissionProgress(p, missionNumber, 1);
				} else if (mission.mType == WeeklyMissionType.DELVE_BOUNTY && content == MonumentaContent.DELVE_BOUNTY) {
					SeasonalEventManager.addWeeklyMissionProgress(p, missionNumber, 1);
				}
				missionNumber++;
			}
		}
	}
}
