package com.playmonumenta.plugins.abilities.other;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;

import org.bukkit.Particle;
import org.bukkit.entity.Player;

import org.checkerframework.checker.nullness.qual.Nullable;



public class PatronWhite extends PatronParticles {
	public PatronWhite(Plugin plugin, @Nullable Player player) {
		super(
			plugin,
			player,
			Particle.CLOUD,
			"ShinyWhite",
			Constants.PATREON_TIER_2
		);
	}
}
