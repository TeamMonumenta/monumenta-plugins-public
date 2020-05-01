package com.playmonumenta.plugins.abilities.delves.cursed;

import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.delves.StatMultiplier;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * RUTHLESS: You take x2 more damage and deal x2 less damage.
 */

public class Ruthless extends StatMultiplier {

	private static final int RUTHLESS_CHALLENGE_SCORE = 11;
	private static final double RUTHLESS_DAMAGE_DEALT_MULTIPLIER = 0.5;
	private static final double RUTHLESS_DAMAGE_TAKEN_MULTIPLIER = 2;

	public Ruthless(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player,
				ChatColor.GRAY + "Weathering injury that shouldn't be possible, they return with " + ChatColor.RED + ChatColor.BOLD + "RUTHLESS" + ChatColor.GRAY + " strikes of their own.",
				RUTHLESS_DAMAGE_DEALT_MULTIPLIER, RUTHLESS_DAMAGE_TAKEN_MULTIPLIER, RUTHLESS_DAMAGE_TAKEN_MULTIPLIER);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.isDelveChallengeActive(player, RUTHLESS_CHALLENGE_SCORE);
	}

}
