package com.playmonumenta.plugins.depths.abilities.shadow;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class DeadlyStrike extends DepthsAbility {

	public static final String ABILITY_NAME = "Deadly Strike";
	public static final double[] DAMAGE = {0.10, 0.15, 0.20, 0.25, 0.3, 0.4};

	public static final DepthsAbilityInfo<DeadlyStrike> INFO =
		new DepthsAbilityInfo<>(DeadlyStrike.class, ABILITY_NAME, DeadlyStrike::new, DepthsTree.SHADOWDANCER, DepthsTrigger.PASSIVE)
			.displayItem(Material.BLACK_CONCRETE_POWDER)
			.descriptions(DeadlyStrike::getDescription)
			.singleCharm(false);

	private final double mDamage;

	public DeadlyStrike(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamage = DAMAGE[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.DEADLY_STRIKE_DAMAGE_AMPLIFIER.mEffectName);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE || event.getType() == DamageType.MELEE_SKILL || event.getType() == DamageType.MELEE_ENCH) {
			event.updateDamageWithMultiplier(1 + mDamage);
		}
		return false; // only changes event damage
	}

	private static Description<DeadlyStrike> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.add("You deal ")
			.addPercent(a -> a.mDamage, DAMAGE[rarity - 1], false, true)
			.add(" more melee damage.");
	}


}

