package com.playmonumenta.plugins.effects;

import org.bukkit.ChatColor;
import org.jetbrains.annotations.Nullable;

public class StarBlight extends FlatHealthBoost {

	public StarBlight(int duration, double amount, String modifierName) {
		super(duration, amount, modifierName);
	}

	@SuppressWarnings("deprecation")
	@Override
	public @Nullable String getSpecificDisplay() {
		return ChatColor.RED + "Blight Infection Level (" + mAmount + " Max Health)";
	}
}
