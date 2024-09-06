package com.playmonumenta.plugins.effects.hexfall;

import com.playmonumenta.plugins.effects.Effect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

public class DeathImmunity extends Effect {
	public static final String GENERIC_NAME = "Death Immunity";
	public static final String effectId = "deathImmunity";

	public DeathImmunity(int duration) {
		super(duration, effectId);
	}

	@Override
	public boolean shouldDeleteOnLogout() {
		return true;
	}

	@Override
	public @Nullable Component getSpecificDisplay() {
		return Component.text("Death Immunity", NamedTextColor.GREEN);
	}

	@Override
	public String toString() {
		return String.format(GENERIC_NAME + ":%d", this.getDuration());
	}
}
