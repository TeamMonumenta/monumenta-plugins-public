package com.playmonumenta.plugins.abilities.other;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import javax.annotation.Nullable;
import org.bukkit.Particle;
import org.bukkit.entity.Player;



public class PatronGreen extends PatronParticles {
	public PatronGreen(Plugin plugin, @Nullable Player player) {
		super(
			plugin,
			player,
			Particle.VILLAGER_HAPPY,
			"ShinyGreen",
			Constants.PATREON_TIER_4
		);
	}
}
