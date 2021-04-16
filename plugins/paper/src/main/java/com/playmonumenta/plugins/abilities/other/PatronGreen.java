package com.playmonumenta.plugins.abilities.other;

import com.playmonumenta.plugins.Plugin;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;



public class PatronGreen extends PatronParticles<Object> {
	public PatronGreen(Plugin plugin, @Nullable Player player) {
		super(
			plugin,
			player,
			Particle.VILLAGER_HAPPY,
			"ShinyGreen",
			20
		);
	}
}