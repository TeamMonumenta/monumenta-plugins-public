package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.nms.utils.NmsBossUtils;

/*
 * This isn't a boss AI or tag. It applies an AI that makes a mob able to wander around within a radius.
 * The radius is defined as the distance of the mEndLoc from the mSpawnLoc. The mob will walk at normal speed.
 * If they target a mob, their wandering pathfinding will not interrupt their mob targetting pathfinding.
 * They choose their next wander point and move every 7-9 seconds (or 140-180 ticks)
 * Will make mob passive.
 */

public class EvokerNoVex extends BossAbilityGroup {
	public static final String identityTag = "boss_evoker_no_vex";
	public static final int detectionRange = 50;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new EvokerNoVex(plugin, boss);
	}

	public EvokerNoVex(Plugin plugin, LivingEntity boss) {
		NmsBossUtils.removeVexSpawnAIFromEvoker(boss);

		super.constructBoss(plugin, identityTag, boss, null, null, detectionRange, null);
	}
}
