package com.playmonumenta.plugins.abilities.delves;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.event.entity.SpawnerSpawnEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.BlockBreakBoss;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.FastUtils;

public class Pernicious extends DelveModifier {

	private static final double[] BLOCK_BREAK_CHANCE = {
			0.05,
			0.1,
			0.15,
			0.2,
			0.25,
			0.3
	};

	private final double mBlockBreakChance;

	public static final String DESCRIPTION = "Enemies can destroy terrain.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Enemies have a " + Math.round(BLOCK_BREAK_CHANCE[0] * 100) + "% chance to have Block Break."
			}, {
				"Enemies have a " + Math.round(BLOCK_BREAK_CHANCE[1] * 100) + "% chance to have Block Break."
			}, {
				"Enemies have a " + Math.round(BLOCK_BREAK_CHANCE[2] * 100) + "% chance to have Block Break."
			}, {
				"Enemies have a " + Math.round(BLOCK_BREAK_CHANCE[3] * 100) + "% chance to have Block Break."
			}, {
				"Enemies have a " + Math.round(BLOCK_BREAK_CHANCE[4] * 100) + "% chance to have Block Break."
			}, {
				"Enemies have a " + Math.round(BLOCK_BREAK_CHANCE[5] * 100) + "% chance to have Block Break."
			}
	};

	public Pernicious(Plugin plugin, Player player) {
		super(plugin, player, Modifier.PERNICIOUS);

		if (player != null) {
			int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.PERNICIOUS);
			mBlockBreakChance = BLOCK_BREAK_CHANCE[rank - 1];
		} else {
			mBlockBreakChance = 0;
		}
	}

	@Override
	public void applyModifiers(LivingEntity mob, SpawnerSpawnEvent event) {
		if (!(mob instanceof Vex) && FastUtils.RANDOM.nextDouble() < mBlockBreakChance) {
			mob.addScoreboardTag(BlockBreakBoss.identityTag);
		}
	}
}
