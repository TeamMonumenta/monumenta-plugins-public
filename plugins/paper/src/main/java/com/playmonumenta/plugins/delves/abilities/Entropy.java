package com.playmonumenta.plugins.delves.abilities;

public class Entropy {

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

	public static int getDepthPointsAssigned(int rank) {
		return rank <= 0 ? 0 : DEPTH_POINTS_ASSIGNED[rank - 1];
	}

}
