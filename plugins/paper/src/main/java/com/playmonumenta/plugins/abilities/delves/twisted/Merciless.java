package com.playmonumenta.plugins.abilities.delves.twisted;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.delves.StatMultiplier;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class Merciless extends StatMultiplier {

	public static final String MESSAGE = ChatColor.GRAY + "With each one fallen, another returns with " + ChatColor.DARK_RED + ChatColor.BOLD + "MERCILESS" + ChatColor.GRAY + " resolve.";
	public static final int SCORE = 21;

	private static final double MERCILESS_MOB_HEALTH_MULTIPLIER = 1.6;
	private static final double MERCILESS_MOB_DAMAGE_MULTIPLIER = 2;
	private static final double RUTHLESS_ABILITY_CHANCE = 1;

	private static final String[] RUTHLESS_ABILITY_POOL = {
		"boss_avenger"
	};

	public Merciless(Plugin plugin, World world, Player player) {
		super(plugin, world, player, MERCILESS_MOB_DAMAGE_MULTIPLIER, MERCILESS_MOB_DAMAGE_MULTIPLIER, MERCILESS_MOB_HEALTH_MULTIPLIER,
				RUTHLESS_ABILITY_POOL, RUTHLESS_ABILITY_CHANCE);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.isDelveChallengeActive(player, SCORE);
	}

}
