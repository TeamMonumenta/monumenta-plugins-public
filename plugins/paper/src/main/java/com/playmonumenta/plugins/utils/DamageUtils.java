package com.playmonumenta.plugins.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;
import javax.annotation.Nullable;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;

public class DamageUtils {

	public static double getDamageMultiplier(double armor, double agility, double epf, boolean environmental) {
		double ar = Math.max(0, armor);
		double ag = Math.max(0, agility);
		double defense = ar + ag - 0.5 * ar * ag / (ar + ag);
		return environmental ? Math.pow(0.96, (defense / 2) + epf) : Math.pow(0.96, defense + epf);
	}

	/**
	 * Deals damage to a LivingEntity.
	 *
	 * @param damager LivingEntity dealing damage, pass null if not applicable
	 * @param damagee LivingEntity receiving damage
	 * @param type    DamageType the damage counts as
	 * @param amount  amount of damage to be dealt
	 */
	public static void damage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type, double amount) {
		damage(damager, damagee, type, amount, null);
	}

	/**
	 * Deals damage to a LivingEntity.
	 *
	 * @param damager LivingEntity dealing damage, pass null if not applicable
	 * @param damagee LivingEntity receiving damage
	 * @param type    DamageType the damage counts as
	 * @param amount  amount of damage to be dealt
	 * @param ability ClassAbility causing the damage, pass null if not applicable
	 */
	public static void damage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type, double amount, @Nullable ClassAbility ability) {
		damage(damager, damagee, type, amount, ability, false);
	}

	/**
	 * Deals damage to a LivingEntity.
	 *
	 * @param damager       LivingEntity dealing damage, pass null if not applicable
	 * @param damagee       LivingEntity receiving damage
	 * @param type          DamageType the damage counts as
	 * @param amount        amount of damage to be dealt
	 * @param ability       ClassAbility causing the damage, pass null if not applicable
	 * @param bypassIFrames whether the damage should bypass IFrames
	 */
	public static void damage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type, double amount, @Nullable ClassAbility ability, boolean bypassIFrames) {
		damage(damager, damagee, type, amount, ability, bypassIFrames, false);
	}

	/**
	 * Deals damage to a LivingEntity.
	 *
	 * @param damager       LivingEntity dealing damage, pass null if not applicable
	 * @param damagee       LivingEntity receiving damage
	 * @param type          DamageType the damage counts as
	 * @param amount        amount of damage to be dealt
	 * @param ability       ClassAbility causing the damage, pass null if not applicable
	 * @param bypassIFrames whether the damage should bypass IFrames
	 * @param causeKnockback whether the damage should cause knockback
	 */
	public static void damage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type, double amount, @Nullable ClassAbility ability, boolean bypassIFrames, boolean causeKnockback) {
		damage(damager, damagee, type, amount, ability, bypassIFrames, causeKnockback, null);
	}

	/**
	 * Deals damage to a LivingEntity.
	 *
	 * @param damager        LivingEntity dealing damage, pass null if not applicable
	 * @param damagee        LivingEntity receiving damage
	 * @param type           DamageType the damage counts as
	 * @param amount         amount of damage to be dealt
	 * @param ability        ClassAbility causing the damage, pass null if not applicable
	 * @param bypassIFrames  whether the damage should bypass IFrames
	 * @param causeKnockback whether the damage should cause knockback
	 * @param bossCause      string to pass for boss death messages
	 */
	public static void damage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type, double amount, @Nullable ClassAbility ability, boolean bypassIFrames, boolean causeKnockback, @Nullable String bossCause) {
		damage(new DamageEvent(null, damagee, damager, damager, type, ability, amount), bypassIFrames, causeKnockback, bossCause);
	}

	/**
	 * Deals damage to a LivingEntity.
	 * <br>
	 * Use this method when additional metadata needs to be added to the DamageEvent before calling it.
	 *
	 * @param event          DamageEvent to be called
	 * @param bypassIFrames  whether the damage should bypass IFrames
	 * @param causeKnockback whether the damage should cause knockback
	 */
	public static void damage(DamageEvent event, boolean bypassIFrames, boolean causeKnockback, @Nullable String bossCause) {
		LivingEntity damagee = event.getDamagee();
		@Nullable LivingEntity damager = event.getSource();

		if (!damagee.isValid() || damagee.isInvulnerable()) {
			return;
		}

		int originalAttackCooldown = 0;
		if (damager != null) {
			NmsUtils.getVersionAdapter().getAttackCooldown(damager);
		}

		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return;
		}

		int originalIFrames = damagee.getNoDamageTicks();
		double originalLastDamage = damagee.getLastDamage();
		Vector originalVelocity = damagee.getVelocity();
		if (bypassIFrames) {
			damagee.setNoDamageTicks(0);
		}

		double actualDamage = event.getDamage();
		NmsUtils.getVersionAdapter().customDamageEntity(damager, damagee, actualDamage, bossCause);

		if (bypassIFrames) {
			damagee.setNoDamageTicks(originalIFrames);
			damagee.setLastDamage(originalLastDamage);
		}

		if (!causeKnockback) {
			damagee.setVelocity(originalVelocity);
		}

		if (damager != null) {
			NmsUtils.getVersionAdapter().setAttackCooldown(damager, originalAttackCooldown);
		}
	}

	public static void dualTypeDamage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type1, DamageType type2, double amount, double percentType1) {
		dualTypeDamage(damager, damagee, type1, type2, amount, percentType1, null, false, true, null);
	}

	public static void dualTypeDamage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type1, DamageType type2, double amount, double percentType1, @Nullable ClassAbility ability, boolean bypassIFrames, boolean causeKnockback, @Nullable String bossCause) {
		int originalIFrames = damagee.getNoDamageTicks();
		damage(damager, damagee, type1, amount * percentType1, ability, bypassIFrames, causeKnockback, bossCause);
		damagee.setNoDamageTicks(originalIFrames);
		damage(damager, damagee, type2, amount * (1 - percentType1), ability, bypassIFrames, causeKnockback, bossCause);
	}
}
