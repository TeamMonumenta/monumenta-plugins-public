package com.playmonumenta.plugins.effects;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

public class StarBlight extends FlatHealthBoost {

	public StarBlight(int duration, double amount, String modifierName) {
		super(duration, amount, modifierName);
		deleteOnLogout(true);
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return Component.text(getDisplayedName() + " (" + mAmount + " Max Health)", NamedTextColor.RED);
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Blight Infection Level";
	}
}
