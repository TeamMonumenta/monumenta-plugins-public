package com.playmonumenta.plugins.abilities.delves.twisted;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.delves.StatMultiplier;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * RELENTLESS: Mobs have x1.4 speed, have x2 health, and deal x2 damage.
 */

public class Relentless extends StatMultiplier {

	public static final String MESSAGE = ChatColor.GRAY + "Footsteps hound you, " + ChatColor.DARK_RED + ChatColor.BOLD + "RELENTLESS" + ChatColor.GRAY + " in their deadly pursuit.";
	public static final int SCORE = 22;

	private static final double RELENTLESS_MOB_HEALTH_MULTIPLIER = 2;
	private static final double RELENTLESS_DAMAGE_TAKEN_MULTIPLIER = 2;
	private static final double RELENTLESS_SPEED_MULTIPLIER = 1.4;

	public Relentless(Plugin plugin, World world, Player player) {
		super(plugin, world, player, RELENTLESS_DAMAGE_TAKEN_MULTIPLIER, RELENTLESS_DAMAGE_TAKEN_MULTIPLIER, RELENTLESS_MOB_HEALTH_MULTIPLIER, RELENTLESS_SPEED_MULTIPLIER);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.isDelveChallengeActive(player, SCORE);
	}

}
