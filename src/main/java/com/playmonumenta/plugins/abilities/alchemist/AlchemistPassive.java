package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.Plugin;

import java.util.Random;

import org.bukkit.entity.Player;
import org.bukkit.World;

public class AlchemistPassive extends Ability {

	public AlchemistPassive(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 5;
		mInfo.specId = -1;
	}
}