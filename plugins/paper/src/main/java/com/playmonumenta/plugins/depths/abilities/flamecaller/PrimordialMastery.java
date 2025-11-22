package com.playmonumenta.plugins.depths.abilities.flamecaller;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class PrimordialMastery extends DepthsAbility {

	public static final String ABILITY_NAME = "Primordial Mastery";
	public static final double[] SPELL_MOD = {0.08, 0.12, 0.16, 0.2, 0.24, 0.32};

	public static final DepthsAbilityInfo<PrimordialMastery> INFO =
		new DepthsAbilityInfo<>(PrimordialMastery.class, ABILITY_NAME, PrimordialMastery::new, DepthsTree.FLAMECALLER, DepthsTrigger.PASSIVE)
			.displayItem(Material.FIRE_CORAL_FAN)
			.descriptions(PrimordialMastery::getDescription)
			.singleCharm(false);

	private final double mDamageModifier;

	public PrimordialMastery(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamageModifier = SPELL_MOD[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.PRIMORDIAL_MASTERY_DAMAGE_MODIFIER.mEffectName);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		DamageEvent.DamageType type = event.getType();
		if (type == DamageEvent.DamageType.TRUE || type == DamageEvent.DamageType.OTHER) {
			return false;
		}
		if (event.getAbility() != null && !event.getAbility().isFake()) {
			event.updateDamageWithMultiplier(1 + mDamageModifier);
		}
		return false; // only changes event damage
	}

	private static Description<PrimordialMastery> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.add("Your abilities deal ")
			.addPercent(a -> a.mDamageModifier, SPELL_MOD[rarity - 1], false, true)
			.add(" more damage.");
	}
}

