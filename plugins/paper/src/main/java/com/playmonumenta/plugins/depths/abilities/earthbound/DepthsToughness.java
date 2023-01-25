package com.playmonumenta.plugins.depths.abilities.earthbound;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DepthsToughness extends DepthsAbility {

	public static final String ABILITY_NAME = "Toughness";
	public static final String TOUGHNESS_MODIFIER_NAME = "ToughnessPercentHealthModifier";
	public static final double[] PERCENT_MAX_HEALTH = {.10, .125, .15, .175, .20, .25};

	public static final DepthsAbilityInfo<DepthsToughness> INFO =
		new DepthsAbilityInfo<>(DepthsToughness.class, ABILITY_NAME, DepthsToughness::new, DepthsTree.EARTHBOUND, DepthsTrigger.PASSIVE)
			.displayItem(new ItemStack(Material.CRYING_OBSIDIAN))
			.descriptions(DepthsToughness::getDescription);

	public DepthsToughness(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (player != null) {
			EntityUtils.addAttribute(player, Attribute.GENERIC_MAX_HEALTH,
				new AttributeModifier(TOUGHNESS_MODIFIER_NAME, PERCENT_MAX_HEALTH[mRarity - 1], AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}
	}

	private static TextComponent getDescription(int rarity, TextColor color) {
		return Component.text("Gain ")
			.append(Component.text(StringUtils.multiplierToPercentage(PERCENT_MAX_HEALTH[rarity - 1]) + "%", color))
			.append(Component.text(" max health."));
	}
}
