package com.playmonumenta.plugins.depths.abilities.gifts;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class CelestialSurprise extends DepthsAbility {
	public static final String ABILITY_NAME = "Celestial Surprise";

	public static final DepthsAbilityInfo<CelestialSurprise> INFO =
		new DepthsAbilityInfo<>(CelestialSurprise.class, ABILITY_NAME, CelestialSurprise::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.CAKE)
			.floors(floor -> floor == 3)
			.gain(CelestialSurprise::gain)
			.descriptions(CelestialSurprise::getDescription);

	public CelestialSurprise(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static void gain(Player player) {
		DepthsManager dm = DepthsManager.getInstance();
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null) {
			return;
		}
		int[] chances = {100, 0, 0, 0, 0};
		dm.getRandomAbility(player, dp, chances, DepthsTree.GIFT, false);
		dm.getRandomAbility(player, dp, chances, DepthsTree.GIFT, false);
		dm.setPlayerLevelInAbility(ABILITY_NAME, player, dp, 0, false, false);
	}

	private static Description<CelestialSurprise> getDescription() {
		return new DescriptionBuilder<>(() -> INFO).add("Gain two celestial gifts at random.");
	}
}
