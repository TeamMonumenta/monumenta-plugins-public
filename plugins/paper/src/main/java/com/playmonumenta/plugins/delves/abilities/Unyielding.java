package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.bosses.bosses.UnyieldingBoss;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.LivingEntity;

public class Unyielding {

	public static final String DESCRIPTION = "Elites find renewed strength at half health.";

	public static String[] rankDescription(int level) {
		return new String[]{
			"Elite enemies heal 120% maximum health over 3 seconds",
			"upon falling to 50% health and shed negative effects.",
			"The attack that triggers this cannot bring the elite",
			"to under 50% health. The healing can be cancelled through",
			"hard crowd control (knock away, pull, stun, silence, freeze)."
		};
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		if (level == 0) {
			return;
		}
		if (!DelvesUtils.isDelveMob(mob) && EntityUtils.isElite(mob)) {
			mob.addScoreboardTag(UnyieldingBoss.identityTag);
			mob.addScoreboardTag(UnyieldingBoss.identityTag + "[healing=0.04,durationticks=60,tickstoheal=2]");
		}
	}
}
