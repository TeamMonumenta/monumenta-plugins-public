package com.playmonumenta.plugins.abilities.delves;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.DistanceCloserBoss;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import javax.annotation.Nullable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntitySpawnEvent;

public class Relentless extends DelveModifier {

	private static final int DISTANCE_CLOSER_DISTANCE = 8;

	private static final int[] DISTANCE_CLOSER_SPEED_RAW_PERCENT = {
			10,
			20,
			30,
			40,
			50
	};

	public static final String DESCRIPTION = "Enemies are harder to stop.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				"Enemies are " + DISTANCE_CLOSER_SPEED_RAW_PERCENT[0] + "% faster when more than " + DISTANCE_CLOSER_DISTANCE + " blocks away."
			}, {
				"Enemies are " + DISTANCE_CLOSER_SPEED_RAW_PERCENT[1] + "% faster when more than " + DISTANCE_CLOSER_DISTANCE + " blocks away."
			}, {
				"Enemies are " + DISTANCE_CLOSER_SPEED_RAW_PERCENT[2] + "% faster when more than " + DISTANCE_CLOSER_DISTANCE + " blocks away."
			}, {
				"Enemies are " + DISTANCE_CLOSER_SPEED_RAW_PERCENT[3] + "% faster when more than " + DISTANCE_CLOSER_DISTANCE + " blocks away."
			}, {
				"Enemies are " + DISTANCE_CLOSER_SPEED_RAW_PERCENT[4] + "% faster when more than " + DISTANCE_CLOSER_DISTANCE + " blocks away."
			}
	};

	private final int mDistanceCloserSpeedRawPercent;

	public Relentless(Plugin plugin, @Nullable Player player) {
		super(plugin, player, Modifier.RELENTLESS);

		if (player != null) {
			int rank = DelvesUtils.getDelveInfo(player).getRank(Modifier.RELENTLESS);
			mDistanceCloserSpeedRawPercent = DISTANCE_CLOSER_SPEED_RAW_PERCENT[rank - 1];
		} else {
			mDistanceCloserSpeedRawPercent = 0;
		}
	}

	@Override
	public void applyModifiers(LivingEntity mob, EntitySpawnEvent event) {
		mob.addScoreboardTag(DistanceCloserBoss.identityTag);
		mob.addScoreboardTag(DistanceCloserBoss.identityTag + DISTANCE_CLOSER_DISTANCE + "," + mDistanceCloserSpeedRawPercent);
	}

}
