package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.delves.mobabilities.SpectralSummonBoss;
import org.bukkit.entity.LivingEntity;

public class Spectral {

	private static final double SPAWN_CHANCE_PER_LEVEL = 0.07;

	public static final String DESCRIPTION = "Dying enemies transform into new enemies.";

	public static String[] rankDescription(int level) {
		return new String[]{
			"Dying Enemies have a " + Math.round(SPAWN_CHANCE_PER_LEVEL * level * 100) + "% chance",
				"to spawn Specters."
		};
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		if (!DelvesUtils.isDelveMob(mob)) {
			mob.addScoreboardTag(SpectralSummonBoss.identityTag);
			mob.addScoreboardTag(SpectralSummonBoss.identityTag + "[spawnchance=" + SPAWN_CHANCE_PER_LEVEL * level + "]");
		}
	}

}
