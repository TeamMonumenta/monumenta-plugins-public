package com.playmonumenta.plugins.abilities.shaman.hexbreaker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.entity.Player;

public class DestructiveExpertise extends Ability {
	public static final double DAMAGE_BOOST = 0.18;

	public static final AbilityInfo<DestructiveExpertise> INFO =
		new AbilityInfo<>(DestructiveExpertise.class, null, DestructiveExpertise::new)
			.canUse(player -> ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_SPEC_NAME).orElse(0) == Shaman.HEXBREAKER_ID);

	public DestructiveExpertise(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AbilityUtils.resetClass(player);
		}
	}

	public static double damageBuff(Player player) {
		return (ServerProperties.getAbilityEnhancementsEnabled(player) &&
			ScoreboardUtils.getScoreboardValue(player, "Specialization").orElse(0) == Shaman.HEXBREAKER_ID) ? 1 + DAMAGE_BOOST : 1;
	}
}
