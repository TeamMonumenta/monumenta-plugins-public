package com.playmonumenta.plugins.depths.abilities.prismatic;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsRarity;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class Abnormality extends DepthsAbility {
	public static final String ABILITY_NAME = "Abnormality";

	public static final DepthsAbilityInfo<Abnormality> INFO =
		new DepthsAbilityInfo<>(Abnormality.class, ABILITY_NAME, Abnormality::new, DepthsTree.PRISMATIC, DepthsTrigger.PASSIVE)
			.displayItem(Material.GLOW_INK_SAC)
			.gain(Abnormality::gain)
			.descriptions(Abnormality::getDescription);

	public Abnormality(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public static void gain(Player player) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null) {
			return;
		}
		int level = dp.getLevelInAbility(ABILITY_NAME);

		int[] placeholder = {100, 0, 0, 0, 0};
		DepthsManager.getInstance().getRandomAbility(player, dp, placeholder, null, true);
		dp.addReward(DepthsRoomType.DepthsRewardType.PRISMATIC, level);
		dp.addReward(DepthsRoomType.DepthsRewardType.CURSE);
		dp.mRerolls++;

		DepthsManager.getInstance().setPlayerLevelInAbility(ABILITY_NAME, player, dp, 0, false, false);
	}

	public static Description<Abnormality> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.add("Gain a random ability at ")
			.add(DepthsRarity.TWISTED.getDisplay())
			.add(" rarity, a selection of ")
			.add(DepthsTree.PRISMATIC.getNameComponent())
			.add(" abilities at ")
			.add(DepthsUtils.getRarityComponent(rarity))
			.add(" rarity, a selection of ")
			.add(DepthsTree.CURSE.getNameComponent())
			.add(" abilities, and 1 reroll. Remove this ability");
	}
}
