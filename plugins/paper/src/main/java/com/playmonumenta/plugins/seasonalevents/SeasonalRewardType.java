package com.playmonumenta.plugins.seasonalevents;

import org.jetbrains.annotations.Nullable;

public enum SeasonalRewardType {
	TITLE("title"),
	ABILITY_SKIN("ability_skin"),
	PLOT_BORDER("plot_border"),
	LOOT_TABLE("loot_table"),
	ELITE_FINISHER("elite_finisher"),
	ITEM_SKIN("item_skin"),
	LOOT_SPIN("loot_spin"),
	SHULKER_BOX("shulker_box"),
	UNIQUE_SPIN("unique_spin");

	private final String mType;

	SeasonalRewardType(String type) {
		mType = type;
	}

	public String getType() {
		return mType;
	}

	public static @Nullable SeasonalRewardType getRewardTypeSelection(String type) {
		if (type == null) {
			return null;
		}
		for (SeasonalRewardType selection : SeasonalRewardType.values()) {
			if (selection.getType().equals(type)) {
				return selection;
			}
		}
		return null;
	}
}
