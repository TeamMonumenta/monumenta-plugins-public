package com.playmonumenta.plugins.itemstats.enums;

import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public enum Tier {
	NONE("none", ItemStatUtils.DUMMY_LORE_TO_REMOVE),
	ZERO("0", "Tier 0", NamedTextColor.DARK_GRAY),
	I("1", "Tier I", NamedTextColor.DARK_GRAY),
	II("2", "Tier II", NamedTextColor.DARK_GRAY),
	III("3", "Tier III", NamedTextColor.DARK_GRAY),
	IV("4", "Tier IV", NamedTextColor.DARK_GRAY),
	V("5", "Tier V", NamedTextColor.DARK_GRAY),
	COMMON("common", "Common", TextColor.fromHexString("#C0C0C0")),
	UNCOMMON("uncommon", "Uncommon", TextColor.fromHexString("#C0C0C0")),
	RARE("rare", "Rare", TextColor.fromHexString("#4AC2E5")),
	ARTIFACT("artifact", "Artifact", TextColor.fromHexString("#D02E28")),
	EPIC("epic", Component.text("Epic", TextColor.fromHexString("#B314E3")).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true)),
	LEGENDARY("legendary", Component.text("Legendary", TextColor.fromHexString("#FFD700")).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true)),
	UNIQUE("unique", "Unique", TextColor.fromHexString("#C8A2C8")),
	PATRON("patron", "Patron Made", TextColor.fromHexString("#82DB17")),
	EVENT("event", "Event", TextColor.fromHexString("#7FFFD4")),
	LEGACY("legacy", "Legacy", TextColor.fromHexString("#EEE6D6")),
	CURRENCY("currency", "Currency", TextColor.fromHexString("#DCAE32")),
	EVENT_CURRENCY("event_currency", "Event Currency", TextColor.fromHexString("#DCAE32")),
	FISH("fish", "Fish", TextColor.fromHexString("#1DCC9A")),
	KEYTIER("key", "Key", TextColor.fromHexString("#47B6B5")),
	TROPHY("trophy", "Trophy", TextColor.fromHexString("#CAFFFD")),
	OBFUSCATED("obfuscated", Component.text("Stick_:)", TextColor.fromHexString("#5D2D87")).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.OBFUSCATED, true)),
	SHULKER_BOX("shulker", "Invalid Type", TextColor.fromHexString("#EEE6D6")),
	CHARM("charm", "Charm", TextColor.fromHexString("#FFFA75")),
	LEGACY_CHARM("legacycharm", "Legacy Charm", TextColor.fromHexString("#EEE6D6")),
	RARE_CHARM("rarecharm", "Rare Charm", TextColor.fromHexString("#4AC2E5")),
	EPIC_CHARM("epiccharm", Component.text("Epic Charm", TextColor.fromHexString("#B314E3")).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true)),
	ZENITH_CHARM("zenithcharm", Component.text("Zenith Charm", TextColor.fromHexString("#FF9CF0")).decoration(TextDecoration.ITALIC, false)),
	QUEST_COMPASS("quest_compass", "Invalid Type", TextColor.fromHexString("#EEE6D6"));

	public static final String KEY = "Tier";

	final String mName;
	final Component mDisplay;
	final String mPlainDisplay;

	Tier(String name, Component display) {
		mName = name;
		mDisplay = display;
		mPlainDisplay = MessagingUtils.plainText(display);
	}

	Tier(String name, String display, TextColor color) {
		this(name, Component.text(display, color).decoration(TextDecoration.ITALIC, false));
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

	public static Tier getTier(String name) {
		for (Tier tier : values()) {
			if (tier.getName().equals(name)) {
				return tier;
			}
		}

		return NONE;
	}
}
