package com.playmonumenta.plugins.depths.abilities.prismatic;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.FastUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class Prosperity extends DepthsAbility {
	public static final String ABILITY_NAME = "Prosperity";
	public static final double[] CHANCE = {0.25, 0.45, 0.65, 0.85, 1.0, 1.5};

	public static final DepthsAbilityInfo<Prosperity> INFO =
		new DepthsAbilityInfo<>(Prosperity.class, ABILITY_NAME, Prosperity::new, DepthsTree.PRISMATIC, DepthsTrigger.PASSIVE)
			.displayItem(Material.ENDER_CHEST)
			.descriptions(Prosperity::getDescription);

	public Prosperity(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public int getExtraChoices() {
		double chance = CHANCE[mRarity - 1];
		int count = (int) chance;
		chance -= count;
		if (chance > 0 && FastUtils.RANDOM.nextDouble() < chance) {
			count++;
		}
		return count;
	}

	private static Description<Prosperity> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.add("Gain a ")
			.addPercent(a -> CHANCE[rarity - 1], CHANCE[rarity - 1], false, true)
			.add(" chance to get an extra option in ability and upgrade rewards.");
	}
}
