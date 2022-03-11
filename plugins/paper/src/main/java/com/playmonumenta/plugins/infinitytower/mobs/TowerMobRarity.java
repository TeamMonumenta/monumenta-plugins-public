package com.playmonumenta.plugins.infinitytower.mobs;

public enum TowerMobRarity {
	COMMON(1, "Common", 0.07, "ITCommon", 1, 0.8, 0.65, 0.60, 0.50, 0.35, 0.25, 0.20, 0.15, 0.05),
	RARE(2, "Rare", 0.09, "ITRare", 0, 0.2, 0.3, 0.3, 0.30, 0.4, 0.4, 0.30, 0.25, 0.25),
	EPIC(3, "Epic", 0.11, "ITEpic", 0, 0, 0.05, 0.1, 0.20, 0.25, 0.30, 0.40, 0.45, 0.50),
	LEGENDARY(4, "Legendary", 0.13, "ITLegendary", 0, 0, 0, 0, 0, 0, 0.05, 0.10, 0.15, 0.20);

	private final int mIndex;
	private final double[] mRarity;
	private final double mDamageMult;
	private final String mName;
	private final String mTag;

	TowerMobRarity(int index, String name, double damageMult, String tag, double... rarity) {
		mIndex = index;
		mRarity = rarity;
		mName = name;
		mTag = tag;
		mDamageMult = damageMult;
	}

	public double getDamageMult() {
		return mDamageMult;
	}

	public int getIndex() {
		return mIndex;
	}

	public double getWeight(int lvl) {
		if (mRarity.length > lvl) {
			return mRarity[lvl];
		}
		return 0;
	}

	public String getName() {
		return mName;
	}

	public String getTag() {
		return mTag;
	}

	public static TowerMobRarity fromName(String name) {
		for (TowerMobRarity rarity : values()) {
			if (rarity.mName.equals(name)) {
				return rarity;
			}
		}

		return COMMON;
	}

	public static double[] getArrayWeight(int lvl) {
		double[] totalWeight = new double[values().length];

		int i = 0;

		for (TowerMobRarity rarity : values()) {
			totalWeight[i++] = rarity.getWeight(lvl);
		}

		return totalWeight;
	}
}
