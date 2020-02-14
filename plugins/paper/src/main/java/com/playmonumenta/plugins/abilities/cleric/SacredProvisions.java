package com.playmonumenta.plugins.abilities.cleric;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;

public class SacredProvisions extends Ability {

	public SacredProvisions(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Sacred Provisions");
		mInfo.scoreboardId = "SacredProvisions";
	}
}
