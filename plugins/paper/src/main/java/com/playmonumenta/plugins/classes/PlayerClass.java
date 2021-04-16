package com.playmonumenta.plugins.classes;

import java.util.ArrayList;

import org.bukkit.entity.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;

public class PlayerClass {

	public ArrayList<Ability> mAbilities = new ArrayList<Ability>();
	public int mClass;
	public String mClassName;

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

	public JsonObject toJson() {
		JsonArray abilities = new JsonArray();
		for (Ability ability : mAbilities) {
			if (ability != null) {
				abilities.add(ability.getInfo().toJson());
			}
		}

		JsonArray specs = new JsonArray();
		specs.add(mSpecOne.toJson());
		specs.add(mSpecTwo.toJson());

		JsonObject info = new JsonObject();
		info.addProperty("classId", mClass);
		info.addProperty("className", mClassName);
		info.add("skills", abilities);
		info.add("specs", specs);
		return info;
	}
}
