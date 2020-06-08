package com.playmonumenta.plugins.abilities.delves.cursed;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.delves.StatMultiplier;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * UNYIELDING: Mobs have x1.3 speed, have x1.6 health, and deal x1.6 damage.
 */

public class Unyielding extends StatMultiplier {

	private static final int UNYIELDING_CHALLENGE_SCORE = 12;
	private static final double UNYIELDING_MOB_HEALTH_MULTIPLIER = 1.6;
	private static final double UNYIELDING_DAMAGE_TAKEN_MULTIPLIER = 1.6;
	private static final double UNYIELDING_SPEED_MULTIPLIER = 1.3;

	public Unyielding(Plugin plugin, World world, Player player) {
		super(plugin, world, player,
				ChatColor.GRAY + "Footsteps hound you, " + ChatColor.RED + ChatColor.BOLD + "UNYIELDING" + ChatColor.GRAY + " in their deadly pursuit.",
				UNYIELDING_DAMAGE_TAKEN_MULTIPLIER, UNYIELDING_DAMAGE_TAKEN_MULTIPLIER, UNYIELDING_MOB_HEALTH_MULTIPLIER, UNYIELDING_SPEED_MULTIPLIER);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.isDelveChallengeActive(player, UNYIELDING_CHALLENGE_SCORE);
	}

}
