package com.playmonumenta.plugins.depths.abilities.curses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class CurseOfEnvy extends DepthsAbility {
	public static final String ABILITY_NAME = "Curse of Envy";

	public static final DepthsAbilityInfo<CurseOfEnvy> INFO =
		new DepthsAbilityInfo<>(CurseOfEnvy.class, ABILITY_NAME, CurseOfEnvy::new, DepthsTree.CURSE, DepthsTrigger.PASSIVE)
			.displayItem(Material.APPLE)
			.gain(CurseOfEnvy::gain)
			.descriptions(CurseOfEnvy::getDescription);

	public CurseOfEnvy(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	public static void gain(Player player) {
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(player);
		if (dp == null) {
			return;
		}
		dp.mEligibleTrees = Arrays.stream(DepthsTree.OWNABLE_TREES).filter(tree -> !dp.mEligibleTrees.contains(tree)).collect(Collectors.toList());
	}

	private static Description<CurseOfEnvy> getDescription() {
		return new DescriptionBuilder<CurseOfEnvy>()
			.add("Your trees are inverted.");
	}
}
