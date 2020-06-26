package com.playmonumenta.plugins.abilities.delves.cursed;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.delves.StatMultiplier;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * MYSTIC: Mobs have x1.6 health and 60% of mobs get a random STRONK ability
 */
public class Mystic extends StatMultiplier {

	public static final String MESSAGE = ChatColor.GRAY + "A strange feeling washes over you, whispering of " + ChatColor.RED + ChatColor.BOLD + "MYSTIC" + ChatColor.GRAY + " powers channeled within.";
	public static final int SCORE = 13;

	private static final double MYSTIC_MOB_HEALTH_MULTIPLIER = 1.6;
	private static final double MYSTIC_1_ABILITY_DAMAGE_TAKEN_MULTIPLIER = 0.8;
	private static final double MYSTIC_2_ABILITY_DAMAGE_TAKEN_MULTIPLIER = 1.6;
	private static final double MYSTIC_ABILITY_CHANCE = 0.6;

	private static final String[] MYSTIC_ABILITY_POOL = {
			"boss_pulselaser",
			"boss_flamelaser",
			"boss_chargerstrong",
			"boss_chargerstrong",
			"boss_magicarrow",
			"boss_magicarrow"
	};

	public Mystic(Plugin plugin, World world, Player player) {
		super(plugin, world, player,
				1, ServerProperties.getClassSpecializationsEnabled() ? MYSTIC_2_ABILITY_DAMAGE_TAKEN_MULTIPLIER : MYSTIC_1_ABILITY_DAMAGE_TAKEN_MULTIPLIER,
				MYSTIC_MOB_HEALTH_MULTIPLIER, MYSTIC_ABILITY_POOL, MYSTIC_ABILITY_CHANCE);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.isDelveChallengeActive(player, SCORE);
	}

}
