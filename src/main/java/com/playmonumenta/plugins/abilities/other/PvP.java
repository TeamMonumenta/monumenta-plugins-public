package com.playmonumenta.plugins.abilities.other;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;

/*
 * This is a utility "ability" that makes it easy to see whether a player has PvP enabled or not
 */
public class PvP extends Ability {

	public PvP(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
	}

	@Override
	public boolean canUse(Player player) {
		return player.getScoreboardTags().contains("pvp");
	}
}
