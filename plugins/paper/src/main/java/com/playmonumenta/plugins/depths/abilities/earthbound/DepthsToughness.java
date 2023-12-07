package com.playmonumenta.plugins.depths.abilities.earthbound;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

public class DepthsToughness extends DepthsAbility {

	public static final String ABILITY_NAME = "Toughness";
	public static final String TOUGHNESS_MODIFIER_NAME = "ToughnessPercentHealthModifier";
	public static final double[] PERCENT_MAX_HEALTH = {.10, .125, .15, .175, .20, .25};

	public static final DepthsAbilityInfo<DepthsToughness> INFO =
		new DepthsAbilityInfo<>(DepthsToughness.class, ABILITY_NAME, DepthsToughness::new, DepthsTree.EARTHBOUND, DepthsTrigger.PASSIVE)
			.remove(player -> EntityUtils.removeAttribute(player, Attribute.GENERIC_MAX_HEALTH, TOUGHNESS_MODIFIER_NAME))
			.displayItem(Material.CRYING_OBSIDIAN)
			.descriptions(DepthsToughness::getDescription)
			.singleCharm(false);

	private final double mPercentMaxHealth;

	public DepthsToughness(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mPercentMaxHealth = PERCENT_MAX_HEALTH[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.TOUGHNESS_MAX_HEALTH.mEffectName);
		EntityUtils.addAttribute(player, Attribute.GENERIC_MAX_HEALTH,
			new AttributeModifier(TOUGHNESS_MODIFIER_NAME, mPercentMaxHealth, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
	}

	private static Description<DepthsToughness> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<DepthsToughness>(color)
			.add("Gain ")
			.addPercent(a -> a.mPercentMaxHealth, PERCENT_MAX_HEALTH[rarity - 1], false, true)
			.add(" max health.");
	}
}
