package com.playmonumenta.plugins.abilities.cleric;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;

public class SacredProvisions extends Ability {

	public SacredProvisions(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Sacred Provisions");
		mInfo.scoreboardId = "SacredProvisions";
		mInfo.mShorthandName = "SP";
		mInfo.mDescriptions.add("Players within 30 blocks of a cleric have a 20% chance to not consume food, potions, arrows, or durability when the respective item is used. Does not stack with multiple clerics.");
		mInfo.mDescriptions.add("The chance is increased to 40%.");
	}
}
