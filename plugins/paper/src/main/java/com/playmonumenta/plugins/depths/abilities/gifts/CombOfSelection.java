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

public class CombOfSelection extends DepthsAbility {
	public static final String ABILITY_NAME = "Comb of Selection";

	public static final DepthsAbilityInfo<CombOfSelection> INFO =
		new DepthsAbilityInfo<>(CombOfSelection.class, ABILITY_NAME, CombOfSelection::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.HORN_CORAL_FAN)
			.gain(CombOfSelection::gain)
			.descriptions(CombOfSelection::getDescription);

	public CombOfSelection(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static void gain(Player player) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null) {
			return;
		}
		dp.mRerolls += 2;
	}

	private static Description<CombOfSelection> getDescription() {
		return new DescriptionBuilder<>(() -> INFO).add("Each time you spend a reroll on an ability selection, the new set of abilities is upgraded by one level. Gain two rerolls.");
	}
}
