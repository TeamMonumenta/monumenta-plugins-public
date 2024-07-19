package com.playmonumenta.plugins.depths.abilities.curses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class CurseOfLust extends DepthsAbility {
	public static final String ABILITY_NAME = "Curse of Lust";
	public static final double RANGE = 12;
	public static final double DAMAGE = 0.2;

	public static final DepthsAbilityInfo<CurseOfLust> INFO =
		new DepthsAbilityInfo<>(CurseOfLust.class, ABILITY_NAME, CurseOfLust::new, DepthsTree.CURSE, DepthsTrigger.PASSIVE)
			.displayItem(Material.RED_CANDLE) //idk probably there's a better choice
			.descriptions(CurseOfLust::getDescription);

	public CurseOfLust(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (DamageEvent.DamageType.getScalableDamageType().contains(event.getType())) {
			List<Player> players = PlayerUtils.otherPlayersInRange(mPlayer, RANGE, true);
			if (!players.isEmpty()) {
				event.updateDamageWithMultiplier(1 - players.size() * DAMAGE);
			}
		}
		return false;
	}

	private static Description<CurseOfLust> getDescription() {
		return new DescriptionBuilder<CurseOfLust>()
			.add("For each other player within ")
			.add(a -> RANGE, RANGE)
			.add(" blocks of you, you deal ")
			.addPercent(DAMAGE)
			.add(" less damage.");
	}
}
