package com.playmonumenta.plugins.abilities.scout;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;

public class BowMastery extends Ability {

	private static final double BOW_MASTERY_1_DAMAGE_MULTIPLIER = 1.2;
	private static final double BOW_MASTERY_2_DAMAGE_MULTIPLIER = 1.35;

	private final double mDamageMultiplier;
	public BowMastery(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Bow Mastery");
		mInfo.mScoreboardId = "BowMastery";
		mInfo.mShorthandName = "BM";
		mInfo.mDescriptions.add("Your arrows deal 20% more damage.");
		mInfo.mDescriptions.add("Your arrows deal 35% more damage.");
		mInfo.mIgnoreTriggerCap = true;

		mDamageMultiplier = getAbilityScore() == 1 ? BOW_MASTERY_1_DAMAGE_MULTIPLIER : BOW_MASTERY_2_DAMAGE_MULTIPLIER;
	}

	@Override
	public boolean playerShotArrowEvent(AbstractArrow arrow) {
		mPlugin.mProjectileEffectTimers.addEntity(arrow, Particle.CLOUD);
		return true;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity le, EntityDamageByEntityEvent event) {
		if (proj instanceof Arrow || proj instanceof SpectralArrow) {
			event.setDamage(event.getDamage() * mDamageMultiplier);
		}

		return true;
	}

	public static double getDamageMultiplier(Player player) {
		BowMastery bm = AbilityManager.getManager().getPlayerAbility(player, BowMastery.class);
		return bm == null ? 1 : bm.mDamageMultiplier;
	}

}
