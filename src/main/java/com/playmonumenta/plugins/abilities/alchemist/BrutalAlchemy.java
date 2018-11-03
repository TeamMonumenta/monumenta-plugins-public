package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.Plugin;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Player;

public class BrutalAlchemy extends Ability {
	private static final int BRUTAL_ALCHEMY_DAMAGE_1 = 3;
	private static final int BRUTAL_ALCHEMY_DAMAGE_2 = 5;
	private static final int BRUTAL_ALCHEMY_WITHER_1_DURATION = 4 * 20;
	private static final int BRUTAL_ALCHEMY_WITHER_2_DURATION = 6 * 20;

	public BrutalAlchemy(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 5;
		mInfo.specId = -1;
		mInfo.scoreboardId = "BrutalAlchemy";
	}
}