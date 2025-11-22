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
import java.util.Arrays;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class KaleidoscopicLens extends DepthsAbility {
	public static final String ABILITY_NAME = "Kaleidoscopic Lens";

	public static final DepthsAbilityInfo<KaleidoscopicLens> INFO =
		new DepthsAbilityInfo<>(KaleidoscopicLens.class, ABILITY_NAME, KaleidoscopicLens::new, DepthsTree.GIFT, DepthsTrigger.PASSIVE)
			.displayItem(Material.SPYGLASS)
			.floors(floor -> floor == 3)
			.gain(KaleidoscopicLens::gain)
			.descriptions(KaleidoscopicLens::getDescription);

	public KaleidoscopicLens(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static void gain(Player player) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null) {
			return;
		}
		dp.mEligibleTrees = Arrays.stream(DepthsTree.OWNABLE_TREES).filter(tree -> !dp.mEligibleTrees.contains(tree)).collect(Collectors.toList());
		dp.addReward(DepthsRoomType.DepthsRewardType.PRISMATIC);
		DepthsManager.getInstance().setPlayerLevelInAbility(ABILITY_NAME, player, dp, 0, false, false);
	}

	private static Description<KaleidoscopicLens> getDescription() {
		return new DescriptionBuilder<>(() -> INFO).add("Lose your current trees. Gain the trees you didn't have previously, and a ")
			.add(DepthsTree.PRISMATIC.getNameComponent())
			.add(" ability selection.");
	}
}
