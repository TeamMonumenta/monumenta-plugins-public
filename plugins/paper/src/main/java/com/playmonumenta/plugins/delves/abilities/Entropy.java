package com.playmonumenta.plugins.delves.abilities;

public class Entropy {

	private static final int DEPTH_POINTS_ASSIGNED_PER_LEVEL = 2;

	public static final String DESCRIPTION = "Additional Depth Points are randomly assigned.";

	public static String[] rankDescription(int level) {
			return new String[]{
				DEPTH_POINTS_ASSIGNED_PER_LEVEL * level + " Depth Points are randomly assigned."
			};
	}

	public static int getDepthPointsAssigned(int rank) {
		return rank <= 0 ? 0 : DEPTH_POINTS_ASSIGNED_PER_LEVEL * rank;
	}

}
