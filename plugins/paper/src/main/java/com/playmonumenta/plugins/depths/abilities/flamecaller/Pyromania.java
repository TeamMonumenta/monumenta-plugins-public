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
import com.playmonumenta.plugins.utils.EntityUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Pyromania extends DepthsAbility {

	public static final String ABILITY_NAME = "Pyromania";
	public static final double[] DAMAGE = {0.02, 0.025, 0.03, 0.035, 0.04, 0.05};
	public static final int RADIUS = 6;
	public static final int TWISTED_RADIUS = 8;

	public static final DepthsAbilityInfo<Pyromania> INFO =
		new DepthsAbilityInfo<>(Pyromania.class, ABILITY_NAME, Pyromania::new, DepthsTree.FLAMECALLER, DepthsTrigger.PASSIVE)
			.displayItem(Material.CAMPFIRE)
			.descriptions(Pyromania::getDescription)
			.singleCharm(false);

	private final double mRadius;
	private final double mDamagePerMob;

	public Pyromania(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mRadius = CharmManager.getRadius(mPlayer, CharmEffects.PYROMANIA_RADIUS.mEffectName, mRarity == 6 ? TWISTED_RADIUS : RADIUS);
		mDamagePerMob = DAMAGE[mRarity - 1] + CharmManager.getLevelPercentDecimal(mPlayer, CharmEffects.PYROMANIA_DAMAGE_PER_MOB.mEffectName);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		int fireCount = 0;

		for (LivingEntity e : EntityUtils.getNearbyMobs(mPlayer.getLocation(), mRadius)) {
			if (e.getFireTicks() > 0) {
				fireCount++;
			}
		}
		if (fireCount > 0) {
			event.setDamage(event.getDamage() * (1 + (mDamagePerMob * fireCount)));
		}
		return false;
	}

	private static Description<Pyromania> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<Pyromania>(color)
			.add("For every mob on fire within ")
			.add(a -> a.mRadius, rarity == 6 ? TWISTED_RADIUS : RADIUS, false, null, rarity == 6)
			.add(" blocks of you, gain ")
			.addPercent(a -> a.mDamagePerMob, DAMAGE[rarity - 1], false, true)
			.add(" increased damage.");
	}
}

