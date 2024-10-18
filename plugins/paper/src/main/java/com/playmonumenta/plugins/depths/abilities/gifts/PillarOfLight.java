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

public class PillarOfLight extends DepthsAbility {
	public static final String ABILITY_NAME = "Pillar Of Light";
	public static final int REVIVE_RADIUS_MULTIPLIER = 3;

	public static final DepthsAbilityInfo<PillarOfLight> INFO =
		new DepthsAbilityInfo<>(PillarOfLight.class, ABILITY_NAME, PillarOfLight::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.BEACON)
			.descriptions(PillarOfLight::getDescription);

	public PillarOfLight(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static Description<PillarOfLight> getDescription() {
		return new DescriptionBuilder<PillarOfLight>().add("The radius in which you can revive teammates triples.");
	}
}
