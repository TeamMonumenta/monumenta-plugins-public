package com.playmonumenta.plugins.effects;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public class StarBlight extends FlatHealthBoost {

	public StarBlight(int duration, double amount, String modifierName) {
		super(duration, amount, modifierName);
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return Component.text(getDisplayedName() + " (" + mAmount + " Max Health)");
	}

	@Override
	public @Nullable String getDisplayedName() {
		return "Blight Infection Level";
	}
}
