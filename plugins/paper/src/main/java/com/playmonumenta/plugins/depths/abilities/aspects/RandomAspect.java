package com.playmonumenta.plugins.depths.abilities.aspects;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsContent;
import com.playmonumenta.plugins.depths.DepthsRarity;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.WeaponAspectDepthsAbility;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class RandomAspect extends WeaponAspectDepthsAbility {

	public static final String ABILITY_NAME = "Mystery Box";

	public static final DepthsAbilityInfo<RandomAspect> INFO =
		new DepthsAbilityInfo<>(RandomAspect.class, ABILITY_NAME, RandomAspect::new, null, DepthsTrigger.WEAPON_ASPECT)
			.displayItem(Material.BARREL)
			.descriptions(RandomAspect::getDescription);

	public RandomAspect(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static Description<RandomAspect> getDescription() {
		if (DepthsUtils.getDepthsContent() == DepthsContent.CELESTIAL_ZENITH) {
			return new DescriptionBuilder<>(() -> INFO)
				.add("Obtain a random ability at ")
				.add(DepthsRarity.TWISTED.getDisplay())
				.add(" level.");
		} else {
			return new DescriptionBuilder<>(() -> INFO)
				.add("Obtain a random ability. Transforms into a random other aspect after defeating floor 3.");
		}
	}

}

