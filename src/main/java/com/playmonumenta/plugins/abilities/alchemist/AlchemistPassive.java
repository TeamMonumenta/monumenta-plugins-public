package com.playmonumenta.plugins.abilities.alchemist;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;

public class AlchemistPassive extends Ability {

	public AlchemistPassive(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 5;
		mInfo.specId = -1;
	}
}
