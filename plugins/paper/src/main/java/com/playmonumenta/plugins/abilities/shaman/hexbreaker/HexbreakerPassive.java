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

public class HexbreakerPassive extends Ability {

	public static final AbilityInfo<HexbreakerPassive> INFO =
		new AbilityInfo<>(HexbreakerPassive.class, null, HexbreakerPassive::new)
			.canUse(player -> ScoreboardUtils.getScoreboardValue(player, AbilityUtils.SCOREBOARD_SPEC_NAME).orElse(0) == Shaman.HEXBREAKER_ID);

	public HexbreakerPassive(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AuditListener.logSevere(player.getName() + " has accessed shaman abilities incorrectly, class has been reset, please report to developers.");
			AbilityUtils.resetClass(player);
		}
	}

	public static double damageBuff(Player player) {
		return (ServerProperties.getAbilityEnhancementsEnabled(player) &&
			ScoreboardUtils.getScoreboardValue(player, "Specialization").orElse(0) == Shaman.HEXBREAKER_ID) ? 1 + Shaman.HEX_PASSIVE_DAMAGE_BOOST : 1;
	}
}
