package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.delves.mobabilities.SpectralSummonBoss;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;

public class Spectral {

	private static final double SPAWN_CHANCE_PER_LEVEL = 0.07;

	public static final String DESCRIPTION = "Dying enemies transform into new enemies.";

	public static Component[] rankDescription(int level) {
		return new Component[]{
			Component.text("Dying Enemies have a " + Math.round(SPAWN_CHANCE_PER_LEVEL * level * 100) + "% chance"),
				Component.text("to spawn Specters.")
		};
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		if (!DelvesUtils.isDelveMob(mob)) {
			mob.addScoreboardTag(SpectralSummonBoss.identityTag);
			mob.addScoreboardTag(SpectralSummonBoss.identityTag + "[spawnchance=" + SPAWN_CHANCE_PER_LEVEL * level + "]");
		}
	}

}
