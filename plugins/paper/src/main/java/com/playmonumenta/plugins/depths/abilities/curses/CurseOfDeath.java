package com.playmonumenta.plugins.depths.abilities.curses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsListener;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.utils.StringUtils;
import net.kyori.adventure.text.Component;
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
		return new DescriptionBuilder<>(() -> INFO)
			.add("Your grave revive timer is reduced by 2 deaths' worth.")
			.add((a, p) -> {
				if (a == null || p == null) {
					return Component.empty();
				}
				DepthsPlayer player = DepthsManager.getInstance().getDepthsPlayer(p);
				DepthsParty party = DepthsManager.getInstance().getDepthsParty(p);
				if (party != null && player != null) {
					int timer = DepthsListener.getGraveDuration(party, player, p);
					return Component.text("\nCurrent grave timer: " + StringUtils.ticksToSeconds(timer) + " seconds.");
				}
				return Component.empty();
			});
	}
}
