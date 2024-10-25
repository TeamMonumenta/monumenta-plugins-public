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
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class PoetsQuill extends DepthsAbility {
	public static final String ABILITY_NAME = "Poet's Quill";

	public static final DepthsAbilityInfo<PoetsQuill> INFO =
		new DepthsAbilityInfo<>(PoetsQuill.class, ABILITY_NAME, PoetsQuill::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.WRITABLE_BOOK)
			.floors(floor -> floor == 2)
			.offerable(p -> {
				DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(p);
				if (dp == null) {
					return false;
				}
				return !dp.mEligibleTrees.isEmpty() && dp.mEligibleTrees.size() < 7;
			})
			.gain(PoetsQuill::gain)
			.descriptions(PoetsQuill::getDescription);

	public PoetsQuill(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static void gain(Player player) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null) {
			return;
		}
		dp.mEarnedRewards.add(DepthsRoomType.DepthsRewardType.POETS);
	}

	private static Description<PoetsQuill> getDescription() {
		return new DescriptionBuilder<PoetsQuill>().add("Remove a tree (and all abilities related to it) and replace it with another tree of your choice.");
	}
}
