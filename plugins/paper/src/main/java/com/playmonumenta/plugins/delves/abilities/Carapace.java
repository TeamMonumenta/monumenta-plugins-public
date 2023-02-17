package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.bosses.bosses.BarrierBoss;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.LivingEntity;

public class Carapace {
	private static final List<List<String>> ABILITY_POOL;
	private static final double ABILITY_CHANCE_PER_LEVEL = 0.1;

	static {
		ABILITY_POOL = new ArrayList<>();

		//ProjectileBoss - tracking
		List<String> barrierBoss = new ArrayList<>();
		barrierBoss.add(BarrierBoss.identityTag);
		barrierBoss.add(BarrierBoss.identityTag + "[cooldown=10000,iscarapace=true,particle=[(REDSTONE,4,0,1,0,0,#a64d4d,2)]]");
		ABILITY_POOL.add(barrierBoss);
	}

	public static final String DESCRIPTION = "Enemies gain a protective shell.";

	public static String[] rankDescription(int level) {
		return new String[]{
			"Enemies have a " + Math.round(ABILITY_CHANCE_PER_LEVEL * level * 100) + "% chance to gain Carapaces.",
			"Carapaces block the first hit the enemy takes, and the enemy",
			"deals 30% more damage until the Carapace is broken."
		};
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		if (FastUtils.RANDOM.nextDouble() < ABILITY_CHANCE_PER_LEVEL * level && !DelvesUtils.isDelveMob(mob)) {
			// This runs prior to BossManager parsing, so we can just add tags directly
			List<String> ability = ABILITY_POOL.get(FastUtils.RANDOM.nextInt(ABILITY_POOL.size()));
			for (String abilityTag : ability) {
				mob.addScoreboardTag(abilityTag);
			}
		}
	}

}
