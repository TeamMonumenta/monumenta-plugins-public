package com.playmonumenta.plugins.depths.charmfactory;

import org.checkerframework.checker.nullness.qual.Nullable;

public enum CharmEffectActions {

	COMMON("Common", 1, -5, false),
	UNCOMMON("Uncommon", 2, -8, false),
	RARE("Rare", 3, -11, false),
	EPIC("Epic", 4, -14, false),
	LEGENDARY("Legendary", 5, -16, false),
	N_COMMON("Negative Common", 1, 4, true),
	N_UNCOMMON("Negative Uncommon", 2, 7, true),
	N_RARE("Negative Rare", 3, 10, true),
	N_EPIC("Negative Epic", 4, 13, true),
	N_LEGENDARY("Negative Legendary", 5, 15, true);

	public final String mAction;
	public final int mRarity;
	public final int mBudget;
	public final boolean mIsNegative;

	CharmEffectActions(String name, int rarity, int budget, boolean isNegative) {
		mAction = name;
		mRarity = rarity;
		mBudget = budget;
		mIsNegative = isNegative;
	}

	public static @Nullable CharmEffectActions getEffect(String actionName) {
		for (CharmEffectActions ce : CharmEffectActions.values()) {
			if (ce.mAction.equals(actionName)) {
				return ce;
			}
		}
		return null;
	}
}
