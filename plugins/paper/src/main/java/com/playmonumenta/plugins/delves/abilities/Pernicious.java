package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.BlockBreakBoss;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Vex;

public class Pernicious {

	private static final double BLOCK_BREAK_CHANCE_PER_LEVEL = 0.1;


	public static final String DESCRIPTION = "Enemies can destroy terrain.";

	public static String[] rankDescription(int level) {
			return new String[]{
				"Enemies have a " + Math.round(BLOCK_BREAK_CHANCE_PER_LEVEL * level * 100) + "% chance to have Block Break."
			};
	}


	public static void applyModifiers(LivingEntity mob, int level) {
		if (!(mob instanceof Vex) && FastUtils.RANDOM.nextDouble() < BLOCK_BREAK_CHANCE_PER_LEVEL * level && !DelvesUtils.isDelveMob(mob)) {
			mob.addScoreboardTag(BlockBreakBoss.identityTag);
			mob.addScoreboardTag(BlockBreakBoss.identityTag + "[adapttoboundingbox=true]");
		}
	}
}
