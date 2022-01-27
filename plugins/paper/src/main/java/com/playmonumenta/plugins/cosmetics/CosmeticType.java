package com.playmonumenta.plugins.cosmetics;

import org.bukkit.Material;
import org.checkerframework.checker.nullness.qual.Nullable;

public enum CosmeticType {
	TITLE("title", "Player Title", Material.NAME_TAG),
	ELITE_FINISHER("elite_finisher", "Elite Finisher", Material.FIREWORK_ROCKET),
	PLOT_BORDER("plot_border", "Plot Border", Material.BARRIER);

	private final String mType;
	private final String mDisplayName;
	private final Material mDisplayItem;


	CosmeticType(String type, String displayName, Material material) {
		mType = type;
		mDisplayName = displayName;
		mDisplayItem = material;
	}

	public String getType() {
		return mType;
	}

	public String getDisplayName() {
		return mDisplayName;
	}

	public Material getDisplayItem() {
		return mDisplayItem;
	}

	public static @Nullable CosmeticType getTypeSelection(String type) {
		if (type == null) {
			return null;
		}
		for (CosmeticType selection : CosmeticType.values()) {
			if (selection.getType().equals(type)) {
				return selection;
			}
		}
		return null;
	}
}
