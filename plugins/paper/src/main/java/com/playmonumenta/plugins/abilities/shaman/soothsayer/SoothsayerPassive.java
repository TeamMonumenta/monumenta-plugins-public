package com.playmonumenta.plugins.abilities.shaman.soothsayer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.entity.Player;

public class SoothsayerPassive extends Ability {

	public static final AbilityInfo<SoothsayerPassive> INFO =
		new AbilityInfo<>(SoothsayerPassive.class, null, SoothsayerPassive::new)
			.canUse(player -> ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_SPEC_NAME).orElse(0) == Shaman.SOOTHSAYER_ID);

	public SoothsayerPassive(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public static double damageBuff(Player player) {
		return (ServerProperties.getClassSpecializationsEnabled(player) &&
			ScoreboardUtils.getScoreboardValue(player, "Specialization").orElse(0) == Shaman.SOOTHSAYER_ID) ? 1 + Shaman.SOOTH_PASSIVE_DAMAGE_BOOST : 1;
	}

	public static double healingBuff(Player player) {
		return (ServerProperties.getClassSpecializationsEnabled(player) &&
			ScoreboardUtils.getScoreboardValue(player, "Specialization").orElse(0) == Shaman.SOOTHSAYER_ID) ? 1 + Shaman.SOOTH_PASSIVE_HEAL_PERCENT : 1;
	}
}
