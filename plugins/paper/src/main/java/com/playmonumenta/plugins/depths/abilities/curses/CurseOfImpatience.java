package com.playmonumenta.plugins.depths.abilities.curses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.abilities.earthbound.Bulwark;
import com.playmonumenta.plugins.depths.abilities.windwalker.DepthsDodging;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import java.util.Objects;
import java.util.UUID;
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
		UUID uuid = mPlayer.getUniqueId();
		mPlugin.mAbilityManager.getPlayerAbilities(mPlayer).getAbilities().stream()
			.map(Ability::getInfo)
			.filter(AbilityInfo::hasCooldown)
			.map(AbilityInfo::getLinkedSpell)
			.filter(Objects::nonNull) // Shouldn't be null here ever but whatever
			.filter(ca -> ca != e.getSpell())
			.forEach(ca -> {
				// Modify cooldown directly - we don't want any effects, enchants, etc. messing with this
				int cooldown = mPlugin.mTimers.getCooldown(uuid, ca);
				mPlugin.mTimers.addCooldown(mPlayer, ca, cooldown + 20);
			});
		return true;
	}

	private static Description<CurseOfImpatience> getDescription() {
		return new DescriptionBuilder<CurseOfImpatience>()
			.add("After casting an ability, all other abilities' cooldowns are increased by 1s.");
	}
}
