package com.playmonumenta.plugins.abilities.other;

import com.playmonumenta.plugins.Plugin;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;



public class PatronWhite extends PatronParticles<Object> {
	public PatronWhite(Plugin plugin, @Nullable Player player) {
		super(
			plugin,
			player,
			Particle.CLOUD,
			"ShinyWhite",
			5
		);
	}
}