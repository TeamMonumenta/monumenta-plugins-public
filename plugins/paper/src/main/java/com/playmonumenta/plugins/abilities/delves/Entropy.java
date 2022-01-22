package com.playmonumenta.plugins.abilities.delves;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Entropy extends DelveModifier {

	private static final int[] DEPTH_POINTS_ASSIGNED = {
			2,
			4,
			6,
			8,
			10
	};

	public static final String DESCRIPTION = "Additional Depth Points are randomly assigned.";

	public static final String[][] RANK_DESCRIPTIONS = {
			{
				DEPTH_POINTS_ASSIGNED[0] + " Depth Points are randomly assigned."
			}, {
				DEPTH_POINTS_ASSIGNED[1] + " Depth Points are randomly assigned."
			}, {
				DEPTH_POINTS_ASSIGNED[2] + " Depth Points are randomly assigned."
			}, {
				DEPTH_POINTS_ASSIGNED[3] + " Depth Points are randomly assigned."
			}, {
				DEPTH_POINTS_ASSIGNED[4] + " Depth Points are randomly assigned."
			}
	};

	public Entropy(Plugin plugin, @Nullable Player player) {
		super(plugin, player, Modifier.ENTROPY);
	}

	public static int getDepthPointsAssigned(int rank) {
		return rank == 0 ? 0 : DEPTH_POINTS_ASSIGNED[rank - 1];
	}

}
