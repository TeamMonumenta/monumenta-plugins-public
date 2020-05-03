package com.playmonumenta.plugins.abilities.delves.cursed;

import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.delves.StatMultiplier;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * ARCANIC: You deal x1.6 less damage and half of mobs get a random STRONK ability
 */

public class Mystic extends StatMultiplier {

	private static final int MYSTIC_CHALLENGE_SCORE = 13;
	private static final double MYSTIC_DAMAGE_DEALT_MULTIPLIER = 0.625;
	private static final double MYSTIC_1_ABILITY_DAMAGE_TAKEN_MULTIPLIER = 1;
	private static final double MYSTIC_2_ABILITY_DAMAGE_TAKEN_MULTIPLIER = 2;
	private static final double MYSTIC_ABILITY_CHANCE = 0.5;

	private static final String[] MYSTIC_ABILITY_POOL = {
			" boss_pulselaser",
			" boss_flamelaser",
			" boss_chargerstrong",
			" boss_flamenova",
			" boss_frostnova"
	};

	public Mystic(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player,
				ChatColor.GRAY + "A strange feeling washes over you, whispering of " + ChatColor.RED + ChatColor.BOLD + "MYSTIC" + ChatColor.GRAY + " powers channeled within.",
				MYSTIC_DAMAGE_DEALT_MULTIPLIER, 1,
				ServerProperties.getClassSpecializationsEnabled() ? MYSTIC_2_ABILITY_DAMAGE_TAKEN_MULTIPLIER : MYSTIC_1_ABILITY_DAMAGE_TAKEN_MULTIPLIER,
				MYSTIC_ABILITY_POOL, MYSTIC_ABILITY_CHANCE);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.isDelveChallengeActive(player, MYSTIC_CHALLENGE_SCORE);
	}

}
