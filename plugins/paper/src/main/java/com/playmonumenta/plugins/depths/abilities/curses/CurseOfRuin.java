package com.playmonumenta.plugins.depths.abilities.curses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class CurseOfRuin extends DepthsAbility {
	public static final String ABILITY_NAME = "Curse of Ruin";

	public static final DepthsAbilityInfo<CurseOfRuin> INFO =
		new DepthsAbilityInfo<>(CurseOfRuin.class, ABILITY_NAME, CurseOfRuin::new, DepthsTree.CURSE, DepthsTrigger.PASSIVE)
			.displayItem(Material.CRACKED_NETHER_BRICKS)
			.floors(floor -> floor == 1)
			.descriptions(CurseOfRuin::getDescription);

	public CurseOfRuin(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static Description<CurseOfRuin> getDescription() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("When you select an ability or upgrade, downgrade a random ability by 1 level.");
	}
}
