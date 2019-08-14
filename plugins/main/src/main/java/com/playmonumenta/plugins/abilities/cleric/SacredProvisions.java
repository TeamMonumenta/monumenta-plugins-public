package com.playmonumenta.plugins.abilities.cleric;

import java.util.Random;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;

import org.bukkit.World;
import org.bukkit.entity.Player;

public class SacredProvisions extends Ability {

	public SacredProvisions(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "SacredProvisions";
	}
}