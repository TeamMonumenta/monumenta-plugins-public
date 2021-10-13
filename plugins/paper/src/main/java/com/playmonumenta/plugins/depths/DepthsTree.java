package com.playmonumenta.plugins.depths;

public enum DepthsTree {
	FROSTBORN("Frostborn"),
	METALLIC("Steelsage"),
	SUNLIGHT("Dawnbringer"),
	EARTHBOUND("Earthbound"),
	SHADOWS("Shadowdancer"),
	WINDWALKER("Windwalker"),
	FLAMECALLER("Flamecaller");

	private final String displayName;

	DepthsTree(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

}
