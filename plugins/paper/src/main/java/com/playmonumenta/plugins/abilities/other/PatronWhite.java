package com.playmonumenta.plugins.abilities.other;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;



public class PatronWhite extends PatronParticles {
	public PatronWhite(Plugin plugin, @Nullable Player player) {
		super(
			plugin,
			player,
			Particle.REDSTONE,
			new Particle.DustOptions(Color.WHITE, 1f),
			"ShinyWhite",
			Constants.PATREON_TIER_2
		);
	}
}
