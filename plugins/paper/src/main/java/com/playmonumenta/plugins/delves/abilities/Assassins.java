package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.bosses.bosses.DodgeBoss;
import com.playmonumenta.plugins.bosses.bosses.TpBehindBoss;
import com.playmonumenta.plugins.bosses.bosses.UnseenBoss;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.LivingEntity;

public class Assassins {
	private static List<List<String>> ABILITY_POOL;

	public static final String DESCRIPTION = "Enemies become stealthy assassins.";

	public static final String[][] RANK_DESCRIPTIONS = {
		{
			"Mobs deal 25% extra damage when not in the player",
			"field of view, and have a 30% chance to become",
			"\"stealthed,\" gaining new abilities."
		}
	};

	static {
		ABILITY_POOL = new ArrayList<>();

		//TpBehindTargetedBoss
		List<String> tpBehind = new ArrayList<>();
		tpBehind.add(TpBehindBoss.identityTag);
		tpBehind.add(TpBehindBoss.identityTag + "[range=50,random=false]");
		ABILITY_POOL.add(tpBehind);

		List<String> dodge = new ArrayList<>();
		dodge.add(DodgeBoss.identityTag);
		ABILITY_POOL.add(dodge);
	}

	public static void applyModifiers(LivingEntity mob, int level) {
		if (!DelvesUtils.isDelveMob(mob)) {
			mob.addScoreboardTag(UnseenBoss.identityTag);
			mob.addScoreboardTag(UnseenBoss.identityTag + "[damageincrease=1.5]");
			if (FastUtils.RANDOM.nextDouble() < .3) {
				List<String> ability = ABILITY_POOL.get(FastUtils.RANDOM.nextInt(ABILITY_POOL.size()));
				for (String abilityTag : ability) {
					mob.addScoreboardTag(abilityTag);
				}
			}
		}
	}
}
