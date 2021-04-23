package com.playmonumenta.plugins.abilities.other;

import com.playmonumenta.plugins.Plugin;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



public class PatronGreen extends PatronParticles {
	public PatronGreen(@NotNull Plugin plugin, @Nullable Player player) {
		super(
			plugin,
			player,
			Particle.VILLAGER_HAPPY,
			"ShinyGreen",
			20
		);
	}
}