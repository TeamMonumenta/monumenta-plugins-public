package com.playmonumenta.plugins.depths;

import com.playmonumenta.plugins.delves.DelvesModifier;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class DepthsEndlessDifficulty {

	//Delve points to add per floor in endless mode
	//Each index is the floor to be assigned at the end of (ex. 3rd floor -> 4th floor is 10)
	public static final int[] DELVE_POINTS_PER_FLOOR = {0, 0, 10, 4, 4, 10, 4, 4, 10, 4, 4, 10, 4, 4};

	public static void applyDelvePointsToParty(List<DepthsPlayer> depthsPlayers, int pointsToAssign, Map<DelvesModifier, Integer> delvePointsForParty, boolean twisted) {
		//First, take an available player and assign points to them based on the party's current assignment
		Player playerToUse = null;
		for (DepthsPlayer dp : depthsPlayers) {
			Player p = Bukkit.getPlayer(dp.mPlayerId);
			if (p == null || !p.isOnline()) {
				continue;
			}
			playerToUse = p;
			p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "Assigning your party " + pointsToAssign + " delve points randomly. Sneak left click while holding your Depths Trinket to view all delve modifiers.");
			break;
		}

		if (playerToUse == null) {
			return;
		}

		String shard = ServerProperties.getShardName();
		//Get delve info for that player
		if (delvePointsForParty != null && delvePointsForParty.size() > 0) {

			for (DelvesModifier m : DelvesModifier.values()) {
				DelvesUtils.setDelvePoint(null, playerToUse, shard, m, delvePointsForParty.getOrDefault(m, 0));
			}
		}

		//Assign twisted and entropy if they are on the last floor
		if (twisted) {
			DelvesUtils.setDelvePoint(null, playerToUse, shard, DelvesModifier.ENTROPY, 5);
			DelvesUtils.setDelvePoint(null, playerToUse, shard, DelvesModifier.TWISTED, 1);
		} else {
			//Assign random points to that player on top of what they currently have
			DelvesUtils.assignRandomDelvePoints(playerToUse, shard, pointsToAssign);
		}

		//Store player's modifiers in the party index
		for (DelvesModifier m : DelvesModifier.values()) {
			delvePointsForParty.put(m, DelvesUtils.getDelveModLevel(playerToUse, shard, m));
		}

		//Assign the scores to all other active players
		for (DepthsPlayer dp : depthsPlayers) {
			Player p = Bukkit.getPlayer(dp.mPlayerId);
			if (p == null || !p.isOnline() || p.equals(playerToUse)) {
				continue;
			}

			if (pointsToAssign > 0) {
				p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "Assigning your party " + pointsToAssign + " delve points randomly. Sneak left click while holding your Depths Trinket to view all delve modifiers.");
			}

			DelvesUtils.copyDelvePoint(null, playerToUse, p, shard);
		}

	}
}
