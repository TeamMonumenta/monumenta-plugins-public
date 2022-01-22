package com.playmonumenta.plugins.depths.abilities.earthbound;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public class DepthsToughness extends DepthsAbility {

	public static final String ABILITY_NAME = "Toughness";
	public static final String TOUGHNESS_MODIFIER_NAME = "ToughnessPercentHealthModifier";
	public static final double[] PERCENT_MAX_HEALTH = {.10, .125, .15, .175, .20, .25};

	public DepthsToughness(Plugin plugin, Player player) {
		super(plugin, player, ABILITY_NAME);
		mDisplayItem = Material.CRYING_OBSIDIAN;
		mTree = DepthsTree.EARTHBOUND;

		if (player != null) {
			EntityUtils.addAttribute(player, Attribute.GENERIC_MAX_HEALTH,
			new AttributeModifier(TOUGHNESS_MODIFIER_NAME, PERCENT_MAX_HEALTH[mRarity - 1], AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
	}

	@Override
	public String getDescription(int rarity) {
		return "Gain " + DepthsUtils.getRarityColor(rarity) + PERCENT_MAX_HEALTH[rarity - 1] * 100 + "%" + ChatColor.WHITE + " max health.";
	}

	@Override
	public DepthsTree getDepthsTree() {
		return DepthsTree.EARTHBOUND;
	}
}
