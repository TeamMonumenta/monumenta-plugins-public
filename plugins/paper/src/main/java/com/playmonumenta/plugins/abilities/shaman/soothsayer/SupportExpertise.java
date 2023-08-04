package com.playmonumenta.plugins.abilities.shaman.soothsayer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.entity.Player;

public class SupportExpertise extends Ability {
	public static final double DAMAGE_BOOST = 0.04;

	public static final String CHARM_RADIUS = "Support Expertise Radius";

	public static final int RADIUS = 12;
	public static final double SELF_BOOST = 0.02;

	public static final AbilityInfo<SupportExpertise> INFO =
		new AbilityInfo<>(SupportExpertise.class, null, SupportExpertise::new)
			.canUse(player -> ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_SPEC_NAME).orElse(0) == Shaman.SOOTHSAYER_ID);

	public SupportExpertise(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public static double damageBuff(Player player) {
		double damageBoost = AbilityUtils.getEffectiveTotalSpecPoints(player) * DAMAGE_BOOST;
		return (ServerProperties.getClassSpecializationsEnabled(player) &&
			ScoreboardUtils.getScoreboardValue(player, "Specialization").orElse(0) == Shaman.SOOTHSAYER_ID) ? 1 + damageBoost : 1;
	}
}
