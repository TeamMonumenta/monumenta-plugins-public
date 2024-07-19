package com.playmonumenta.plugins.depths.abilities.curses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.earthbound.Bulwark;
import com.playmonumenta.plugins.depths.abilities.windwalker.DepthsDodging;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.utils.AbilityUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class CurseOfImpatience extends DepthsAbility {

	public static final String ABILITY_NAME = "Curse of Impatience";

	public static final DepthsAbilityInfo<CurseOfImpatience> INFO =
		new DepthsAbilityInfo<>(CurseOfImpatience.class, ABILITY_NAME, CurseOfImpatience::new, DepthsTree.CURSE, DepthsTrigger.PASSIVE)
			.displayItem(Material.CLOCK)
			.descriptions(CurseOfImpatience::getDescription);

	public CurseOfImpatience(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent e) {
		if (e.getAbility().getInfo() instanceof DepthsAbilityInfo<?> info && (info == Bulwark.INFO || info == DepthsDodging.INFO || info.getDepthsTrigger() == DepthsTrigger.LIFELINE)) {
			return true;
		}
		AbilityUtils.silencePlayer(mPlayer, 20);
		return true;
	}

	private static Description<CurseOfImpatience> getDescription() {
		return new DescriptionBuilder<CurseOfImpatience>()
			.add("You are silenced for 1s after casting an ability.");
	}
}
