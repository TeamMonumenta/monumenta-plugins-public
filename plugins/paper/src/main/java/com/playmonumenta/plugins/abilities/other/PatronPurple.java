package com.playmonumenta.plugins.abilities.other;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;

import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



public class PatronPurple extends PatronParticles {
	public PatronPurple(@NotNull Plugin plugin, @Nullable Player player) {
		super(
			plugin,
			player,
			Particle.DRAGON_BREATH,
			"ShinyPurple",
			Constants.PATREON_TIER_3
		);
	}
}