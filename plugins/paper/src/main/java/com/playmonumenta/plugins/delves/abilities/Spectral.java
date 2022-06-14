package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.delves.mobabilities.SpectralSummonBoss;
import org.bukkit.entity.LivingEntity;

public class Spectral {

	private static final double[] SPAWN_CHANCE = {
			0.07,
			0.14,
			0.21,
			0.28,
			0.35,
			0.42,
			0.49
	};

	public static final String DESCRIPTION = "Dying enemies transform into new enemies.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Dying Enemies have a " + Math.round(SPAWN_CHANCE[0] * 100) + "% chance",
				"to spawn Specters."
			}, {
				"Dying Enemies have a " + Math.round(SPAWN_CHANCE[1] * 100) + "% chance",
				"to spawn Specters."
			}, {
				"Dying Enemies have a " + Math.round(SPAWN_CHANCE[2] * 100) + "% chance",
				"to spawn Specters."
			}, {
				"Dying Enemies have a " + Math.round(SPAWN_CHANCE[3] * 100) + "% chance",
				"to spawn Specters."
			}, {
				"Dying Enemies have a " + Math.round(SPAWN_CHANCE[4] * 100) + "% chance",
				"to spawn Specters."
			}, {
				"Dying Enemies have a " + Math.round(SPAWN_CHANCE[5] * 100) + "% chance",
				"to spawn Specters."
			}, {
				"Dying Enemies have a " + Math.round(SPAWN_CHANCE[6] * 100) + "% chance",
				"to spawn Specters."
			}
	};

	public static void applyModifiers(LivingEntity mob, int level) {
		if (!DelvesUtils.isDelveMob(mob)) {
			mob.addScoreboardTag(SpectralSummonBoss.identityTag);
			mob.addScoreboardTag(SpectralSummonBoss.identityTag + "[spawnchance=" + SPAWN_CHANCE[level - 1] + "]");
		}
	}

}
