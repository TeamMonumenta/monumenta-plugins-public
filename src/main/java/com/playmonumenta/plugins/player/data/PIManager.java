package com.playmonumenta.plugins.player.data;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin.Classes;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityCollection;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.specializations.ClassSpecialization;
import com.playmonumenta.plugins.utils.ScoreboardUtils;


public class PIManager {

	private List<PlayerInfo> data = new ArrayList<PlayerInfo>();

	private static PIManager manager = new PIManager();

	public PIManager() { }

	public static PIManager getManager() { return manager; }

	public PlayerInfo initializePlayerInfo(Player player) {
		if (!hasPlayerInfo(player)) {
			PlayerInfo pInfo = updatePlayerInfo(player);
			data.add(pInfo);
			return pInfo;
		} else {
			return updatePlayerInfo(player);
		}
	}

	public PlayerInfo updatePlayerInfo(Player player) {
		PlayerInfo pInfo;
		if (!hasPlayerInfo(player)) {
			pInfo = new PlayerInfo(player);
		} else {
			pInfo = getPlayerInfo(player);
		}

		if (pInfo != null) {
			pInfo.classId = ScoreboardUtils.getScoreboardValue(player, "Class");;
			pInfo.specId = ScoreboardUtils.getScoreboardValue(player, "Specialization");

			/*
			 * Checks if the player currently has an ability collection.
			 * If not, get all abilities and check their scores.
			 * Add abilities depending on scores and class & spec ids.
			 */
			AbilityCollection collection;
			if (pInfo.abilities == null) {
				collection = new AbilityCollection(player);
				collection.refreshAbilities(pInfo);
				pInfo.abilities = collection;
			} else {
				collection = pInfo.abilities;
				collection.refreshAbilities(pInfo);
			}
		}
		return pInfo;
	}

	public boolean hasPlayerInfo(Player player) {
		for (PlayerInfo da : data) {
			if (da.getPlayer().equals(player))
				return true;
		}
		return false;
	}

	public PlayerInfo getPlayerInfo(Player player) {
		PlayerInfo info = null;
		for (PlayerInfo da : data) {
			if (da.getPlayer().equals(player)) {
				info = da;
				break;
			}
		}
		if (info == null)
			info = initializePlayerInfo(player);
		return info;
	}

}
