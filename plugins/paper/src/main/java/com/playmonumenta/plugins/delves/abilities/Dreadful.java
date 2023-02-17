package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.delves.mobabilities.DreadfulSummonBoss;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.LivingEntity;

public class Dreadful {

	private static final double SPAWN_CHANCE_PER_LEVEL = 0.2;

	public static final String DESCRIPTION = "Dying elites transform into new enemies.";

	public static String[] rankDescription(int level) {
			return new String[]{
				"Dying Elites have a " + Math.round(SPAWN_CHANCE_PER_LEVEL * level * 100) + "% chance",
				"to spawn Dreadnaughts."
			};
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		if (EntityUtils.isElite(mob) && !DelvesUtils.isDelveMob(mob)) {
			mob.addScoreboardTag(DreadfulSummonBoss.identityTag);
			mob.addScoreboardTag(DreadfulSummonBoss.identityTag + "[spawnchance=" + SPAWN_CHANCE_PER_LEVEL * level + "]");
		}
	}

}
