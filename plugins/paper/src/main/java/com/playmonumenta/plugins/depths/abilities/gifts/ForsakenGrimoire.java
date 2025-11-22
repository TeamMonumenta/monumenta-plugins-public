package com.playmonumenta.plugins.depths.abilities.gifts;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsRarity;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class ForsakenGrimoire extends DepthsAbility {
	public static final String ABILITY_NAME = "Forsaken Grimoire";

	public static final DepthsAbilityInfo<ForsakenGrimoire> INFO =
		new DepthsAbilityInfo<>(ForsakenGrimoire.class, ABILITY_NAME, ForsakenGrimoire::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.BOOK)
			.gain(ForsakenGrimoire::gain)
			.descriptions(ForsakenGrimoire::getDescription);

	public ForsakenGrimoire(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static void gain(Player player) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null) {
			return;
		}
		dp.addReward(DepthsRoomType.DepthsRewardType.GRIMOIRE);
	}

	private static Description<ForsakenGrimoire> getDescription() {
		return new DescriptionBuilder<>(() -> INFO).add("Select an active ability from your trees and obtain it at ")
			.add(DepthsRarity.RARE.getDisplay())
			.add(" rarity.");
	}
}
