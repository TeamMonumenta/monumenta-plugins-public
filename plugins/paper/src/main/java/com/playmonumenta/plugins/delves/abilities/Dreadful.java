package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.delves.mobabilities.DreadfulSummonBoss;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.LivingEntity;

public class Dreadful {

	private static final double SPAWN_CHANCE_PER_LEVEL = 0.12;

	public static final String DESCRIPTION = "Dying elites transform into new enemies.";
	public static final String AVOID_DREADFUL = "boss_dreadfulimmune";

	public static Component[] rankDescription(int level) {
		return new Component[]{
			Component.text("Dying Elites have a " + Math.round(SPAWN_CHANCE_PER_LEVEL * level * 100) + "% chance"),
			Component.text("to spawn Dreadnaughts.")
		};
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		if (EntityUtils.isElite(mob) && !DelvesUtils.isDelveMob(mob) && !mob.getScoreboardTags().contains(AVOID_DREADFUL)) {
			mob.addScoreboardTag(DreadfulSummonBoss.identityTag);
			mob.addScoreboardTag(DreadfulSummonBoss.identityTag + "[spawnchance=" + SPAWN_CHANCE_PER_LEVEL * level + "]");
		}
	}

}
