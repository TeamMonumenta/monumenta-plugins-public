package com.playmonumenta.plugins.abilities.delves;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vex;
import org.bukkit.event.entity.EntitySpawnEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.BlockBreakBoss;
import com.playmonumenta.plugins.bosses.bosses.DistanceCloserBoss;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.FastUtils;

public class Relentless extends DelveModifier {

	private static final int DISTANCE_CLOSER_DISTANCE = 6;

	private static final int[] DISTANCE_CLOSER_SPEED_RAW_PERCENT = {
			10,
			15,
			20,
			25,
			30
	};

	private static final double[] BLOCK_BREAK_CHANCE = {
			0.02,
			0.04,
			0.06,
			0.08,
			0.1
	};

	public static final String DESCRIPTION = "Enemies are harder to stop.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Enemies are " + DISTANCE_CLOSER_SPEED_RAW_PERCENT[0] + "% faster when more than " + DISTANCE_CLOSER_DISTANCE + " blocks away.",
				"Enemies have a " + Math.round(BLOCK_BREAK_CHANCE[0] * 100) + "% chance to have Block Break."
			}, {
				"Enemies are " + DISTANCE_CLOSER_SPEED_RAW_PERCENT[1] + "% faster when more than " + DISTANCE_CLOSER_DISTANCE + " blocks away.",
				"Enemies have a " + Math.round(BLOCK_BREAK_CHANCE[1] * 100) + "% chance to have Block Break."
			}, {
				"Enemies are " + DISTANCE_CLOSER_SPEED_RAW_PERCENT[2] + "% faster when more than " + DISTANCE_CLOSER_DISTANCE + " blocks away.",
				"Enemies have a " + Math.round(BLOCK_BREAK_CHANCE[2] * 100) + "% chance to have Block Break."
			}, {
				"Enemies are " + DISTANCE_CLOSER_SPEED_RAW_PERCENT[3] + "% faster when more than " + DISTANCE_CLOSER_DISTANCE + " blocks away.",
				"Enemies have a " + Math.round(BLOCK_BREAK_CHANCE[3] * 100) + "% chance to have Block Break."
			}, {
				"Enemies are " + DISTANCE_CLOSER_SPEED_RAW_PERCENT[4] + "% faster when more than " + DISTANCE_CLOSER_DISTANCE + " blocks away.",
				"Enemies have a " + Math.round(BLOCK_BREAK_CHANCE[4] * 100) + "% chance to have Block Break."
			}
	};

	private final int mDistanceCloserSpeedRawPercent;
	private final double mBlockBreakChance;

	public Relentless(Plugin plugin, Player player) {
		super(plugin, player, Modifier.RELENTLESS);

		if (player != null) {
			int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.RELENTLESS);
			mDistanceCloserSpeedRawPercent = DISTANCE_CLOSER_SPEED_RAW_PERCENT[rank - 1];
			mBlockBreakChance = BLOCK_BREAK_CHANCE[rank - 1];
		} else {
			mDistanceCloserSpeedRawPercent = 0;
			mBlockBreakChance = 0;
		}
	}

	@Override
	public void applyModifiers(LivingEntity mob, EntitySpawnEvent event) {
		mob.addScoreboardTag(DistanceCloserBoss.identityTag);
		mob.addScoreboardTag(DistanceCloserBoss.identityTag + DISTANCE_CLOSER_DISTANCE + "," + mDistanceCloserSpeedRawPercent);

		if (!(mob instanceof Vex) && FastUtils.RANDOM.nextDouble() < mBlockBreakChance) {
			mob.addScoreboardTag(BlockBreakBoss.identityTag);
		}
	}

}
