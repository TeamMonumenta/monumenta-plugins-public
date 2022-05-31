package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.bosses.bosses.DistanceCloserBoss;
import com.playmonumenta.plugins.delves.DelvesUtils;
import org.bukkit.entity.LivingEntity;

public class Relentless {

	private static final int DISTANCE_CLOSER_DISTANCE = 8;

	private static final int[] DISTANCE_CLOSER_SPEED_RAW_PERCENT = {
			10,
			20,
			30,
			40,
			50
	};

	public static final String DESCRIPTION = "Enemies are harder to stop.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Enemies are " + DISTANCE_CLOSER_SPEED_RAW_PERCENT[0] + "% faster when more than " + DISTANCE_CLOSER_DISTANCE + " blocks away."
			}, {
				"Enemies are " + DISTANCE_CLOSER_SPEED_RAW_PERCENT[1] + "% faster when more than " + DISTANCE_CLOSER_DISTANCE + " blocks away."
			}, {
				"Enemies are " + DISTANCE_CLOSER_SPEED_RAW_PERCENT[2] + "% faster when more than " + DISTANCE_CLOSER_DISTANCE + " blocks away."
			}, {
				"Enemies are " + DISTANCE_CLOSER_SPEED_RAW_PERCENT[3] + "% faster when more than " + DISTANCE_CLOSER_DISTANCE + " blocks away."
			}, {
				"Enemies are " + DISTANCE_CLOSER_SPEED_RAW_PERCENT[4] + "% faster when more than " + DISTANCE_CLOSER_DISTANCE + " blocks away."
			}
	};


	public static void applyModifiers(LivingEntity mob, int level) {
		if (!DelvesUtils.isDelveMob(mob)) {
			mob.addScoreboardTag(DistanceCloserBoss.identityTag);
			mob.addScoreboardTag(DistanceCloserBoss.identityTag + DISTANCE_CLOSER_DISTANCE + "," + DISTANCE_CLOSER_SPEED_RAW_PERCENT[level - 1]);
		}
	}

}
