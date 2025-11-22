package com.playmonumenta.plugins.itemstats.enums;

import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public enum Region {
	NONE("none", ItemStatUtils.DUMMY_LORE_TO_REMOVE),
	SHULKER_BOX("shulker", Component.text("INVALID ENTRY", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)),
	VALLEY("valley", Component.text("King's Valley : ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)),
	ISLES("isles", Component.text("Celsian Isles : ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)),
	RING("ring", Component.text("Architect's Ring : ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));

	public static final String KEY = "Region";

	final String mName;
	final Component mDisplay;
	final String mPlainDisplay;

	Region(String name, Component display) {
		mName = name;
		mDisplay = display;
		mPlainDisplay = MessagingUtils.plainText(display);
	}

	public String getName() {
		return mName;
	}

	public Component getDisplay() {
		return mDisplay;
	}

	public String getPlainDisplay() {
		return mPlainDisplay;
	}

	public static Region getRegion(String name) {
		for (Region region : values()) {
			if (region.getName().equals(name)) {
				return region;
			}
		}

		return NONE;
	}

	public static int getRegionNumber(String name) {
		return getRegionNumber(getRegion(name));
	}

	public static int getRegionNumber(Region region) {
		switch (region) {
			case VALLEY:
				return 1;
			case ISLES:
				return 2;
			case RING:
				return 3;
			default:
				return 0;
		}
	}
}
