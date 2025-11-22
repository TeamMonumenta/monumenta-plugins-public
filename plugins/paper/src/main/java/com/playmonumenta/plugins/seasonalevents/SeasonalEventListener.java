package com.playmonumenta.plugins.seasonalevents;

import com.playmonumenta.plugins.delves.DelvesManager;
import com.playmonumenta.plugins.delves.DelvesModifier;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.events.MonumentaEvent;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class SeasonalEventListener implements Listener {

	public static final String ROD_WAVE_SCOREBOARD = "DRDFinished";

	// Handle all types of events that may award mission progress

	/**
	 * Block break event for tracking spawner based missions
	 */
	@EventHandler(ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		Material blockType = event.getBlock().getType();
		Player p = event.getPlayer();

		if (blockType == Material.SPAWNER) {
			// Loop through missions and update them if they apply
			for (Mission mission : SeasonalEventManager.getActiveMissions()) {
				if (mission.mType == MissionType.SPAWNERS) {
					SeasonalEventManager.addMissionProgress(p, mission, 1);
				} else if (mission.mType == MissionType.SPAWNERS_POI && PlayerUtils.playerIsInPOI(p)) {
					SeasonalEventManager.addMissionProgress(p, mission, 1);
				}
			}
		}
	}

	/**
	 * Called from DepthsParty, with rooms reached.
	 * Run through any potential depths missions and give credit
	 */
	public static void playerCompletedDepths(Player p, int roomNumber) {
		// Check for mission type of depths rooms
		for (Mission mission : SeasonalEventManager.getActiveMissions()) {
			if (mission.mType == MissionType.DEPTHS_ROOMS) {
				SeasonalEventManager.addMissionProgress(p, mission, roomNumber);
			} else if (mission.mType == MissionType.REGIONAL_CONTENT && 2 == mission.mRegion) {
				// Region matches up - award points
				SeasonalEventManager.addMissionProgress(p, mission, 1);
			} else if (mission.mType == MissionType.CONTENT && mission.mContent != null && mission.mContent.contains(MonumentaContent.DEPTHS) && roomNumber >= 30) {
				// Content matches up - award points
				SeasonalEventManager.addMissionProgress(p, mission, 1);
			}
		}
	}

	/**
	 * Called from DepthsParty, with rooms reached.
	 * Run through any potential depths missions and give credit
	 */
	public static void playerCompletedZenith(Player p, int roomNumber, int ascension) {
		for (Mission mission : SeasonalEventManager.getActiveMissions()) {
			if (mission.mType == MissionType.ZENITH_ROOMS) {
				SeasonalEventManager.addMissionProgress(p, mission, roomNumber);
			} else if (mission.mType == MissionType.REGIONAL_CONTENT && 3 == mission.mRegion && roomNumber >= 30) {
				// Region matches up - award points
				SeasonalEventManager.addMissionProgress(p, mission, 1);
			} else if (mission.mType == MissionType.CONTENT && mission.mContent != null && mission.mContent.contains(MonumentaContent.ZENITH) && roomNumber >= 30) {
				// Content matches up - award points
				SeasonalEventManager.addMissionProgress(p, mission, 1);
			} else if (mission.mType == MissionType.ZENITH_ASCENSION && roomNumber >= 30 && ascension >= mission.mAscension) {
				SeasonalEventManager.addMissionProgress(p, mission, 1);
			}
		}
	}

	/**
	 * Called from the gallery game.
	 * Run through any potential gallery missions and give credit
	 */
	public static void playerGalleryWave(Player p) {
		// Check for mission type of gallery rounds
		for (Mission mission : SeasonalEventManager.getActiveMissions()) {
			if (mission.mType == MissionType.CONTENT && mission.mContent != null && mission.mContent.contains(MonumentaContent.GALLERY_ROUND)) {
				// Content matches up - award points
				SeasonalEventManager.addMissionProgress(p, mission, 1);
			}
		}
	}

	/**
	 * Called from RushManager.
	 * Run through any potential rush mission and give credit.
	 */

	public static void playerRushRound(Player p) {
		for (Mission mission : SeasonalEventManager.getActiveMissions()) {
			if (mission.mType == MissionType.CONTENT && mission.mContent != null && mission.mContent.contains(MonumentaContent.RUSH_WAVE)) {
				SeasonalEventManager.addMissionProgress(p, mission, 1);
			}
		}
	}

	/**
	 * Main event handler for tracking Monumenta content completion.
	 * Runs through most mission types and checks for if
	 * credit should be awarded.
	 */
	@EventHandler(ignoreCancelled = true)
	public void playerCompletedContentEvent(MonumentaEvent event) {
		MonumentaContent content = MonumentaContent.getContentSelection(event.getEvent());
		Player p = event.getPlayer();
		if (p != null && content != null) {
			// Loop through missions and update them if they apply
			for (Mission mission : SeasonalEventManager.getActiveMissions()) {
				if (mission.mType == MissionType.CONTENT && mission.mContent != null && mission.mContent.contains(content)) {
					// Content matches up - award points
					SeasonalEventManager.addMissionProgress(p, mission, 1);
				} else if (mission.mType == MissionType.DUNGEONS && content.getContentType() == ContentType.DUNGEON) {
					// Dungeon matches up - award points
					SeasonalEventManager.addMissionProgress(p, mission, 1);
				} else if (mission.mType == MissionType.STRIKES && content.getContentType() == ContentType.STRIKE) {
					// Strike matches up - award points
					SeasonalEventManager.addMissionProgress(p, mission, 1);
				} else if (mission.mType == MissionType.BOSSES && content.getContentType() == ContentType.BOSS) {
					// Boss matches up - award points
					SeasonalEventManager.addMissionProgress(p, mission, 1);
				} else if ((mission.mType == MissionType.DELVE_MODIFIER || mission.mType == MissionType.DELVE_POINTS) && DelvesUtils.SHARD_SCOREBOARD_PREFIX_MAPPINGS.containsKey(content.getLabel())) {
					// Content is eligible for delves - get scores and check for modifier
					if (mission.mType == MissionType.DELVE_POINTS && DelvesUtils.getPlayerTotalDelvePoint(null, p, content.getLabel()) >= mission.mDelvePoints) {
						if (mission.mContent == null || mission.mContent.contains(content)) {
							SeasonalEventManager.addMissionProgress(p, mission, 1);
						}
					} else if (mission.mType == MissionType.DELVE_MODIFIER) {
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
							SeasonalEventManager.addMissionProgress(p, mission, 1);
						}
					}
				} else if (mission.mType == MissionType.CHALLENGE_DELVE && DelvesUtils.SHARD_SCOREBOARD_PREFIX_MAPPINGS.containsKey(content.getLabel())) {
					if (DelvesManager.validateDelvePreset(p, content.getLabel())) {
						if (mission.mContent == null || mission.mContent.contains(content)) {
							SeasonalEventManager.addMissionProgress(p, mission, 1);
						}
					}
				} else if (mission.mType == MissionType.REGIONAL_CONTENT && content.getRegion() == mission.mRegion) {
					// Region matches up - award points
					SeasonalEventManager.addMissionProgress(p, mission, 1);
				} else if (mission.mType == MissionType.DAILY_BOUNTY && (content == MonumentaContent.KINGS_BOUNTY || content == MonumentaContent.CELSIAN_BOUNTY || content == MonumentaContent.RING_BOUNTY)) {
					SeasonalEventManager.addMissionProgress(p, mission, 1);
				} else if (mission.mType == MissionType.DELVE_BOUNTY && content == MonumentaContent.DELVE_BOUNTY) {
					SeasonalEventManager.addMissionProgress(p, mission, 1);
				} else if (mission.mType == MissionType.POI_BIOME && mission.mContent != null && mission.mContent.contains(content)) {
					// POI matches up - award points
					SeasonalEventManager.addMissionProgress(p, mission, 1);
				}
			}
		}
	}
}
