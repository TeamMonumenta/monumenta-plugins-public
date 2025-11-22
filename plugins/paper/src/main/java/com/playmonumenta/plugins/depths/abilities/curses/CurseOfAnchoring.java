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

public class CurseOfAnchoring extends DepthsAbility {
	public static final String ABILITY_NAME = "Curse of Anchoring";

	public static final DepthsAbilityInfo<CurseOfAnchoring> INFO =
		new DepthsAbilityInfo<>(CurseOfAnchoring.class, ABILITY_NAME, CurseOfAnchoring::new, DepthsTree.CURSE, DepthsTrigger.SWAP)
			.displayItem(Material.BEDROCK)
			.descriptions(CurseOfAnchoring::getDescription);

	public CurseOfAnchoring(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static Description<CurseOfAnchoring> getDescription() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Your Swap trigger is permanently locked.");
	}
}
