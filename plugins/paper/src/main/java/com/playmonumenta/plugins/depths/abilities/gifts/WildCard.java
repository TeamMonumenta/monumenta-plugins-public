package com.playmonumenta.plugins.depths.abilities.gifts;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class WildCard extends DepthsAbility {
	public static final String ABILITY_NAME = "Wild Card";

	public static final DepthsAbilityInfo<WildCard> INFO =
		new DepthsAbilityInfo<>(WildCard.class, ABILITY_NAME, WildCard::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.FLOWER_BANNER_PATTERN)
			.descriptions(WildCard::getDescription);

	public WildCard(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static Description<WildCard> getDescription() {
		return new DescriptionBuilder<WildCard>().add("Every time you enter a Wildcard room, upgrade a random ability by one level.");
	}
}
