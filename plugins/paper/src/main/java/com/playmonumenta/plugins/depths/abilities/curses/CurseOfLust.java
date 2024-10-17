package com.playmonumenta.plugins.depths.abilities.curses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.Objects;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class CurseOfLust extends DepthsAbility {
	public static final String ABILITY_NAME = "Curse of Lust";
	public static final int MIN_BLOCKS = 7;
	public static final int MAX_BLOCKS = 10;
	public static final double DAMAGE_REDUCTION_PER_BLOCK = 0.08;

	public static final DepthsAbilityInfo<CurseOfLust> INFO =
		new DepthsAbilityInfo<>(CurseOfLust.class, ABILITY_NAME, CurseOfLust::new, DepthsTree.CURSE, DepthsTrigger.PASSIVE)
			.displayItem(Material.RED_CANDLE)
			.descriptions(CurseOfLust::getDescription);

	public CurseOfLust(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (DamageEvent.DamageType.getScalableDamageType().contains(event.getType())) {
			DepthsParty party = DepthsManager.getInstance().getDepthsParty(mPlayer);
			if (party == null) {
				return false;
			}
			double closestSquared = party.getPlayers().stream()
				.filter(Objects::nonNull)
				.filter(p -> p != mPlayer)
				.filter(DepthsManager.getInstance()::isAlive)
				.mapToDouble(p -> p.getLocation().distanceSquared(mPlayer.getLocation()))
				.min().orElse(0);
			if (closestSquared >= MIN_BLOCKS * MIN_BLOCKS) {
				double mult = 1 - DAMAGE_REDUCTION_PER_BLOCK * (Math.min(Math.sqrt(closestSquared), MAX_BLOCKS) - MIN_BLOCKS);
				event.updateDamageWithMultiplier(mult);
			}
		}
		return false;
	}

	private static Description<CurseOfLust> getDescription() {
		return new DescriptionBuilder<CurseOfLust>()
			.add("Deal ")
			.addPercent(DAMAGE_REDUCTION_PER_BLOCK)
			.add(" less damage for each block above " + MIN_BLOCKS + " blocks away your closest teammate is, up to ")
			.addPercent(MAX_BLOCKS * DAMAGE_REDUCTION_PER_BLOCK)
			.add(" reduced damage.");
	}
}
