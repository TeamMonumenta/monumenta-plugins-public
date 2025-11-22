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
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.jetbrains.annotations.Nullable;

public class CurseOfDependency extends DepthsAbility {
	public static @Nullable EntityRegainHealthEvent OTHER_PLAYER_EVENT = null;

	public static final String ABILITY_NAME = "Curse of Dependency";
	public static final double SELF_HEALING_MULTIPLIER = 0.65;

	public static final DepthsAbilityInfo<CurseOfDependency> INFO =
		new DepthsAbilityInfo<>(CurseOfDependency.class, ABILITY_NAME, CurseOfDependency::new, DepthsTree.CURSE, DepthsTrigger.PASSIVE)
			.displayItem(Material.LEAD)
			.descriptions(CurseOfDependency::getDescription);

	public CurseOfDependency(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public void playerRegainHealthEvent(EntityRegainHealthEvent event) {
		if (event != OTHER_PLAYER_EVENT) {
			event.setAmount(event.getAmount() * SELF_HEALING_MULTIPLIER);
		}
		OTHER_PLAYER_EVENT = null;
	}

	private static Description<CurseOfDependency> getDescription() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Self-healing is ")
			.addPercent(SELF_HEALING_MULTIPLIER)
			.add(" as effective. Healing from other players is unaffected.");
	}
}
