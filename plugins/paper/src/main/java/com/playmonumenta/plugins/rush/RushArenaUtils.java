package com.playmonumenta.plugins.rush;

import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.List;
import java.util.NoSuchElementException;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class RushArenaUtils {

	protected static final String RUSH_SPAWNER_TAG = "RushDown";
	protected static final String RUSH_CENTER_TAG = "RushDownCenter";
	protected static final String RUSH_SPAWN_TAG = "RushDownSpawn";
	protected static final String RUSH_STRUCTURE_TAG = "RushDownStructure";

	protected static final int RUSH_RADIUS = 32;

	protected static final int SEASON_PARTICLE_COUNT = 50;

	// TODO: Would this make sense in RushManager?
	public static ArmorStand getStandOrThrow(Player player, String tag) {
		return getStandOrThrow(RushManager.retrieveArmorStandList(player.getLocation()), tag);
	}

	public static ArmorStand getStandOrThrow(List<ArmorStand> list, String tag) {
		return list.stream()
			.filter(e -> e.getScoreboardTags().contains(tag))
			.findFirst()
			.orElseGet(() -> {
				RushManager.printDebugMessage(
					"Unable to find marker stand with tag: " + tag, list.get(0).getWorld().getPlayers().get(0));
				throw new NoSuchElementException("No ArmorStand found with tag: " + tag);
			});
	}

	public static Location getLocOrThrow(List<ArmorStand> list, String tag) {
		return getStandOrThrow(list, tag).getLocation();
	}

	protected static int retrieveRound(ArmorStand stand) {
		PersistentDataContainer data = stand.getPersistentDataContainer();
		Integer round = data.get(RushManager.RUSH_WAVE_KEY, PersistentDataType.INTEGER);
		if (round == null) {
			data.set(RushManager.RUSH_WAVE_KEY, PersistentDataType.INTEGER, 1);
			round = 1;
		}
		return round;
	}

	// Player count should be initialized on creation (Ie 3 player instance, even if soloing is scaled for 3)
	protected static int retrievePlayerCount(ArmorStand stand, Player fallbackPlayer) {
		PersistentDataContainer data = stand.getPersistentDataContainer();
		Integer playerCount = data.get(RushManager.RUSH_PLAYER_COUNT_KEY, PersistentDataType.INTEGER);
		Boolean isMultiplayer = data.get(RushManager.RUSH_IS_MULTIPLAYER, PersistentDataType.BOOLEAN);
		// Retrieve player count & store it onto the marker stand
		if (playerCount == null) {
			int count = ScoreboardUtils.getScoreboardValue(fallbackPlayer, "RushPlayercount")
				.orElse(PlayerUtils.playersInRange(fallbackPlayer.getLocation(), RUSH_RADIUS, true).size());
			data.set(RushManager.RUSH_PLAYER_COUNT_KEY, PersistentDataType.INTEGER, count);
			playerCount = count;
		}
		if(isMultiplayer == null) {
			isMultiplayer = playerCount > 1;
			data.set(RushManager.RUSH_IS_MULTIPLAYER, PersistentDataType.BOOLEAN, isMultiplayer);
		}

		return playerCount;
	}

	protected static int updatePlayerCount(ArmorStand stand) {
		PersistentDataContainer data = stand.getPersistentDataContainer();
		Integer count = data.get(RushManager.RUSH_PLAYER_COUNT_KEY, PersistentDataType.INTEGER);
		if (count == null) {
			return stand.getWorld().getPlayerCount() - 1;
		}
		int newCount = Math.max(1, count - 1);
		data.set(RushManager.RUSH_PLAYER_COUNT_KEY, PersistentDataType.INTEGER, newCount);
		return newCount;
	}
}
