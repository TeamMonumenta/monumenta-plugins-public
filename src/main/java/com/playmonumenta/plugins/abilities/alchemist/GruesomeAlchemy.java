package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.Plugin;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Player;

public class GruesomeAlchemy extends Ability {
	private static final int GRUESOME_ALCHEMY_DURATION = 8 * 20;
	private static final int GRUESOME_ALCHEMY_VULN = 4; //25%
	private static final int GRUESOME_ALCHEMY_SLOW = 2;

	public GruesomeAlchemy(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 5;
		mInfo.specId = -1;
		mInfo.scoreboardId = "GruesomeAlchemy";
	}
}