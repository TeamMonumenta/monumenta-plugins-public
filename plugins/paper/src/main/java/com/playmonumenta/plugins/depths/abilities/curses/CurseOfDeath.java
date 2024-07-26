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

public class CurseOfDeath extends DepthsAbility {
	public static final String ABILITY_NAME = "Curse of Death";

	public static final DepthsAbilityInfo<CurseOfDeath> INFO =
		new DepthsAbilityInfo<>(CurseOfDeath.class, ABILITY_NAME, CurseOfDeath::new, DepthsTree.CURSE, DepthsTrigger.PASSIVE)
			.displayItem(Material.NETHERITE_HOE)
			.descriptions(CurseOfDeath::getDescription);

	public CurseOfDeath(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static Description<CurseOfDeath> getDescription() {
		return new DescriptionBuilder<CurseOfDeath>()
			.add("Your revive timer is reduced to the minimum.");
	}
}
