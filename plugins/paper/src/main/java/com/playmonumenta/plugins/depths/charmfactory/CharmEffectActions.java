package com.playmonumenta.plugins.depths.charmfactory;

import javax.annotation.Nullable;

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
		for (CharmEffectActions ce : values()) {
			if (ce.mAction.equals(actionName)) {
				return ce;
			}
		}
		return null;
	}

	public static @Nullable CharmEffectActions upgradeAction(@Nullable CharmEffectActions action) {
		if (action == null) {
			return null;
		}

		return switch (action) {
			case N_LEGENDARY -> N_EPIC;
			case N_EPIC -> N_RARE;
			case N_RARE -> N_UNCOMMON;
			case N_UNCOMMON -> N_COMMON;
			case N_COMMON -> COMMON;
			case COMMON -> UNCOMMON;
			case UNCOMMON -> RARE;
			case RARE -> EPIC;
			case EPIC -> LEGENDARY;
			default -> null;
		};
	}

	public static @Nullable CharmEffectActions getActionFromInt(int level) {
		return switch (level) {
			case 1 -> COMMON;
			case 2 -> UNCOMMON;
			case 3 -> RARE;
			case 4 -> EPIC;
			case 5 -> LEGENDARY;
			default -> null;
		};
	}
}
