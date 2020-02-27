package com.playmonumenta.plugins.abilities.alchemist.apothecary;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;

/*
 * WARDING REMEDY:
 * You and allies in a 12 block radius gain 1 absorption health
 * every 5 / 4 seconds, up to 6 absorption health. When an affected
 * player has 4 or more absorption health from any source, gain an
 * additional 10% / 20% damage on melee and ranged attacks.
 */

public class WardingRemedy extends Ability {

	public WardingRemedy(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Warding Remedy");
		mInfo.scoreboardId = "WardingRemedy";
		mInfo.mShorthandName = "WR";
		mInfo.mDescriptions.add("You and allies in a 12 block radius gain 1 absorption health every 5 seconds, up to 6 absorption health. When an affected player has 4 or more absorption health from any source, gain an additional 10% damage on melee and ranged attacks.");
		mInfo.mDescriptions.add("Gain 1 absorption health every 4 seconds instead, and the damage bonus is increased to 20%.");
	}

}
