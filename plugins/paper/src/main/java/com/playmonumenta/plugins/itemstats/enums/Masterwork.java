package com.playmonumenta.plugins.itemstats.enums;

import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.Nullable;

public enum Masterwork {
	NONE("none", ItemStatUtils.DUMMY_LORE_TO_REMOVE),
	ZERO("0", 0),
	I("1", 1),
	II("2", 2),
	III("3", 3),
	IV("4", 4),
	V("5", 5),
	VI("6", 6),
	VIIA("7a", 7, TextColor.fromHexString("#D02E28")),
	VIIB("7b", 7, TextColor.fromHexString("#4AC2E5")),
	VIIC("7c", 7, TextColor.fromHexString("#FFFA75")),
	ERROR("error", Component.text("ERROR", TextColor.fromHexString("#704C8A")).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.OBFUSCATED, true));

	public static final String KEY = "Masterwork";
	public static final int CURRENT_MAX_MASTERWORK = 4;

	final String mName;
	final Component mDisplay;
	final String mPlainDisplay;

	Masterwork(String name, Component display) {
		mName = name;
		mDisplay = display;
		mPlainDisplay = MessagingUtils.plainText(display);
	}

	Masterwork(String name, int level, TextColor color) {
		this(name, Component.text("★".repeat(level), color).append(Component.text("☆".repeat(Math.max(0, CURRENT_MAX_MASTERWORK - level)), NamedTextColor.DARK_GRAY)).decoration(TextDecoration.ITALIC, false));
	}

	Masterwork(String name, int level) {
		this(name, level, TextColor.fromHexString("#FFB43E"));
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

	public static Masterwork getMasterwork(@Nullable String name) {
		if (name == null) {
			return NONE;
		}

		for (Masterwork m : values()) {
			if (m.getName().equals(name)) {
				return m;
			}
		}

		return NONE;
	}

	// Only use for 0-5
	public Masterwork next() {
		return switch (this) {
			case ZERO -> I;
			case I -> II;
			case II -> III;
			case III -> IV;
			case IV -> V;
			case V -> VI;
			default -> this;
		};
	}
}
