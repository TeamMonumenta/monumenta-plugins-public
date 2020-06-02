package com.playmonumenta.plugins.abilities.delves.twisted;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.delves.StatMultiplier;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * RELENTLESS: Mobs have x1.5 speed, have x2 health, and deal x2 damage.
 */

public class Relentless extends StatMultiplier {

	private static final int RELENTLESS_CHALLENGE_SCORE = 22;
	private static final double RELENTLESS_DAMAGE_DEALT_MULTIPLIER = 0.5;
	private static final double RELENTLESS_DAMAGE_TAKEN_MULTIPLIER = 2;
	private static final double RELENTLESS_SPEED_MULTIPLIER = 1.5;

	public Relentless(Plugin plugin, World world, Player player) {
		super(plugin, world, player,
				ChatColor.GRAY + "Footsteps hound you, " + ChatColor.DARK_RED + ChatColor.BOLD + "RELENTLESS" + ChatColor.GRAY + " in their deadly pursuit.",
				RELENTLESS_DAMAGE_DEALT_MULTIPLIER, RELENTLESS_DAMAGE_TAKEN_MULTIPLIER, RELENTLESS_DAMAGE_TAKEN_MULTIPLIER, RELENTLESS_SPEED_MULTIPLIER);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.isDelveChallengeActive(player, RELENTLESS_CHALLENGE_SCORE);
	}

}
