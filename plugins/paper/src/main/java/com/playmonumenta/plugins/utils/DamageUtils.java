package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import javax.annotation.Nullable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class DamageUtils {

	/**
	 * To be used by {@link com.playmonumenta.plugins.listeners.DamageListener} to fill this info into the created {@link DamageEvent} on custom damage.
	 */
	public static @Nullable DamageEvent.Metadata nextEventMetadata = null;

	public static double getDamageMultiplier(double armor, double agility, double epf, boolean environmental) {
		double ar = Math.max(0, armor);
		double ag = Math.max(0, agility);
		double defense = ar + ag == 0 ? 0 : ar + ag - 0.5 * ar * ag / (ar + ag);
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
		damage(damager, damagee, new DamageEvent.Metadata(type, ability), amount, bypassIFrames, causeKnockback, bossCause);
	}

	/**
	 * Deals damage to a LivingEntity.
	 * <br>
	 * Use this method when additional metadata needs to be added to the DamageEvent before calling it.
	 *
	 * @param damager        LivingEntity dealing damage, pass null if not applicable
	 * @param damagee        LivingEntity receiving damage
	 * @param metadata       Metadata for the event
	 * @param amount         amount of damage to be dealt
	 * @param bypassIFrames  whether the damage should bypass IFrames
	 * @param causeKnockback whether the damage should cause knockback
	 */
	public static void damage(@Nullable LivingEntity damager, LivingEntity damagee, DamageEvent.Metadata metadata, double amount, boolean bypassIFrames, boolean causeKnockback, @Nullable String bossCause) {

		if (!damagee.isValid() || damagee.isInvulnerable()) {
			return;
		}

		int originalAttackCooldown = 0;
		if (damager != null) {
			originalAttackCooldown = NmsUtils.getVersionAdapter().getAttackCooldown(damager);
		}

		int originalIFrames = damagee.getNoDamageTicks();
		double originalLastDamage = damagee.getLastDamage();
		Vector originalVelocity = damagee.getVelocity();
		if (bypassIFrames) {
			damagee.setNoDamageTicks(0);
		}

		DamageUtils.nextEventMetadata = metadata;
		try {
			NmsUtils.getVersionAdapter().customDamageEntity(damager, damagee, amount, bossCause);
		} finally {
			DamageUtils.nextEventMetadata = null;

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
