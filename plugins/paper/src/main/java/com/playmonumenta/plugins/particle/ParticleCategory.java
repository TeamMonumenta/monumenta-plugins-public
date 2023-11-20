package com.playmonumenta.plugins.particle;

import com.playmonumenta.plugins.Constants;
import org.jetbrains.annotations.Nullable;

public enum ParticleCategory {

	OWN_PASSIVE(Constants.Objectives.PP_OWN_PASSIVE, "Own Passive Abilities"),
	OWN_ACTIVE(Constants.Objectives.PP_OWN_ACTIVE, "Own Active Abilities"),
	OWN_BUFF(Constants.Objectives.PP_OWN_BUFF, "(De)Buffs on yourself"),
	OWN_EMOJI(Constants.Objectives.PP_OWN_EMOJI, "Own Emojis"),
	OTHER_PASSIVE(Constants.Objectives.PP_OTHER_PASSIVE, "Others' Passive Abilities"),
	OTHER_ACTIVE(Constants.Objectives.PP_OTHER_ACTIVE, "Others' Active Abilities"),
	OTHER_BUFF(Constants.Objectives.PP_OTHER_BUFF, "(De)Buffs on other players"),
	OTHER_EMOJI(Constants.Objectives.PP_OTHER_EMOJI, "Others' Emojis"),
	ENEMY(Constants.Objectives.PP_ENEMY, "Other Enemies' Abilities"),
	ENEMY_BUFF(Constants.Objectives.PP_ENEMY_BUFF, "(De)Buffs on Enemies"),
	BOSS(Constants.Objectives.PP_BOSS, "Boss Abilities"),
	FULL(null, "");

	public final @Nullable String mObjectiveName;
	public final String mDisplayName;

	ParticleCategory(@Nullable String objectiveName, String displayName) {
		mObjectiveName = objectiveName;
		mDisplayName = displayName;
	}

}
