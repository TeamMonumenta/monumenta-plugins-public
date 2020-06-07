package com.playmonumenta.plugins.abilities.warrior.berserker;

import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;

/* Growing Rage: Passively gain +10% / +30% damage on
 * non-ability based melee damage. Every 2 hearts you fall
 * below max health, gain 5% on your damage bonus.
 * Damage capped at 40%/60%.
 */

public class GrowingRage extends Ability {

	private static final double GROWING_RAGE_1_DAMAGE_PERCENT = 0.1;
	private static final double GROWING_RAGE_2_DAMAGE_PERCENT = 0.3;
	private static final double GROWING_RAGE_MAX_INCREASES = 6;
	private static final double GROWING_RAGE_HEALTH_THRESHOLD = 4;

	private double mDamagePercent;
	private int mHealthThreshold = 0;

	public GrowingRage(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Growing Rage");
		mInfo.mScoreboardId = "GrowingRage";
		mInfo.mShorthandName = "GR";
		mInfo.mDescriptions.add("You do 10% more damage with non-skill based melee damage. Every 2 hearts you fall below your maximum health, gain another 5% more damage, to a maximum of 40% total.");
		mInfo.mDescriptions.add("The damage bonus is increased to 30%, to a maximum of 60%.");
		mDamagePercent = 1 + (getAbilityScore() == 1 ? GROWING_RAGE_1_DAMAGE_PERCENT : GROWING_RAGE_2_DAMAGE_PERCENT);
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		AttributeInstance maxHealth = mPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH);
		if (maxHealth != null) {
			int healthThreshold = (int) Math.min(GROWING_RAGE_MAX_INCREASES, (int)((maxHealth.getValue() - mPlayer.getHealth()) / GROWING_RAGE_HEALTH_THRESHOLD));
			if (healthThreshold != mHealthThreshold) {
				mDamagePercent = mDamagePercent + (healthThreshold - mHealthThreshold) * 0.05;
				mHealthThreshold = healthThreshold;
			}
		}
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			event.setDamage(event.getDamage() * mDamagePercent);
		}

		return true;
	}

}
