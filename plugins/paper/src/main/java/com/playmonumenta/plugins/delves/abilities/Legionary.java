package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.entity.LivingEntity;

public class Legionary {

	private static final double[] SPAWN_CHANCE = {
			0.15,
			0.3,
			0.45,
			0.6,
			0.75,
			0.9,
			1.05
	};

	public static final String DESCRIPTION = "Enemies come in larger numbers.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Spawners have a " + Math.round(SPAWN_CHANCE[0] * 100) + "% chance",
				"to spawn a copy of an enemy."
			}, {
				"Spawners have a " + Math.round(SPAWN_CHANCE[1] * 100) + "% chance",
				"to spawn a copy of an enemy."
			}, {
				"Spawners have a " + Math.round(SPAWN_CHANCE[2] * 100) + "% chance",
				"to spawn a copy of an enemy."
			}, {
				"Spawners have a " + Math.round(SPAWN_CHANCE[3] * 100) + "% chance",
				"to spawn a copy of an enemy."
			}, {
				"Spawners have a " + Math.round(SPAWN_CHANCE[4] * 100) + "% chance",
				"to spawn a copy of an enemy."
			}, {
				"Spawners have a " + Math.round(SPAWN_CHANCE[5] * 100) + "% chance",
				"to spawn a copy of an enemy."
			}, {
				"Spawners have a " + Math.round(SPAWN_CHANCE[6] * 100) + "% chance",
				"to spawn a copy of an enemy."
			}
	};

	public static void applyModifiers(LivingEntity mob, int level) {
		if (FastUtils.RANDOM.nextDouble() < SPAWN_CHANCE[level - 1] && !DelvesUtils.isDelveMob(mob)) {
			DelvesUtils.duplicateLibraryOfSoulsMob(mob);
		}
		//Chance for a third if chance > 100
		if (FastUtils.RANDOM.nextDouble() < SPAWN_CHANCE[level - 1] - 1 && !DelvesUtils.isDelveMob(mob)) {
			DelvesUtils.duplicateLibraryOfSoulsMob(mob);
		}
	}

}
