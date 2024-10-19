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

public class VenomOfTheBroodmother extends DepthsAbility {
	public static final String ABILITY_NAME = "Venom of the Broodmother";

	public static final DepthsAbilityInfo<VenomOfTheBroodmother> INFO =
		new DepthsAbilityInfo<>(VenomOfTheBroodmother.class, ABILITY_NAME, VenomOfTheBroodmother::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.POTION)
			.floors(floor -> floor == 3)
			.offerable(VenomOfTheBroodmother::offerable)
			.gain(VenomOfTheBroodmother::gain)
			.descriptions(VenomOfTheBroodmother::getDescription);

	public VenomOfTheBroodmother(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static boolean offerable(Player player) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		return dp != null && dp.mNumDeaths > 0;
	}

	private static void gain(Player player) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null) {
			return;
		}
		dp.mNumDeaths = 0;
		DepthsManager.getInstance().setPlayerLevelInAbility(ABILITY_NAME, player, 0, false);
	}

	private static Description<VenomOfTheBroodmother> getDescription() {
		return new DescriptionBuilder<VenomOfTheBroodmother>().add("Restore your grave timer to the maximum.");
	}
}
