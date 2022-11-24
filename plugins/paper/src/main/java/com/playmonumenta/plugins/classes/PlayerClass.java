package com.playmonumenta.plugins.classes;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import javax.annotation.Nullable;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class PlayerClass {

	public ArrayList<AbilityInfo<?>> mAbilities = new ArrayList<>();
	public int mClass;
	public @Nullable String mClassName;
	public @Nullable NamedTextColor mClassColor;
	public @Nullable ChatColor mChatColor;
	public ItemStack mDisplayItem;
	public @Nullable String mQuestReq = null;
	public int mQuestReqMin;
	public @Nullable String mClassDescription;
	public @Nullable String mClassPassiveDescription;
	public @Nullable String mClassPassiveName;

	public PlayerSpec mSpecOne = new PlayerSpec();
	public PlayerSpec mSpecTwo = new PlayerSpec();

	public ImmutableList<AbilityInfo<?>> mTriggerOrder;

	public Boolean getClassAccessPerms(Player player) {
		return true;
	}

	public Boolean getSpecAccessToChoose(Player player, PlayerSpec spec) {
		int specQuestReq = ScoreboardUtils.getScoreboardValue(player, spec.mSpecQuestScoreboard).orElse(0);
		int specClassReq = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME).orElse(0);
		int specSpecReq = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_SPEC_NAME).orElse(0);
		return specQuestReq >= 100 && specClassReq == mClass && specSpecReq == 0;
	}

	public Boolean getSpecAccessToChange(Player player, PlayerSpec spec) {
		int specQuestReq = ScoreboardUtils.getScoreboardValue(player, spec.mSpecQuestScoreboard).orElse(0);
		int specClassReq = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME).orElse(0);
		int specSpecReq = ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_SPEC_NAME).orElse(0);
		return specQuestReq >= 100 && specClassReq == mClass && specSpecReq == spec.mSpecialization;
	}

	public JsonObject toJson() {
		JsonArray abilities = new JsonArray();
		for (AbilityInfo<?> ability : mAbilities) {
			if (ability != null) {
				abilities.add(ability.toJson());
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
