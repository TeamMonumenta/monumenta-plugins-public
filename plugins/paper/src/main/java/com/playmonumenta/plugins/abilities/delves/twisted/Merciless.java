package com.playmonumenta.plugins.abilities.delves.twisted;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.delves.StatMultiplier;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * MERCILESS: Mobs have x2 health and x2 damage and get really pissed when you piss on their friends.
 */

public class Merciless extends StatMultiplier {

	private static final int MERCILESS_CHALLENGE_SCORE = 21;
	private static final double MERCILESS_MOB_HEALTH_MULTIPLIER = 2;
	private static final double MERCILESS_DAMAGE_TAKEN_MULTIPLIER = 2;
	private static final double RUTHLESS_ABILITY_CHANCE = 1;

	private static final String[] RUTHLESS_ABILITY_POOL = {
		"boss_avenger"
	};

	public Merciless(Plugin plugin, World world, Player player) {
		super(plugin, world, player,
				ChatColor.GRAY + "Weathering injury that shouldn't be possible, they return with " + ChatColor.DARK_RED + ChatColor.BOLD + "MERCILESS" + ChatColor.GRAY + " strikes of their own.",
				MERCILESS_DAMAGE_TAKEN_MULTIPLIER, MERCILESS_DAMAGE_TAKEN_MULTIPLIER, MERCILESS_MOB_HEALTH_MULTIPLIER, RUTHLESS_ABILITY_POOL, RUTHLESS_ABILITY_CHANCE);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.isDelveChallengeActive(player, MERCILESS_CHALLENGE_SCORE);
	}

}
