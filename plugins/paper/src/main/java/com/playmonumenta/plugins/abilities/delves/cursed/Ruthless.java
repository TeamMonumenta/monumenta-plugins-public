package com.playmonumenta.plugins.abilities.delves.cursed;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.delves.StatMultiplier;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * RUTHLESS: Mobs have x1.6 health and x1.6 damage and get pissed when you piss on their friends
 */

public class Ruthless extends StatMultiplier {

	private static final int RUTHLESS_CHALLENGE_SCORE = 11;
	private static final double RUTHLESS_MOB_HEALTH_MULTIPLIER = 1.6;
	private static final double RUTHLESS_DAMAGE_TAKEN_MULTIPLIER = 1.6;
	private static final double RUTHLESS_ABILITY_CHANCE = 0.5;

	private static final String[] RUTHLESS_ABILITY_POOL = {
		"boss_avenger"
	};

	public Ruthless(Plugin plugin, World world, Player player) {
		super(plugin, world, player,
				ChatColor.GRAY + "Weathering injury that shouldn't be possible, they return with " + ChatColor.RED + ChatColor.BOLD + "RUTHLESS" + ChatColor.GRAY + " strikes of their own.",
				RUTHLESS_DAMAGE_TAKEN_MULTIPLIER, RUTHLESS_DAMAGE_TAKEN_MULTIPLIER, RUTHLESS_MOB_HEALTH_MULTIPLIER, RUTHLESS_ABILITY_POOL, RUTHLESS_ABILITY_CHANCE);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.isDelveChallengeActive(player, RUTHLESS_CHALLENGE_SCORE);
	}

}
