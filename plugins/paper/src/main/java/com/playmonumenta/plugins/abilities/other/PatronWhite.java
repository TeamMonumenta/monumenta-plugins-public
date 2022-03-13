package com.playmonumenta.plugins.abilities.other;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import javax.annotation.Nullable;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;



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
