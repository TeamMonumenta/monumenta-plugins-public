package com.playmonumenta.plugins.abilities.delves.twisted;

import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.delves.StatMultiplier;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * MERCILESS: You take x3 more damage and deal x3 less damage.
 */

public class Merciless extends StatMultiplier {

	private static final int MERCILESS_CHALLENGE_SCORE = 21;
	private static final double MERCILESS_DAMAGE_DEALT_MULTIPLIER = 0.333;
	private static final double MERCILESS_DAMAGE_TAKEN_MULTIPLIER = 3;

	public Merciless(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player,
				ChatColor.GRAY + "Weathering injury that shouldn't be possible, they return with " + ChatColor.DARK_RED + ChatColor.BOLD + "MERCILESS" + ChatColor.GRAY + " strikes of their own.",
				MERCILESS_DAMAGE_DEALT_MULTIPLIER, MERCILESS_DAMAGE_TAKEN_MULTIPLIER, MERCILESS_DAMAGE_TAKEN_MULTIPLIER);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.isDelveChallengeActive(player, MERCILESS_CHALLENGE_SCORE);
	}

}
