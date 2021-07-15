package com.playmonumenta.plugins.depths.abilities.dawnbringer;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;

import net.md_5.bungee.api.ChatColor;

public class Enlightenment extends DepthsAbility {

	//Technical implementation of this ability is handled in the depths listener, so that any member of the party can benefit from it

	public static final String ABILITY_NAME = "Enlightenment";
	public static final double[] XP_MULTIPLIER = {1.25, 1.32, 1.38, 1.44, 1.5};


	public Enlightenment(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.EXPERIENCE_BOTTLE;
		mTree = DepthsTree.SUNLIGHT;
	}

	@Override
	public String getDescription(int rarity) {
		return "All players in your party gain " + DepthsUtils.getRarityColor(rarity) + XP_MULTIPLIER[rarity - 1] + "x" + ChatColor.WHITE + " experience. Does not stack if multiple players in the party have the skill.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.SUNLIGHT;
	}

	@Override
	public DepthsTrigger getTrigger() {
		return DepthsTrigger.PASSIVE;
	}
}

