package com.playmonumenta.bossfights.utils;

import java.lang.reflect.InvocationTargetException;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.events.BossAbilityDamageEvent;

public class DamageUtils {

	public static void damage(LivingEntity boss, LivingEntity target, double damage) {
		if (boss != null) {
			BossAbilityDamageEvent event = new BossAbilityDamageEvent(boss, target, damage);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				target.damage(event.getDamage(), boss);
			}
		} else {
			BossAbilityDamageEvent event = new BossAbilityDamageEvent(boss, target, damage);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				target.damage(event.getDamage());
			}
		}
	}

	private static java.lang.reflect.Method cachedHandleMethod = null;
	private static java.lang.reflect.Method cachedGetAbsorpMethod = null;
	private static java.lang.reflect.Method cachedSetAbsorpMethod = null;
	private static float getAbsorp(LivingEntity target) {
		try {
				cachedHandleMethod = target.getClass().getMethod("getHandle");
			Object handle = cachedHandleMethod.invoke(target);

				cachedGetAbsorpMethod = handle.getClass().getMethod("getAbsorptionHearts");
				cachedSetAbsorpMethod = handle.getClass().getMethod("setAbsorptionHearts", float.class);
			return (Float)cachedGetAbsorpMethod.invoke(handle);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
			/* If error, return 0 rather than making caller handle the error */
			return 0;
		}
	}

	public static void setAbsorp(LivingEntity target, float value) {
		try {
			cachedHandleMethod = target.getClass().getMethod("getHandle");
			Object handle = cachedHandleMethod.invoke(target);

			cachedSetAbsorpMethod = handle.getClass().getMethod("setAbsorptionHearts", float.class);
			cachedSetAbsorpMethod.invoke(handle, Math.max(0f, value));
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public static void damagePercent(LivingEntity boss, LivingEntity target, double percentHealth) {
		if (target instanceof Player) {
			Player player = (Player) target;
			if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
				return;
			}
		}
		double toTake = (target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * percentHealth);
		BossAbilityDamageEvent event = new BossAbilityDamageEvent(boss, target, toTake);
		Bukkit.getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			toTake = event.getDamage();
			float absorp = getAbsorp(target);
			double adjustedHealth = (target.getHealth() + absorp) - toTake;

			if (adjustedHealth <= 0) {
				// Kill the player, but allow totems to trigger
				target.damage(100, boss);
			} else {
				if (absorp > 0) {
					if (absorp - toTake > 0) {
						setAbsorp(target, (float) (absorp - toTake));
						toTake = 0;
					} else {
						setAbsorp(target, 0f);
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

	public static void damageFlat(LivingEntity boss, LivingEntity target, float health) {
		if (target instanceof Player) {
			Player player = (Player) target;
			if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
				return;
			}
		}
		double toTake = (target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * health);
		BossAbilityDamageEvent event = new BossAbilityDamageEvent(boss, target, toTake);
		Bukkit.getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			toTake = event.getDamage();
			float absorp = getAbsorp(target);
			double adjustedHealth = (target.getHealth() + absorp) - toTake;

			if (adjustedHealth <= 0) {
				// Kill the player, but allow totems to trigger
				target.damage(100, boss);
			} else {
				if (absorp > 0) {
					if (absorp - toTake > 0) {
						setAbsorp(target, (float) (absorp - toTake));
						toTake = 0;
					} else {
						setAbsorp(target, 0f);
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
