package com.playmonumenta.plugins.classes;

import java.util.ArrayList;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;

public class PlayerClass {
	
	public ArrayList<Ability> mAbilities = new ArrayList<Ability>();
	public int mClass;

	PlayerSpec mSpecOne = new PlayerSpec();
	PlayerSpec mSpecTwo = new PlayerSpec();
	
	public Boolean getClassAccessPerms(Player player) {
		return true;
	}

	public Boolean getSpecAccessToChoose(Player player, PlayerSpec spec) {
		int specQuestReq = ScoreboardUtils.getScoreboardValue(player, spec.mSpecQuestScoreboard);
		int specClassReq = ScoreboardUtils.getScoreboardValue(player, "Class");
		int specSpecReq = ScoreboardUtils.getScoreboardValue(player, "Specialization");
		if (specQuestReq >= 100 && specClassReq == mClass && specSpecReq == 0) {
			return true;
		}
		return false;
	}

	public Boolean getSpecAccessToChange(Player player, PlayerSpec spec) {
		int specQuestReq = ScoreboardUtils.getScoreboardValue(player, spec.mSpecQuestScoreboard);
		int specClassReq = ScoreboardUtils.getScoreboardValue(player, "Class");
		int specSpecReq = ScoreboardUtils.getScoreboardValue(player, "Specialization");
		if (specQuestReq >= 100 && specClassReq == mClass && specSpecReq == spec.mSpecialization) {
			return true;
		}
		return false;
	}
}
