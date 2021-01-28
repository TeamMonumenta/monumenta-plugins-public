package com.playmonumenta.plugins.abilities.delves;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntitySpawnEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.CarapaceBoss;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.EntityUtils;

public class Carapace extends DelveModifier {

	private static final int[] CARAPACE_HEALTH_RAW_PERCENT = {
			8,
			16,
			24,
			32,
			40
	};

	private static final double ELITE_CARAPACE_HEALTH_MULTIPLIER = 0.5;

	public static final String DESCRIPTION = "Enemies gain a protective shell.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Enemies gain Carapaces with " + CARAPACE_HEALTH_RAW_PERCENT[0] + "% of their Max Health",
				"Elites' Carapaces have x" + ELITE_CARAPACE_HEALTH_MULTIPLIER + " Health.",
				"A Carapace blocks that damage amount in any 10s period."
			}, {
				"Enemies gain Carapaces with " + CARAPACE_HEALTH_RAW_PERCENT[1] + "% of their Max Health",
				"Elites' Carapaces have x" + ELITE_CARAPACE_HEALTH_MULTIPLIER + " Health.",
				"A Carapace blocks that damage amount in any 10s period."
			}, {
				"Enemies gain Carapaces with " + CARAPACE_HEALTH_RAW_PERCENT[2] + "% of their Max Health",
				"Elites' Carapaces have x" + ELITE_CARAPACE_HEALTH_MULTIPLIER + " Health.",
				"A Carapace blocks that damage amount in any 10s period."
			}, {
				"Enemies gain Carapaces with " + CARAPACE_HEALTH_RAW_PERCENT[3] + "% of their Max Health",
				"Elites' Carapaces have x" + ELITE_CARAPACE_HEALTH_MULTIPLIER + " Health.",
				"A Carapace blocks that damage amount in any 10s period."
			}, {
				"Enemies gain Carapaces with " + CARAPACE_HEALTH_RAW_PERCENT[4] + "% of their Max Health",
				"Elites' Carapaces have x" + ELITE_CARAPACE_HEALTH_MULTIPLIER + " Health.",
				"A Carapace blocks that damage amount in any 10s period."
			}
	};

	private final int mCarapaceHealthRawPercent;

	public Carapace(Plugin plugin, Player player) {
		super(plugin, player, Modifier.CARAPACE);

		int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.CARAPACE);
		mCarapaceHealthRawPercent = CARAPACE_HEALTH_RAW_PERCENT[rank - 1];
	}

	@Override
	public void applyModifiers(LivingEntity mob, EntitySpawnEvent event) {
		if (!DelvesUtils.isDelveMob(mob) && !EntityUtils.isBoss(mob)) {
			mob.addScoreboardTag(CarapaceBoss.identityTag);

			int healthPercent = EntityUtils.isElite(mob) ?
					(int)(ELITE_CARAPACE_HEALTH_MULTIPLIER * mCarapaceHealthRawPercent) : mCarapaceHealthRawPercent;
			mob.addScoreboardTag(CarapaceBoss.identityTag + healthPercent + ",0");
		}
	}

}
