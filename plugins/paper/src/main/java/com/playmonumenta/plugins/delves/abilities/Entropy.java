package com.playmonumenta.plugins.delves.abilities;

import net.kyori.adventure.text.Component;

public class Entropy {

	private static final int DEPTH_POINTS_ASSIGNED_PER_LEVEL = 2;

	public static final String[] DESCRIPTION = {"Additional Delve Points are randomly assigned.", "Points in this modifier also count towards the total."};

	public static Component[] rankDescription(int level) {
		return new Component[]{
			Component.text(DEPTH_POINTS_ASSIGNED_PER_LEVEL * level + " Delve Points are randomly assigned."),
			Component.text("Additional points may exceed the modifier's cap.")
		};
	}

	public static int getDepthPointsAssigned(int rank) {
		return rank <= 0 ? 0 : DEPTH_POINTS_ASSIGNED_PER_LEVEL * rank;
	}

}
