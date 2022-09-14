package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.BlockBreakBoss;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Vex;

public class Pernicious {

	private static final double[] BLOCK_BREAK_CHANCE = {
			0.1,
			0.2,
			0.3,
			0.4,
			0.5,
			0.6
	};


	public static final String DESCRIPTION = "Enemies can destroy terrain.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Enemies have a " + Math.round(BLOCK_BREAK_CHANCE[0] * 100) + "% chance to have Block Break",
				"and gain 7.5% movement speed."
			}, {
				"Enemies have a " + Math.round(BLOCK_BREAK_CHANCE[1] * 100) + "% chance to have Block Break",
				"and gain 7.5% movement speed."
			}, {
				"Enemies have a " + Math.round(BLOCK_BREAK_CHANCE[2] * 100) + "% chance to have Block Break",
				"and gain 7.5% movement speed."
			}, {
				"Enemies have a " + Math.round(BLOCK_BREAK_CHANCE[3] * 100) + "% chance to have Block Break",
				"and gain 7.5% movement speed."
			}, {
				"Enemies have a " + Math.round(BLOCK_BREAK_CHANCE[4] * 100) + "% chance to have Block Break",
				"and gain 7.5% movement speed."
			}, {
				"Enemies have a " + Math.round(BLOCK_BREAK_CHANCE[5] * 100) + "% chance to have Block Break",
				"and gain 7.5% movement speed."
			}
	};


	public static void applyModifiers(LivingEntity mob, int level) {
		if (!(mob instanceof Vex) && FastUtils.RANDOM.nextDouble() < BLOCK_BREAK_CHANCE[level - 1] && !DelvesUtils.isDelveMob(mob)) {
			mob.addScoreboardTag(BlockBreakBoss.identityTag);
			Plugin.getInstance().mEffectManager.addEffect(mob, "PerniciousSpeedEffect", new PercentSpeed(999999999, .075, "PerniciousSpeedEffect"));
			mob.addScoreboardTag(BlockBreakBoss.identityTag + "[adapttoboundingbox=true]");
		}
	}
}
