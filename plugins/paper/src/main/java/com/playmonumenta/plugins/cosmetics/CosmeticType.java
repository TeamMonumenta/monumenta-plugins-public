package com.playmonumenta.plugins.cosmetics;

import java.util.Locale;
import org.bukkit.Material;
import org.checkerframework.checker.nullness.qual.Nullable;

public enum CosmeticType {
	TITLE("Player Title", Material.NAME_TAG),
	ELITE_FINISHER("Elite Finisher", Material.FIREWORK_ROCKET),
	PLOT_BORDER("Plot Border", Material.BARRIER),
	VANITY("Vanity Item", Material.GOLDEN_CHESTPLATE),
	;

	private final String mDisplayName;
	private final Material mDisplayItem;

	CosmeticType(String displayName, Material material) {
		mDisplayName = displayName;
		mDisplayItem = material;
	}

	public String getType() {
		return name().toLowerCase(Locale.ROOT);
	}

	public String getDisplayName() {
		return mDisplayName;
	}

	public Material getDisplayItem() {
		return mDisplayItem;
	}

	public boolean isEquippable() {
		return this != VANITY;
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
