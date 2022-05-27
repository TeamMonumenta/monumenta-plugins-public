package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.bosses.bosses.CoordinatedAttackBoss;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.entity.LivingEntity;

public class Bloodthirsty {

	private static final double[] BLOODTHIRSTY_CHANCE = {
			0.07,
			0.14,
			0.21,
			0.28,
			0.35
	};

	public static final String DESCRIPTION = "Enemies can coordinate attacks on players.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Enemies have a " + Math.round(BLOODTHIRSTY_CHANCE[0] * 100) + "% chance to be Bloodthirsty."
			}, {
				"Enemies have a " + Math.round(BLOODTHIRSTY_CHANCE[1] * 100) + "% chance to be Bloodthirsty."
			}, {
				"Enemies have a " + Math.round(BLOODTHIRSTY_CHANCE[2] * 100) + "% chance to be Bloodthirsty."
			}, {
				"Enemies have a " + Math.round(BLOODTHIRSTY_CHANCE[3] * 100) + "% chance to be Bloodthirsty."
			}, {
				"Enemies have a " + Math.round(BLOODTHIRSTY_CHANCE[4] * 100) + "% chance to be Bloodthirsty."
			}
	};

	public static void applyModifiers(LivingEntity mob, int level) {
		if (FastUtils.RANDOM.nextDouble() < BLOODTHIRSTY_CHANCE[level - 1] && !DelvesUtils.isDelveMob(mob)) {
			// This runs prior to BossManager parsing, so we can just add tags directly
			mob.addScoreboardTag(CoordinatedAttackBoss.identityTag);
		}
	}

}
