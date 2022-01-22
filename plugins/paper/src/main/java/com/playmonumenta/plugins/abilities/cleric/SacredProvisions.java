package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SacredProvisions extends Ability {

	public SacredProvisions(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Sacred Provisions");
		mInfo.mScoreboardId = "SacredProvisions";
		mInfo.mShorthandName = "SP";
		mInfo.mDescriptions.add("Players within 30 blocks of a cleric have a 20% chance to not consume food, potions, arrows, or durability when the respective item is used. Does not stack with multiple clerics.");
		mInfo.mDescriptions.add("The chance is increased to 40%.");
		mDisplayItem = new ItemStack(Material.COOKED_BEEF, 1);
	}
}
