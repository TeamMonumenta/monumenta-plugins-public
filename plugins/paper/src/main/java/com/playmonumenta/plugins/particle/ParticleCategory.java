package com.playmonumenta.plugins.particle;

import com.playmonumenta.plugins.Constants;
import javax.annotation.Nullable;

public enum ParticleCategory {

	OWN_PASSIVE(Constants.Objectives.PP_OWN_PASSIVE),
	OWN_ACTIVE(Constants.Objectives.PP_OWN_ACTIVE),
	OWN_BUFF(Constants.Objectives.PP_OWN_BUFF),
	OTHER_PASSIVE(Constants.Objectives.PP_OTHER_PASSIVE),
	OTHER_ACTIVE(Constants.Objectives.PP_OTHER_ACTIVE),
	OTHER_BUFF(Constants.Objectives.PP_OTHER_BUFF),
	ENEMY(Constants.Objectives.PP_ENEMY),
	ENEMY_BUFF(Constants.Objectives.PP_ENEMY_BUFF),
	BOSS(Constants.Objectives.PP_BOSS),
	FULL(null);

	public final @Nullable String mObjectiveName;

	ParticleCategory(@Nullable String objectiveName) {
		mObjectiveName = objectiveName;
	}

}
