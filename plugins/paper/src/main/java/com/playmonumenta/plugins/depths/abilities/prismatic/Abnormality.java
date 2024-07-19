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
import com.playmonumenta.plugins.depths.guis.AbstractDepthsRewardGUI;
import com.playmonumenta.plugins.guis.Gui;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class Abnormality extends DepthsAbility {
	public static final String ABILITY_NAME = "Abnormality";

	public static final DepthsAbilityInfo<Abnormality> INFO =
		new DepthsAbilityInfo<>(Abnormality.class, ABILITY_NAME, Abnormality::new, DepthsTree.PRISMATIC, DepthsTrigger.PASSIVE)
			.displayItem(Material.GLOW_INK_SAC)
			.descriptions(Abnormality::getDescription);

	public Abnormality(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null || dp.mCurrentAbnormality) {
			return;
		}
		dp.mCurrentAbnormality = true;
		// Delay to end of tick to avoid potential CME - not sure if this is actually an issue but want to be careful
		Bukkit.getScheduler().runTask(plugin, () -> {
			// Give abilities first - in particular, we don't want to give the prismatic before taking away abnormality
			int[] chances = {0, 0, 0, 0, 0};
			chances[mRarity - 1] = 100;
			DepthsManager.getInstance().getRandomAbility(player, dp, chances, DepthsTree.PRISMATIC, false);

			int[] placeholder = {100, 0, 0, 0, 0};
			DepthsManager.getInstance().getRandomAbility(player, dp, placeholder, null, true);
			DepthsManager.getInstance().getRandomAbility(player, dp, placeholder, DepthsTree.CURSE, false);
			dp.mRerolls++;

			DepthsManager.getInstance().setPlayerLevelInAbility(ABILITY_NAME, player, 0, false);
			dp.mCurrentAbnormality = false;

			if (Gui.getOpenGui(player) instanceof AbstractDepthsRewardGUI gui) {
				// If they just got Curse of Obscurity, this should make the item hidden. Might be visible for a moment but not much we can do.
				// Might also fix potential issues with getting items that were just upgraded/given an item in the slot for since we have now validated the offerings
				gui.update();
			}
		});
	}

	public static Description<Abnormality> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Abnormality>(color)
			.add("Gain a random ")
			.add(DepthsTree.PRISMATIC.getNameComponent())
			.add(" ability at ")
			.add(DepthsUtils.getRarityComponent(rarity))
			.add(" level, a random ability at ")
			.add(DepthsRarity.TWISTED.getDisplay())
			.add(" level, a random ")
			.add(DepthsTree.CURSE.getNameComponent())
			.add(", and 1 reroll. Remove this ability.");
	}
}
