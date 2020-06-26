package com.playmonumenta.plugins.abilities.delves.cursed;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.delves.StatMultiplier;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * RUTHLESS: Mobs have x1.35 health and get pissed when you piss on their friends
 */

public class Ruthless extends StatMultiplier {

	public static final String MESSAGE = ChatColor.GRAY + "With each one fallen, another returns with " + ChatColor.RED + ChatColor.BOLD + "MERCILESS" + ChatColor.GRAY + " resolve.";
	public static final int SCORE = 11;

	private static final double MERCILESS_MOB_HEALTH_MULTIPLIER = 1.35;
	private static final double MERCILESS_MOB_DAMAGE_MULTIPLIER = 1.35;
	private static final double RUTHLESS_ABILITY_CHANCE = 0.5;

	private static final String[] RUTHLESS_ABILITY_POOL = {
		"boss_avenger"
	};

	public Ruthless(Plugin plugin, World world, Player player) {
		super(plugin, world, player, MERCILESS_MOB_DAMAGE_MULTIPLIER, MERCILESS_MOB_DAMAGE_MULTIPLIER, MERCILESS_MOB_HEALTH_MULTIPLIER,
				RUTHLESS_ABILITY_POOL, RUTHLESS_ABILITY_CHANCE);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.isDelveChallengeActive(player, SCORE);
	}

}
