package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.bosses.bosses.CoordinatedAttackBoss;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.entity.LivingEntity;

public class Bloodthirsty {

	private static final double BLOODTHIRSTY_CHANCE_PER_LEVEL = 0.07;

	public static final String DESCRIPTION = "Enemies can coordinate attacks on players.";

	public static String[] rankDescription(int level) {
			return new String[] {
				"Enemies have a " + Math.round(100 * BLOODTHIRSTY_CHANCE_PER_LEVEL * level) + "% chance to be Bloodthirsty."
			};
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		if (FastUtils.RANDOM.nextDouble() < BLOODTHIRSTY_CHANCE_PER_LEVEL * level && !DelvesUtils.isDelveMob(mob)) {
			// This runs prior to BossManager parsing, so we can just add tags directly
			mob.addScoreboardTag(CoordinatedAttackBoss.identityTag);
		}
	}

}
