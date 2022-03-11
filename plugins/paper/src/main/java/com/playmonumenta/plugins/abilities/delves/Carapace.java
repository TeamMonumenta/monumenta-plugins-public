package com.playmonumenta.plugins.abilities.delves;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.BarrierBoss;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntitySpawnEvent;

public class Carapace extends DelveModifier {
	private static final List<List<String>> ABILITY_POOL;
	private static final double[] ABILITY_CHANCE = {
		0.1,
		0.2,
		0.3,
		0.4,
		0.5,
		0.6,
		0.7,
		};

	static {
		ABILITY_POOL = new ArrayList<>();

		//ProjectileBoss - tracking
		List<String> barrierBoss = new ArrayList<>();
		barrierBoss.add(BarrierBoss.identityTag);
		barrierBoss.add(BarrierBoss.identityTag + "[cooldown=10000,iscarapace=true,particle=[(REDSTONE,4,0,1,0,0,#a64d4d,2)]]");
		ABILITY_POOL.add(barrierBoss);
	}

	public static final String DESCRIPTION = "Enemies gain a protective shell.";

	public static final String[][] RANK_DESCRIPTIONS = {
		{
			"Enemies have a " + Math.round(ABILITY_CHANCE[0] * 100) + "% chance to gain Carapaces.",
			"Carapaces block the first hit the enemy takes, and the enemy",
			"deals 30% more damage until the Carapace is broken."
		}, {
			"Enemies have a " + Math.round(ABILITY_CHANCE[1] * 100) + "% chance to gain Carapaces.",
			"Carapaces block the first hit the enemy takes, and the enemy",
			"deals 30% more damage until the Carapace is broken."
		}, {
			"Enemies have a " + Math.round(ABILITY_CHANCE[2] * 100) + "% chance to gain Carapaces.",
			"Carapaces block the first hit the enemy takes, and the enemy",
			"deals 30% more damage until the Carapace is broken."
		}, {
			"Enemies have a " + Math.round(ABILITY_CHANCE[3] * 100) + "% chance to gain Carapaces.",
			"Carapaces block the first hit the enemy takes, and the enemy",
			"deals 30% more damage until the Carapace is broken."
		}, {
			"Enemies have a " + Math.round(ABILITY_CHANCE[4] * 100) + "% chance to gain Carapaces.",
			"Carapaces block the first hit the enemy takes, and the enemy",
			"deals 30% more damage until the Carapace is broken."
		}, {
			"Enemies have a " + Math.round(ABILITY_CHANCE[5] * 100) + "% chance to gain Carapaces.",
			"Carapaces block the first hit the enemy takes, and the enemy",
			"deals 30% more damage until the Carapace is broken."
		}, {
			"Enemies have a " + Math.round(ABILITY_CHANCE[6] * 100) + "% chance to gain Carapaces.",
			"Carapaces block the first hit the enemy takes, and the enemy",
			"deals 30% more damage until the Carapace is broken."
		}
	};

	private final double mAbilityChance;

	public Carapace(Plugin plugin, @Nullable Player player) {
		super(plugin, player, Modifier.CARAPACE);

		if (player != null) {
			int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.CARAPACE);
			mAbilityChance = ABILITY_CHANCE[rank - 1];
		} else {
			mAbilityChance = 0;
		}
	}

	@Override
	public void applyModifiers(LivingEntity mob, EntitySpawnEvent event) {
		if (FastUtils.RANDOM.nextDouble() < mAbilityChance) {
			// This runs prior to BossManager parsing, so we can just add tags directly
			List<String> ability = ABILITY_POOL.get(FastUtils.RANDOM.nextInt(ABILITY_POOL.size()));
			for (String abilityTag : ability) {
				mob.addScoreboardTag(abilityTag);
			}
		}
	}

}
