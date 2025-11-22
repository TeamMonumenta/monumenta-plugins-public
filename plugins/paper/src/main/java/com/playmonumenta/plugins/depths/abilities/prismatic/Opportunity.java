package com.playmonumenta.plugins.depths.abilities.prismatic;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class Opportunity extends DepthsAbility {
	public static final String ABILITY_NAME = "Opportunity";
	public static final int[] REROLLS = {1, 1, 2, 3, 4, 6};

	public static final DepthsAbilityInfo<Opportunity> INFO =
		new DepthsAbilityInfo<>(Opportunity.class, ABILITY_NAME, Opportunity::new, DepthsTree.PRISMATIC, DepthsTrigger.PASSIVE)
			.displayItem(Material.FROGSPAWN)
			.descriptions(Opportunity::getDescription);

	public Opportunity(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		DepthsPlayer depthsPlayer = DepthsManager.getInstance().getDepthsPlayer(player);
		if (depthsPlayer == null) {
			return;
		}

		int newRerolls = REROLLS[mRarity - 1] - depthsPlayer.mOpportunityRerolls;
		if (newRerolls > 0) {
			depthsPlayer.mRerolls += newRerolls;
			depthsPlayer.mOpportunityRerolls = REROLLS[mRarity - 1];
		}
	}

	private static Description<Opportunity> getDescription(int rarity, TextColor color) {
		//Rarity starts counting at 1 while arrays start counting at 0.
		String s = REROLLS[rarity - 1] > 1 ? "s" : "";
		return new DescriptionBuilder<>(() -> INFO, color)
			.add("Gain ")
			.add(a -> REROLLS[rarity - 1], REROLLS[rarity - 1], false, null, true)
			.add(" reroll" + s + ".");
	}
}
