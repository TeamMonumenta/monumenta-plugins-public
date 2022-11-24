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


public class PatronPurple extends PatronParticles {

	public static final AbilityInfo<PatronPurple> INFO =
		new AbilityInfo<>(PatronPurple.class, null, PatronPurple::new)
			.canUse(player -> {
				int particleScore = ScoreboardUtils.getScoreboardValue(player, "ShinyPurple").orElse(0);
				return (
					particleScore > 0
						&& PlayerData.getPatreonDollars(player) >= Constants.PATREON_TIER_3
						&& !PremiumVanishIntegration.isInvisibleOrSpectator(player)
				);
			})
			.ignoresSilence(true);

	public PatronPurple(Plugin plugin, @Nullable Player player) {
		super(
			plugin,
			player,
			Particle.DRAGON_BREATH,
			null,
			INFO
		);
	}
}
