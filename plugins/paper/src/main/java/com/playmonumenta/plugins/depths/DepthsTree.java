package com.playmonumenta.plugins.depths;

public enum DepthsTree {
	FROSTBORN("Frostborn"),
	METALLIC("Steelsage"),
	SUNLIGHT("Dawnbringer"),
	EARTHBOUND("Earthbound"),
	SHADOWS("Shadowdancer"),
	WINDWALKER("Windwalker"),
	FLAMECALLER("Flamecaller");

	private final String mDisplayName;

	DepthsTree(String displayName) {
		this.mDisplayName = displayName;
	}

	public String getDisplayName() {
		return mDisplayName;
	}

}
