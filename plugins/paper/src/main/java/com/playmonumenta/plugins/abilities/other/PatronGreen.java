package com.playmonumenta.plugins.abilities.other;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.player.PlayerData;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import javax.annotation.Nullable;
import org.bukkit.Particle;
import org.bukkit.entity.Player;


public class PatronGreen extends PatronParticles {

	public static final AbilityInfo<PatronGreen> INFO =
		new AbilityInfo<>(PatronGreen.class, null, PatronGreen::new)
			.canUse(player -> {
				int particleScore = ScoreboardUtils.getScoreboardValue(player, "ShinyGreen").orElse(0);
				return (
					particleScore > 0
						&& PlayerData.getPatreonDollars(player) >= Constants.PATREON_TIER_4
						&& !PremiumVanishIntegration.isInvisibleOrSpectator(player)
				);
			});

	public PatronGreen(Plugin plugin, @Nullable Player player) {
		super(
			plugin,
			player,
			Particle.VILLAGER_HAPPY,
			null,
			INFO
		);
	}
}
