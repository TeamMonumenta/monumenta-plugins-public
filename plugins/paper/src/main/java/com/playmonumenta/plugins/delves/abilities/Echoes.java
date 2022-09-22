package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.bosses.bosses.WhispersBoss;
import com.playmonumenta.plugins.delves.DelvesUtils;
import org.bukkit.entity.LivingEntity;

public class Echoes {
	private static final double[] PERCENT_DAMAGE = {
		4,
		8,
		12,
		16,
		20,
		24,
		28
	};

	public static final String DESCRIPTION = "Enemies steal a percentage of max health on their first hit.";

	public static final String[][] RANK_DESCRIPTIONS = {
		{
			"The first instance of damage a mob deals to a player,",
			"shrinks them, stealing " + PERCENT_DAMAGE[0] + "% max health",
			"for 30 seconds. This debuff can be cleansed by killing the mob",
			"(which also heals back the lost hearts)."
		}, {
			"The first instance of damage a mob deals to a player,",
			"shrinks them, stealing " + PERCENT_DAMAGE[1] + "% max health",
			"for 30 seconds. This debuff can be cleansed by killing the mob",
			"(which also heals back the lost hearts)."
		}, {
			"The first instance of damage a mob deals to a player,",
			"shrinks them, stealing " + PERCENT_DAMAGE[2] + "% max health",
			"for 30 seconds. This debuff can be cleansed by killing the mob",
			"(which also heals back the lost hearts)."
		}, {
			"The first instance of damage a mob deals to a player,",
			"shrinks them, stealing " + PERCENT_DAMAGE[3] + "% max health",
			"for 30 seconds. This debuff can be cleansed by killing the mob",
			"(which also heals back the lost hearts)."
		}, {
			"The first instance of damage a mob deals to a player,",
			"shrinks them, stealing " + PERCENT_DAMAGE[4] + "% max health",
			"for 30 seconds. This debuff can be cleansed by killing the mob",
			"(which also heals back the lost hearts)."
		}, {
			"The first instance of damage a mob deals to a player,",
			"shrinks them, stealing " + PERCENT_DAMAGE[5] + "% max health",
			"for 30 seconds. This debuff can be cleansed by killing the mob",
			"(which also heals back the lost hearts)."
		}, {
			"The first instance of damage a mob deals to a player,",
			"shrinks them, stealing " + PERCENT_DAMAGE[6] + "% max health",
			"for 30 seconds. This debuff can be cleansed by killing the mob",
			"(which also heals back the lost hearts)."
		}
	};

	public static void applyModifiers(LivingEntity mob, int level) {
		if (!DelvesUtils.isDelveMob(mob)) {
			mob.addScoreboardTag(WhispersBoss.identityTag);
			mob.addScoreboardTag(WhispersBoss.identityTag + "[percentdamage=" + PERCENT_DAMAGE[level - 1] + "]");
		}
	}
}
