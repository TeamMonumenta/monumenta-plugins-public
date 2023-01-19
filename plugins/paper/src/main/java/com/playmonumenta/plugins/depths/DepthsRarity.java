package com.playmonumenta.plugins.depths;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public enum DepthsRarity {
	COMMON("Common", TextColor.fromHexString("#9f929c")),
	UNCOMMON("Uncommon", TextColor.fromHexString("#70bc6d")),
	RARE("Rare", TextColor.fromHexString("#705eca")),
	EPIC("Epic", TextColor.fromHexString("#cd5eca")),
	LEGENDARY("Legendary", TextColor.fromHexString("#e49b20")),
	TWISTED("XXXXXX", TextColor.color(DepthsUtils.LEVELSIX), true);

	private final String mName;
	private final TextColor mColor;
	private final boolean mObfuscated;

	DepthsRarity(String name, TextColor color) {
		this(name, color, false);
	}

	DepthsRarity(String name, TextColor color, boolean obfuscated) {
		mName = name;
		mColor = color;
		mObfuscated = obfuscated;
	}

	public TextComponent getDisplay() {
		return Component.text(mName, mColor).decoration(TextDecoration.OBFUSCATED, mObfuscated).decoration(TextDecoration.ITALIC, false);
	}

	public String getName() {
		return mName;
	}

	public TextColor getColor() {
		return mColor;
	}
}
