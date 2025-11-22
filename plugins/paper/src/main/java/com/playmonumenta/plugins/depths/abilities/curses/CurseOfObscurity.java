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

public class CurseOfObscurity extends DepthsAbility {
	public static final String ABILITY_NAME = "Curse of Obscurity";

	public static final DepthsAbilityInfo<CurseOfObscurity> INFO =
		new DepthsAbilityInfo<>(CurseOfObscurity.class, ABILITY_NAME, CurseOfObscurity::new, DepthsTree.CURSE, DepthsTrigger.PASSIVE)
			.displayItem(Material.ROTTEN_FLESH)
			.floors(floor -> floor == 1)
			.descriptions(CurseOfObscurity::getDescription);

	public CurseOfObscurity(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	private static Description<CurseOfObscurity> getDescription() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("One of your ability or upgrade choices is hidden.");
	}
}
