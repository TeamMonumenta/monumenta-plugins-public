package com.playmonumenta.plugins.depths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.DelveInfo;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.FastUtils;

public class DepthsEndlessDifficulty {

	//Delve points to add per floor in endless mode
	//Each index is the floor to be assigned at the end of (ex. 3rd floor -> 4th floor is 10)
	public static final int[] DELVE_POINTS_PER_FLOOR = {0, 0, 10, 4, 4, 10, 4, 4, 10, 4, 4, 10, 4, 4};

	public static void applyDelvePointsToParty(List<DepthsPlayer> depthsPlayers, int pointsToAssign, Map<Modifier, Integer> delvePointsForParty, boolean twisted) {
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

		//Get delve info for that player
		DelveInfo info = DelvesUtils.getDelveInfo(playerToUse);
		if (delvePointsForParty != null && delvePointsForParty.size() > 0) {

			for (Modifier m : Modifier.values()) {
				if (delvePointsForParty.get(m) != null) {
					info.setRank(m, delvePointsForParty.get(m).intValue());
				} else {
					info.setRank(m, 0);
				}
			}
		}

		//Assign twisted and entropy if they are on the last floor
		if (twisted) {
			info.setRank(Modifier.TWISTED, 1);
			info.setRank(Modifier.ENTROPY, 5);
			info.storeDelveScore();
		} else {
			//Assign random points to that player on top of what they currently have
			assignRandomDelvePoints(info, pointsToAssign);
		}

		//Store player's modifiers in the party index
		for (Modifier m : Modifier.values()) {
			delvePointsForParty.put(m, info.getRank(m));
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

			DelveInfo playerInfo = DelvesUtils.getDelveInfo(p);
			for (Modifier m : Modifier.values()) {
				if (delvePointsForParty.get(m) != null) {
					playerInfo.setRank(m, delvePointsForParty.get(m).intValue());
				} else {
					playerInfo.setRank(m, 0);
				}
				playerInfo.storeDelveScore();
			}
		}

		//Refresh their class in a few ticks so delve modifiers apply
		for (DepthsPlayer dp : depthsPlayers) {
			Player p = Bukkit.getPlayer(dp.mPlayerId);
			if (p != null && p.isOnline()) {
				AbilityManager.getManager().updatePlayerAbilities(p);
			}
		}
	}

	public static void assignRandomDelvePoints(DelveInfo info, int points) {

		// Mostly copied from entropy assignment, gives random available delve points to the player before
		// distributing them to the rest of the party

		int pointsToAssign = points;

		List<Modifier> modifiers = new ArrayList<>(Arrays.asList(Modifier.values()));
		modifiers.remove(Modifier.ENTROPY);
		modifiers.remove(Modifier.TWISTED);

		while (pointsToAssign > 0 && modifiers.size() > 0) {
			int index = FastUtils.RANDOM.nextInt(modifiers.size());
			Modifier modifier = modifiers.get(index);

			int rank = info.getRank(modifier);
			if (rank < DelveInfo.getRankCap(modifier)) {
				info.setRank(modifier, rank + 1);
				pointsToAssign--;
			} else {
				modifiers.remove(index);
			}
		}
		info.storeDelveScore();
	}
}
