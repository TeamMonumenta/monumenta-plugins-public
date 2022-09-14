package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.bosses.bosses.WhispersBoss;
import com.playmonumenta.plugins.delves.DelvesUtils;
import org.bukkit.entity.LivingEntity;

public class Echoes {
	private static final double[] PERCENT_DAMAGE = {
		2.5,
		5,
		7.5,
		10,
		12.5,
		15,
		17.5
	};

	public static final String DESCRIPTION = "Enemies do a percentage of max health as true damage on hit.";

	public static final String[][] RANK_DESCRIPTIONS = {
		{
			"Enemies deal " + PERCENT_DAMAGE[0] + "% max health",
			"as true damage."
		}, {
			"Enemies deal " + PERCENT_DAMAGE[1] + "% max health",
			"as true damage."
		}, {
			"Enemies deal " + PERCENT_DAMAGE[2] + "% max health",
			"as true damage."
		}, {
			"Enemies deal " + PERCENT_DAMAGE[3] + "% max health",
			"as true damage."
		}, {
			"Enemies deal " + PERCENT_DAMAGE[4] + "% max health",
			"as true damage."
		}, {
			"Enemies deal " + PERCENT_DAMAGE[5] + "% max health",
			"as true damage."
		}, {
			"Enemies deal " + PERCENT_DAMAGE[6] + "% max health",
			"as true damage."
		}
	};

	public static void applyModifiers(LivingEntity mob, int level) {
		if (!DelvesUtils.isDelveMob(mob)) {
			mob.addScoreboardTag(WhispersBoss.identityTag);
			mob.addScoreboardTag(WhispersBoss.identityTag + "[percentdamage=" + PERCENT_DAMAGE[level - 1] + "]");
		}
	}
}
