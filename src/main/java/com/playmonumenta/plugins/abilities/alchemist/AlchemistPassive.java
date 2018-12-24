package com.playmonumenta.plugins.abilities.alchemist;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class AlchemistPassive extends Ability {

	public AlchemistPassive(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Class") == 5;
	}

	//TODO: THIS CLASS DOES NOTHING!
}
