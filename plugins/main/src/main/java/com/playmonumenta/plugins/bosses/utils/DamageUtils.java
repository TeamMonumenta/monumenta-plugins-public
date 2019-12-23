package com.playmonumenta.plugins.bosses.utils;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.events.BossAbilityDamageEvent;
import com.playmonumenta.plugins.utils.AbsorptionUtils;

public class DamageUtils {

	public static void damage(LivingEntity boss, LivingEntity target, double damage) {
		int resistance = 0;
		if (target.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
			resistance = target.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE).getAmplifier() + 1;
		}

		// Resist 5 = no damage
		if (resistance >= 5) {
			return;
		}

		// Don't adjust damage to account for resistance, because target.damage() already does this

		BossAbilityDamageEvent event = new BossAbilityDamageEvent(boss, target, damage);
		Bukkit.getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			if (boss != null) {
				target.damage(event.getDamage(), boss);
			} else {
				target.damage(event.getDamage());
			}
		}
	}

	public static void damagePercent(LivingEntity boss, LivingEntity target, double percentHealth) {
		if (target instanceof Player) {
			Player player = (Player) target;
			if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
				return;
			}
		}

		int resistance = 0;
		if (target.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
			resistance = target.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE).getAmplifier() + 1;
		}

		// Resist 5 = no damage
		if (resistance >= 5) {
			return;
		}

		// Resistance reduces percent HP damage
		percentHealth *= (1 - 0.2*resistance);

		double toTake = (target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * percentHealth);
		BossAbilityDamageEvent event = new BossAbilityDamageEvent(boss, target, toTake);
		Bukkit.getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			toTake = event.getDamage();
			float absorp = AbsorptionUtils.getAbsorption(target);
			double adjustedHealth = (target.getHealth() + absorp) - toTake;

			if (adjustedHealth <= 0) {
				// Kill the player, but allow totems to trigger
				target.damage(100, boss);
			} else {
				if (absorp > 0) {
					if (absorp - toTake > 0) {
						AbsorptionUtils.setAbsorption(target, (float) (absorp - toTake));
						toTake = 0;
					} else {
						AbsorptionUtils.setAbsorption(target, 0f);
						toTake -= absorp;
					}
				}
				if (toTake > 0) {
					target.setHealth(target.getHealth() - toTake);
				}
				target.damage(1, boss);
			}
		}
	}
}
