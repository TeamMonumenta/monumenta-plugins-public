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

public class CurseOfChaos extends DepthsAbility {
	public static final String ABILITY_NAME = "Curse of Chaos";
	public static final int ROOMS = 3;

	public static final DepthsAbilityInfo<CurseOfChaos> INFO =
		new DepthsAbilityInfo<>(CurseOfChaos.class, ABILITY_NAME, CurseOfChaos::new, DepthsTree.CURSE, DepthsTrigger.PASSIVE)
			.displayItem(Material.MUSIC_DISC_11)
			.descriptions(CurseOfChaos::getDescription);

	public CurseOfChaos(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static Description<CurseOfChaos> getDescription() {
		return new DescriptionBuilder<CurseOfChaos>()
			.add("The chaos utility room's effect is applied to you every ")
			.add(ROOMS)
			.add(" rooms.");
	}
}
