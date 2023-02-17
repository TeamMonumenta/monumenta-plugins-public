package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.bosses.bosses.AvengerBoss;
import com.playmonumenta.plugins.bosses.bosses.ToughBoss;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.entity.LivingEntity;

public class Vengeance {
	private static final int PERCENT_CHANCE_PER_LEVEL = 3;

	public static final String DESCRIPTION = "Non elite/boss mobs become avengers.";

	public static String[] rankDescription(int level) {
		return new String[]{
			"Non elite/boss enemies have a " + PERCENT_CHANCE_PER_LEVEL * level + "% chance",
			"to become avengers, giving them 100% more health",
			"and gaining health and damage when nearby enemies die."
		};
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		if (!DelvesUtils.isDelveMob(mob) && !EntityUtils.isElite(mob) && !EntityUtils.isBoss(mob) && FastUtils.RANDOM.nextDouble() < PERCENT_CHANCE_PER_LEVEL * level / 100.0) {
			mob.addScoreboardTag(AvengerBoss.identityTag);
			mob.addScoreboardTag(AvengerBoss.identityTag + "[maxstacks=12,damagepercentincrement=0.075,speedpercentincrement=0]");
			mob.addScoreboardTag(ToughBoss.identityTag);
		}
	}
}
