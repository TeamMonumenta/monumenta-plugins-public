package com.playmonumenta.plugins.abilities.delves;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.SpawnerSpawnEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.VindictiveBoss;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.FastUtils;

public class Vindictive extends DelveModifier {

	private static final double[] ABILITY_CHANCE = {
			0.1,
			0.2,
			0.3
	};

	public static final String DESCRIPTION = "Dying enemies enrage nearby enemies.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Enemies have a " + Math.round(ABILITY_CHANCE[0] * 100) + "% chance to be Vindictive."
			}, {
				"Enemies have a " + Math.round(ABILITY_CHANCE[1] * 100) + "% chance to be Vindictive."
			}, {
				"Enemies have a " + Math.round(ABILITY_CHANCE[2] * 100) + "% chance to be Vindictive."
			}
	};

	private final double mAbilityChance;

	public Vindictive(Plugin plugin, Player player) {
		super(plugin, player, Modifier.VINDICTIVE);

		int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.VINDICTIVE);
		mAbilityChance = ABILITY_CHANCE[rank - 1];
	}

	@Override
	public void applyModifiers(LivingEntity mob, SpawnerSpawnEvent event) {
		if (FastUtils.RANDOM.nextDouble() < mAbilityChance) {
			// This runs prior to BossManager parsing, so we can just add tags directly
			mob.addScoreboardTag(VindictiveBoss.identityTag);
		}
	}

}
