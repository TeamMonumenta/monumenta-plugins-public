package com.playmonumenta.plugins.abilities.delves.twisted;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.delves.StatMultiplier;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * ARCANIC: You deal x2 less damage and mobs get a random STRONKER ability (and are blast resistant)
 */

public class Arcanic extends StatMultiplier {

	private static final int ARCANIC_CHALLENGE_SCORE = 23;
	private static final double ARCANIC_MOB_HEALTH_MULTIPLIER = 2;
	private static final double ARCANIC_1_ABILITY_DAMAGE_TAKEN_MULTIPLIER = 1.25;
	private static final double ARCANIC_2_ABILITY_DAMAGE_TAKEN_MULTIPLIER = 2.5;
	private static final double ARCANIC_ABILITY_CHANCE = 1;

	private static final String[] ARCANIC_ABILITY_POOL = {
		"boss_rejuvenation",
		"boss_bombtossnoblockbreak",
		"boss_seekingprojectile",
		"boss_trackingprojectile",
		"boss_tpbehindtargeted",
		"boss_pulselaser",
		"boss_flamelaser",
		"boss_chargerstrong",
		"boss_chargerstrong",
		"boss_magicarrow",
		"boss_magicarrow"
	};

	public Arcanic(Plugin plugin, World world, Player player) {
		super(plugin, world, player,
				ChatColor.GRAY + "A strange feeling washes over you, whispering of " + ChatColor.DARK_RED + ChatColor.BOLD + "ARCANIC" + ChatColor.GRAY + " powers channeled within.",
				1, ServerProperties.getClassSpecializationsEnabled() ? ARCANIC_2_ABILITY_DAMAGE_TAKEN_MULTIPLIER : ARCANIC_1_ABILITY_DAMAGE_TAKEN_MULTIPLIER,
				ARCANIC_MOB_HEALTH_MULTIPLIER, ARCANIC_ABILITY_POOL, ARCANIC_ABILITY_CHANCE);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.isDelveChallengeActive(player, ARCANIC_CHALLENGE_SCORE);
	}

}
