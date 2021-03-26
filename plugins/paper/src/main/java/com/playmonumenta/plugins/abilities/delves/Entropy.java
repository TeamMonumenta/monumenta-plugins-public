package com.playmonumenta.plugins.abilities.delves;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;

public class Entropy extends DelveModifier {

	private static final int[] DEPTH_POINTS_ASSIGNED = {
			2,
			4,
			6,
			8,
			10
	};

	private static final int[] TOTAL_DEPTH_POINTS_REQUIRED = {
			5,
			10,
			15,
			20,
			25
	};

	public static final String DESCRIPTION = "Additional Depth Points are randomly assigned.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				DEPTH_POINTS_ASSIGNED[0] + " Depth Points are randomly assigned.",
				"Requires at least " + TOTAL_DEPTH_POINTS_REQUIRED[0] + " Depth Points total",
				"to begin a Delve with this modifier selected."
			}, {
				DEPTH_POINTS_ASSIGNED[1] + " Depth Points are randomly assigned.",
				"Requires at least " + TOTAL_DEPTH_POINTS_REQUIRED[1] + " Depth Points total",
				"to begin a Delve with this modifier selected."
			}, {
				DEPTH_POINTS_ASSIGNED[2] + " Depth Points are randomly assigned.",
				"Requires at least " + TOTAL_DEPTH_POINTS_REQUIRED[2] + " Depth Points total",
				"to begin a Delve with this modifier selected."
			}, {
				DEPTH_POINTS_ASSIGNED[3] + " Depth Points are randomly assigned.",
				"Requires at least " + TOTAL_DEPTH_POINTS_REQUIRED[3] + " Depth Points total",
				"to begin a Delve with this modifier selected."
			}, {
				DEPTH_POINTS_ASSIGNED[4] + " Depth Points are randomly assigned.",
				"Requires at least " + TOTAL_DEPTH_POINTS_REQUIRED[4] + " Depth Points total",
				"to begin a Delve with this modifier selected."
			}
	};

	public Entropy(Plugin plugin, Player player) {
		super(plugin, player, Modifier.ENTROPY);
	}

	public static int getDepthPointsAssigned(int rank) {
		return rank == 0 ? 0 : DEPTH_POINTS_ASSIGNED[rank - 1];
	}

	public static int getTotalDepthPointsRequired(int rank) {
		return rank == 0 ? 0 : TOTAL_DEPTH_POINTS_REQUIRED[rank - 1];
	}

}
