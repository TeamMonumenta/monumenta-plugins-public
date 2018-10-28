package com.playmonumenta.plugins.abilities.warrior.berserker;

import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.Plugin;

import java.util.Random;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.World;

/* Psychosis: While below 50% health, gain +2/+4 Attack
 * Damage and 15/25% knockback resistance. If also below 3
 * hearts, gain 3 armor and at level 2 also 0.2 attack speed
 */

public class Psychosis extends Ability {

	private static final double PSYCHOSIS_TRIGGER_HEALTH_PERCENT = 0.5;
	private static final double PSYCHOSIS_TRIGGER_HEALTH = 6;
	private static final double PSYCHOSIS_1_KNOCKBACK_RESISTANCE = 0.15;
	private static final double PSYCHOSIS_2_KNOCKBACK_RESISTANCE = 0.25;
	private static final double PSYCHOSIS_1_DAMAGE = 2;
	private static final double PSYCHOSIS_2_DAMAGE = 4;
	private static final double PSYCHOSIS_ARMOR = 3;
	private static final double PSYCHOSIS_ATTACK_SPEED = 0.2;

	private static boolean psychosisPercentActive = false;
	private static boolean psychosisFlatActive = false;

	public Psychosis(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 1;
		mInfo.specId = 11;
		mInfo.scoreboardId = "Psychosis";
	}

	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (mPlayer.getHealth() <= PSYCHOSIS_TRIGGER_HEALTH_PERCENT * mPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) {
			int psychosis = getAbilityScore();
			double psychosisDamage = psychosis == 1 ? PSYCHOSIS_1_DAMAGE : PSYCHOSIS_2_DAMAGE;
			event.setDamage(event.getDamage() + psychosisDamage);
		}
		return true;
	}

	@Override
	public boolean PlayerDamagedEvent(EntityDamageByEntityEvent event) {
		if (!mPlayer.isDead()) {
			double correctHealth = mPlayer.getHealth() - event.getDamage();
			double maxHealth = mPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			int psychosis = getAbilityScore();
			double knockbackRes = psychosis == 1 ? PSYCHOSIS_1_KNOCKBACK_RESISTANCE : PSYCHOSIS_2_KNOCKBACK_RESISTANCE;
			if (correctHealth > 0 && correctHealth <= PSYCHOSIS_TRIGGER_HEALTH_PERCENT * maxHealth && !psychosisPercentActive) {
				AttributeInstance attkbr = mPlayer.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
				attkbr.setBaseValue(attkbr.getBaseValue() + knockbackRes);
				psychosisPercentActive = true;
			} else if (correctHealth > PSYCHOSIS_TRIGGER_HEALTH_PERCENT * maxHealth && psychosisPercentActive) {
				AttributeInstance attkbr = mPlayer.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
				if (attkbr.getBaseValue() - knockbackRes >= 0) {
					attkbr.setBaseValue(attkbr.getBaseValue() - knockbackRes);
				} else {
					attkbr.setBaseValue(0);
				}
				psychosisPercentActive = false;
			}

			if (correctHealth > 0 && correctHealth <= PSYCHOSIS_TRIGGER_HEALTH && !psychosisFlatActive) {
				AttributeInstance attarmor = mPlayer.getAttribute(Attribute.GENERIC_ARMOR);
				attarmor.setBaseValue(attarmor.getBaseValue() + PSYCHOSIS_ARMOR);
				if (psychosis > 1) {
					AttributeInstance attas = mPlayer.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
					attas.setBaseValue(attas.getBaseValue() + PSYCHOSIS_ATTACK_SPEED);
				}
				psychosisFlatActive = true;
			} else if (correctHealth > PSYCHOSIS_TRIGGER_HEALTH && psychosisFlatActive) {
				AttributeInstance attarmor = mPlayer.getAttribute(Attribute.GENERIC_ARMOR);
				if (attarmor.getBaseValue() - PSYCHOSIS_ARMOR > 0) {
					attarmor.setBaseValue(attarmor.getBaseValue() - PSYCHOSIS_ARMOR);
				} else {
					attarmor.setBaseValue(0);
				}
				if (psychosis > 1) {
					AttributeInstance attas = mPlayer.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
					if (attas.getBaseValue() - PSYCHOSIS_ATTACK_SPEED > 0) {
						attas.setBaseValue(attas.getBaseValue() - PSYCHOSIS_ATTACK_SPEED);
					} else {
						attas.setBaseValue(0);
					}
				}
				psychosisFlatActive = false;
			}
		}
		return true;
	}

}
