package com.playmonumenta.plugins.abilities.other;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.player.PlayerData;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;


public class PatronRed extends PatronParticles {

	public static final AbilityInfo<PatronRed> INFO =
		new AbilityInfo<>(PatronRed.class, null, PatronRed::new)
			.canUse(player -> {
				int particleScore = ScoreboardUtils.getScoreboardValue(player, "ShinyRed").orElse(0);
				return (
					particleScore > 0
						&& PlayerData.getPatreonDollars(player) >= Constants.PATREON_TIER_3
						&& !PremiumVanishIntegration.isInvisibleOrSpectator(player)
				);
			})
			.ignoresSilence(true);

	public PatronRed(Plugin plugin, Player player) {
		super(
			plugin,
			player,
			Particle.REDSTONE,
			new Particle.DustOptions(Color.RED, 1f),
			INFO
		);
	}
}
