package com.playmonumenta.plugins.depths.abilities.curses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.bosses.Broodmother;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Spider;
import org.jetbrains.annotations.Nullable;

public class CurseOfArachnophobia extends DepthsAbility {
	public static final String ABILITY_NAME = "Curse of Arachnophobia";
	public static final double DAMAGE_DEALT = 0.50;
	public static final double DAMAGE_TAKEN = 0.50;

	public static final DepthsAbilityInfo<CurseOfArachnophobia> INFO =
		new DepthsAbilityInfo<>(CurseOfArachnophobia.class, ABILITY_NAME, CurseOfArachnophobia::new, DepthsTree.CURSE, DepthsTrigger.PASSIVE)
			.displayItem(Material.SPIDER_EYE)
			.descriptions(CurseOfArachnophobia::getDescription);

	public CurseOfArachnophobia(Plugin plugin, Player player) {
		super(plugin, player, INFO);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (DamageEvent.DamageType.getScalableDamageType().contains(event.getType()) &&
			(enemy instanceof Spider || ScoreboardUtils.checkTag(enemy, Broodmother.identityTag) || enemy.getName().equals(Broodmother.LIMB_PLAIN_NAME))) {
			event.updateDamageWithMultiplier(1 - DAMAGE_DEALT);
		}
		return false;
	}

	@Override
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (DamageEvent.DamageType.getScalableDamageType().contains(event.getType()) && source != null &&
			(source instanceof Spider || ScoreboardUtils.checkTag(source, Broodmother.identityTag))) {
			event.updateDamageWithMultiplier(1 + DAMAGE_TAKEN);
		}
	}

	private static Description<CurseOfArachnophobia> getDescription() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Deal ")
			.addPercent(DAMAGE_DEALT)
			.add(" less to and take ")
			.addPercent(DAMAGE_TAKEN)
			.add(" more damage from spiders of all sizes.");
	}
}
