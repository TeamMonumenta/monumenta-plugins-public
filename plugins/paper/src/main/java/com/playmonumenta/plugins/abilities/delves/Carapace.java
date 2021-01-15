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

	private static final int[] CARAPACE_SPEED_RAW_PERCENT = {
			2,
			4,
			6,
			8,
			10
	};

	public static final String DESCRIPTION = "Enemies gain a protective shell.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Enemies gain Carapaces with " + CARAPACE_HEALTH_RAW_PERCENT[0] + "% of their Max Health",
				"Elites Carapaces have x" + ELITE_CARAPACE_HEALTH_MULTIPLIER + " Health.",
				"A Carapace blocks that damage amount in any 10s period.",
				"Enemies with inactive Carapaces gain " + CARAPACE_SPEED_RAW_PERCENT[0] + "% Speed."
			}, {
				"Enemies gain Carapaces with " + CARAPACE_HEALTH_RAW_PERCENT[1] + "% of their Max Health",
				"Elites Carapaces have x" + ELITE_CARAPACE_HEALTH_MULTIPLIER + " Health.",
				"A Carapace blocks that damage amount in any 10s period.",
				"Enemies with inactive Carapaces gain " + CARAPACE_SPEED_RAW_PERCENT[1] + "% Speed."
			}, {
				"Enemies gain Carapaces with " + CARAPACE_HEALTH_RAW_PERCENT[2] + "% of their Max Health",
				"Elite Carapaces have x" + ELITE_CARAPACE_HEALTH_MULTIPLIER + " Health.",
				"A Carapace blocks that damage amount in any 10s period.",
				"Enemies with inactive Carapaces gain " + CARAPACE_SPEED_RAW_PERCENT[2] + "% Speed."
			}, {
				"Enemies gain Carapaces with " + CARAPACE_HEALTH_RAW_PERCENT[3] + "% of their Max Health",
				"Elites Carapaces have x" + ELITE_CARAPACE_HEALTH_MULTIPLIER + " Health.",
				"A Carapace blocks that damage amount in any 10s period.",
				"Enemies with inactive Carapaces gain " + CARAPACE_SPEED_RAW_PERCENT[3] + "% Speed."
			}, {
				"Enemies gain Carapaces with " + CARAPACE_HEALTH_RAW_PERCENT[4] + "% of their Max Health",
				"Elites Carapaces have x" + ELITE_CARAPACE_HEALTH_MULTIPLIER + " Health.",
				"A Carapace blocks that damage amount in any 10s period.",
				"Enemies with inactive Carapaces gain " + CARAPACE_SPEED_RAW_PERCENT[4] + "% Speed."
			}
	};

	private final int mCarapaceHealthRawPercent;
	private final int mCarapaceSpeedRawPercent;

	public Carapace(Plugin plugin, Player player) {
		super(plugin, player, Modifier.CARAPACE);

		int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.CARAPACE);
		mCarapaceHealthRawPercent = CARAPACE_HEALTH_RAW_PERCENT[rank - 1];
		mCarapaceSpeedRawPercent = CARAPACE_SPEED_RAW_PERCENT[rank - 1];
	}

	@Override
	public void applyModifiers(LivingEntity mob, EntitySpawnEvent event) {
		if (!DelvesUtils.isDelveMob(mob) && !EntityUtils.isBoss(mob)) {
			mob.addScoreboardTag(CarapaceBoss.identityTag);

			int healthPercent = EntityUtils.isElite(mob) ?
					(int)(ELITE_CARAPACE_HEALTH_MULTIPLIER * mCarapaceHealthRawPercent) : mCarapaceHealthRawPercent;
			mob.addScoreboardTag(CarapaceBoss.identityTag + healthPercent + "," + mCarapaceSpeedRawPercent);
		}
	}

}
