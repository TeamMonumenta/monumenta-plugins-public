package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.bosses.bosses.AvengerBoss;
import com.playmonumenta.plugins.bosses.bosses.ToughBoss;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.entity.LivingEntity;

public class Vengeance {
	private static final int[] PERCENT_CHANCE = {
		3,
		6,
		9,
		12,
		15,
		18,
		21
	};

	public static final String DESCRIPTION = "Non elite/boss mobs become avengers.";

	public static final String[][] RANK_DESCRIPTIONS = {
		{
			"Non elite/boss enemies have a " + PERCENT_CHANCE[0] + "%",
			"to become avengers, giving them 100% more health",
			"and gaining health and damage when nearby enemies die."
		}, {
			"Non elite/boss enemies have a " + PERCENT_CHANCE[1] + "%",
			"to become avengers, giving them 100% more health",
			"and gaining health and damage when nearby enemies die."
		}, {
			"Non elite/boss enemies have a " + PERCENT_CHANCE[2] + "%",
			"to become avengers, giving them 100% more health",
			"and gaining health and damage when nearby enemies die."
		}, {
			"Non elite/boss enemies have a " + PERCENT_CHANCE[3] + "%",
			"to become avengers, giving them 100% more health",
			"and gaining health and damage when nearby enemies die."
		}, {
			"Non elite/boss enemies have a " + PERCENT_CHANCE[4] + "%",
			"to become avengers, giving them 100% more health",
			"and gaining health and damage when nearby enemies die."
		}, {
			"Non elite/boss enemies have a " + PERCENT_CHANCE[5] + "%",
			"to become avengers, giving them 100% more health",
			"and gaining health and damage when nearby enemies die."
		}, {
			"Non elite/boss enemies have a " + PERCENT_CHANCE[6] + "%",
			"to become avengers, giving them 100% more health",
			"and gaining health and damage when nearby enemies die."
		}
	};

	public static void applyModifiers(LivingEntity mob, int level) {
		if (!DelvesUtils.isDelveMob(mob) && !EntityUtils.isElite(mob) && !EntityUtils.isBoss(mob) && FastUtils.RANDOM.nextDouble() < PERCENT_CHANCE[level - 1] / 100.0) {
			mob.addScoreboardTag(AvengerBoss.identityTag);
			mob.addScoreboardTag(AvengerBoss.identityTag + "[maxstacks=4]");
			mob.addScoreboardTag(ToughBoss.identityTag);
		}
	}
}
