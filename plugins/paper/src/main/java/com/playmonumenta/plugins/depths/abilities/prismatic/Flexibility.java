package com.playmonumenta.plugins.depths.abilities.prismatic;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.Objects;
import java.util.stream.Collectors;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Flexibility extends DepthsAbility {
	public static final String ABILITY_NAME = "Flexibility";
	public static final double[] DAMAGE = {0.02, 0.025, 0.03, 0.035, 0.04, 0.05};

	public static final DepthsAbilityInfo<Flexibility> INFO =
		new DepthsAbilityInfo<>(Flexibility.class, ABILITY_NAME, Flexibility::new, DepthsTree.PRISMATIC, DepthsTrigger.PASSIVE)
			.displayItem(Material.WHITE_WOOL)
			.descriptions(Flexibility::getDescription);

	private double mDamage = 1;

	public Flexibility(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		// Since all abilities are refreshed when we get a new ability, we only have to calculate the value once
		Bukkit.getScheduler().runTask(plugin, () -> {
			mDamage = 1 + DAMAGE[mRarity - 1] * DepthsManager.getInstance().getPlayerAbilities(player).stream()
				.filter(info -> info.getDepthsTrigger() != DepthsTrigger.PASSIVE)
				.map(DepthsAbilityInfo::getDepthsTree).filter(Objects::nonNull)
				.collect(Collectors.toSet()).size();
		});
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (DamageEvent.DamageType.getScalableDamageType().contains(event.getType())) {
			event.setDamage(event.getDamage() * mDamage);
		}
		return false;
	}

	private static Description<Flexibility> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Flexibility>(color)
			.add("Gain ")
			.addPercent(a -> DAMAGE[rarity - 1], DAMAGE[rarity - 1], false, true)
			.add(" damage for each unique tree featured in your active ability slots.");
	}
}
