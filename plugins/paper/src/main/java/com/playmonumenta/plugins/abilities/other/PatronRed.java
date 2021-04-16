package com.playmonumenta.plugins.abilities.other;

import com.playmonumenta.plugins.Plugin;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;



public class PatronRed extends PatronParticles<Particle.DustOptions> {
	public PatronRed(Plugin plugin, @Nullable Player player) {
		super(
			plugin,
			player,
			Particle.REDSTONE,
			new Particle.DustOptions(Color.RED, 1f),
			"ShinyRed",
			30
		);
	}
}