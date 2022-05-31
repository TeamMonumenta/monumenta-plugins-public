package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.delves.mobabilities.DreadfulSummonBoss;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.LivingEntity;

public class Dreadful {

	private static final double[] SPAWN_CHANCE = {
			0.2,
			0.4,
			0.6,
			0.8,
			1.0
	};

	public static final String DESCRIPTION = "Dying elites transform into new enemies.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Dying Elites have a " + Math.round(SPAWN_CHANCE[0] * 100) + "% chance",
				"to spawn Dreadnaughts."
			}, {
				"Dying Elites have a " + Math.round(SPAWN_CHANCE[1] * 100) + "% chance",
				"to spawn Dreadnaughts."
			}, {
				"Dying Elites have a " + Math.round(SPAWN_CHANCE[2] * 100) + "% chance",
				"to spawn Dreadnaughts."
			}, {
				"Dying Elites have a " + Math.round(SPAWN_CHANCE[3] * 100) + "% chance",
				"to spawn Dreadnaughts."
			}, {
				"Dying Elites have a " + Math.round(SPAWN_CHANCE[4] * 100) + "% chance",
				"to spawn Dreadnaughts."
			}
	};

	public static void applyModifiers(LivingEntity mob, int level) {
		if (EntityUtils.isElite(mob) && !DelvesUtils.isDelveMob(mob)) {
			mob.addScoreboardTag(DreadfulSummonBoss.identityTag);
			mob.addScoreboardTag(DreadfulSummonBoss.identityTag + "[spawnchange=" + SPAWN_CHANCE[level - 1] + "]");
		}
	}

}
