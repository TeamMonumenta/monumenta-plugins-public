package com.playmonumenta.plugins.depths;

import com.playmonumenta.plugins.delves.DelvesModifier;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class DepthsEndlessDifficulty {

	//Delve points to add per floor in endless mode
	//Each index is the floor to be assigned at the end of (ex. 3rd floor -> 4th floor is 10)
	public static final int[] DELVE_POINTS_PER_FLOOR = {0, 0, 10, 4, 4, 10, 4, 4, 10, 4, 4, 10, 4, 4};

	//Ascension information for Depths 2
	public static final int[] ASCENSION_DELVE_POINTS = {1, 3, 5, 7, 9, 11};
	public static final int ASCENSION_DELVE_POINTS_AMOUNT = 10;
	public static final int ASCENSION_UTILITY_ROOMS = 2;
	public static final int ASCENSION_BOSS_TRICKS = 4;
	public static final int ASCENSION_STARTING_RARITY = 6;
	public static final int ASCENSION_STARTING_RARITY_AMOUNT = 15; //15% reduced ability odds
	public static final int ASCENSION_BOSS_COOLDOWN = 8;
	public static final int ASCENSION_ABILITY_PURGE = 10;
	public static final int ASCENSION_REVIVE_TIME = 12;
	public static final int ASCENSION_ACTIVE_TREE_CAP = 14;
	public static final int ASCENSION_FINAL_BOSS = 15;

	public static void applyDelvePointsToParty(DepthsParty party, int pointsToAssign, Map<DelvesModifier, Integer> delvePointsForParty, boolean twisted) {
		//First, take an available player and assign points to them based on the party's current assignment
		List<Player> players = new ArrayList<>();
		party.mPlayersInParty.forEach(dp -> players.add(Bukkit.getPlayer(dp.mPlayerId)));
		players.removeIf(p -> p == null || !p.isOnline());
		Player playerToUse = players.stream().findAny().orElse(null);
		if (playerToUse == null) {
			return;
		}
		players.remove(playerToUse);

		String shard = ServerProperties.getShardName();
		//Get delve info for that player
		if (delvePointsForParty != null && !delvePointsForParty.isEmpty()) {

			for (DelvesModifier m : DelvesModifier.values()) {
				DelvesUtils.setDelvePoint(null, playerToUse, shard, m, delvePointsForParty.getOrDefault(m, 0));
			}
		}

		//TODO make twisted and entropy, etc. simply not appear in depths and depths2 shards
		//Assign twisted and entropy if they are on the last floor
		if (twisted) {
			DelvesUtils.setDelvePoint(null, playerToUse, shard, DelvesModifier.TWISTED, 5);
		} else {
			//Assign random points to that player on top of what they currently have
			DelvesUtils.assignRandomDelvePoints(playerToUse, shard, pointsToAssign);
		}

		//Store player's modifiers in the party index
		for (DelvesModifier m : DelvesModifier.values()) {
			delvePointsForParty.put(m, DelvesUtils.getDelveModLevel(playerToUse, shard, m));
		}

		//Assign the scores to all other active players
		players.forEach(p -> DelvesUtils.copyDelvePoint(null, playerToUse, p, shard));
		if (!twisted) {
			party.sendMessage("Assigning your party " + pointsToAssign + " Delve Points randomly. Sneak left click while holding your Depths trinket to view all delve modifiers.");
		}
	}
}
