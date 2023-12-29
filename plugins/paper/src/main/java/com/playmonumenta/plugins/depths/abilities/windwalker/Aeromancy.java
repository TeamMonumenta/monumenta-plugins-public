package com.playmonumenta.plugins.depths.abilities.windwalker;

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
import com.playmonumenta.plugins.utils.LocationUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Aeromancy extends DepthsAbility {

	public static final String ABILITY_NAME = "Aeromancy";
	public static final double[] PLAYER_DAMAGE = {0.12, 0.15, 0.18, 0.21, 0.24, 0.3};
	public static final double[] MOB_DAMAGE = {0.056, 0.07, 0.084, 0.098, 0.112, 0.156};

	public static final DepthsAbilityInfo<Aeromancy> INFO =
		new DepthsAbilityInfo<>(Aeromancy.class, ABILITY_NAME, Aeromancy::new, DepthsTree.WINDWALKER, DepthsTrigger.PASSIVE)
			.displayItem(Material.FEATHER)
			.descriptions(Aeromancy::getDescription)
			.singleCharm(false);

	private final double mPlayerDamage;
	private final double mMobDamage;

	public Aeromancy(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mPlayerDamage = PLAYER_DAMAGE[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.AEROMANCY_PLAYER_DAMAGE_AMP.mEffectName);
		mMobDamage = MOB_DAMAGE[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.AEROMANCY_MOB_DAMAGE_AMP.mEffectName);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		DamageEvent.DamageType type = event.getType();
		if (type == DamageEvent.DamageType.TRUE || type == DamageEvent.DamageType.OTHER) {
			return false;
		}

		event.setDamage(event.getDamage() * damageMultiplier(enemy));
		return false; // only changes event damage
	}

	private double damageMultiplier(Entity damagee) {
		double multiplier = 1;
		if (LocationUtils.isAirborne(mPlayer)) {
			multiplier *= 1 + mPlayerDamage;
		}
		if (LocationUtils.isAirborne(damagee)) {
			multiplier *= 1 + mMobDamage;
		}
		return multiplier;
	}

	private static Description<Aeromancy> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Aeromancy>(color)
			.add("You deal ")
			.addPercent(a -> a.mPlayerDamage, PLAYER_DAMAGE[rarity - 1], false, true)
			.add(" more damage while airborne and ")
			.addPercent(a -> a.mMobDamage, MOB_DAMAGE[rarity - 1], false, true)
			.add(" more damage to airborne enemies.");
	}


}

