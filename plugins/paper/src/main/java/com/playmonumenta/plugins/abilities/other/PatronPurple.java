package com.playmonumenta.plugins.abilities.other;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import javax.annotation.Nullable;
import org.bukkit.Particle;
import org.bukkit.entity.Player;



public class PatronPurple extends PatronParticles {
	public PatronPurple(Plugin plugin, @Nullable Player player) {
		super(
			plugin,
			player,
			Particle.DRAGON_BREATH,
			"ShinyPurple",
			Constants.PATREON_TIER_3
		);
	}
}
