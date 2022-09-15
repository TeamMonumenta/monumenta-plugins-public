package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.bosses.bosses.UnyieldingBoss;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.LivingEntity;

public class Unyielding {

	public static final String DESCRIPTION = "Elites become invincible at 50% health.";

	public static final String[][] RANK_DESCRIPTIONS = {
		{
			"Elite enemies gain invulnerability for 2 seconds",
			"upon falling to 50% health. When this occurs, they shed",
			"negative effects and gain 30% damage and 10% speed",
			"for the rest of combat."
		}
	};

	public static void applyModifiers(LivingEntity mob, int level) {
		if (level == 0) {
			return;
		}
		if (!DelvesUtils.isDelveMob(mob) && EntityUtils.isElite(mob)) {
			mob.addScoreboardTag(UnyieldingBoss.identityTag);
			mob.addScoreboardTag(UnyieldingBoss.identityTag + "[damageincrease=0.3,speedincrease=0.1]");
		}
	}
}
