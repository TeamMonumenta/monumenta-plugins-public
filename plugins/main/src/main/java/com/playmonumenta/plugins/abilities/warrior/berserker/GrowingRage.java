package com.playmonumenta.plugins.abilities.warrior.berserker;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;

/* Growing Rage: Passively gain +20% / +40% damage on
 * non-ability based melee damage. Every 2 hearts you fall
 * below max health, gain 1 armor and lose 5% off your
 * damage bonus. Armor capped at 5 / 10.
 */

public class GrowingRage extends Ability {

	private static final double GROWING_RAGE_1_DAMAGE_PERCENT = 0.1;
	private static final double GROWING_RAGE_2_DAMAGE_PERCENT = 0.3;
	private static final int GROWING_RAGE_1_MAX_ARMOR = 5;
	private static final int GROWING_RAGE_2_MAX_ARMOR = 10;
	private static final double GROWING_RAGE_HEALTH_THRESHOLD = 4;

	private int mHealthThreshold = 0;

	public GrowingRage(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "GrowingRage";
	}

	@Override
	public void PeriodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		double maxHealth = mPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
		double maxArmor = getAbilityScore() == 1 ? GROWING_RAGE_1_MAX_ARMOR : GROWING_RAGE_2_MAX_ARMOR;
		int healthThreshold = (int) Math.min(maxArmor, (int)((maxHealth - mPlayer.getHealth()) / GROWING_RAGE_HEALTH_THRESHOLD));
		if (healthThreshold != mHealthThreshold) {
			AttributeInstance attarmor = mPlayer.getAttribute(Attribute.GENERIC_ARMOR);
			attarmor.setBaseValue(attarmor.getBaseValue() - mHealthThreshold + healthThreshold);
			mHealthThreshold = healthThreshold;
		}
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (event.getCause() == DamageCause.ENTITY_ATTACK) {
			double damageMultiplier = getAbilityScore() == 1 ? GROWING_RAGE_1_DAMAGE_PERCENT : GROWING_RAGE_2_DAMAGE_PERCENT;
			event.setDamage(event.getDamage() * damageMultiplier);
		}

		return true;
	}

}
