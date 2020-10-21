package com.playmonumenta.plugins.abilities.delves.twisted;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.delves.StatMultiplier;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class Relentless extends StatMultiplier {

	public static final String MESSAGE = ChatColor.GRAY + "Footsteps hound you, " + ChatColor.DARK_RED + ChatColor.BOLD + "RELENTLESS" + ChatColor.GRAY + " in their deadly pursuit.";
	public static final int SCORE = 22;

	private static final double RELENTLESS_MOB_HEALTH_MULTIPLIER = 1.6;
	private static final double RELENTLESS_DAMAGE_TAKEN_MULTIPLIER = 2;
	private static final double RELENTLESS_SPEED_MULTIPLIER = 1.4;

	public Relentless(Plugin plugin, Player player) {
		super(plugin, player, RELENTLESS_DAMAGE_TAKEN_MULTIPLIER, RELENTLESS_DAMAGE_TAKEN_MULTIPLIER, RELENTLESS_MOB_HEALTH_MULTIPLIER, RELENTLESS_SPEED_MULTIPLIER);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.isDelveChallengeActive(player, SCORE);
	}

}
