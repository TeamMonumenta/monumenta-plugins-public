package com.playmonumenta.plugins.utils;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.potion.PotionEffectType;

public class BossUtils {
	public static class BossAbilityDamageEvent extends Event implements Cancellable {
		private static final HandlerList handlers = new HandlerList();
		private boolean isCancelled;
		private LivingEntity mBoss;
		private Player mDamaged;
		private double mDamage;

		public BossAbilityDamageEvent(LivingEntity boss, Player damaged, double damage) {
			mBoss = boss;
			mDamaged = damaged;
			mDamage = damage;
		}

		public LivingEntity getBoss() {
			return mBoss;
		}

		public Player getDamaged() {
			return mDamaged;
		}

		public void setDamage(double damage) {
			mDamage = damage;
		}

		public double getDamage() {
			return mDamage;
		}

		@Override
		public boolean isCancelled() {
			return isCancelled;
		}

		@Override
		public void setCancelled(boolean arg0) {
			this.isCancelled = arg0;
		}

		// Mandatory Event Methods (If you remove these, I'm 99% sure the event will break)

		@Override
		public HandlerList getHandlers() {
			return handlers;
		}

		public static HandlerList getHandlerList() {
			return handlers;
		}
	}

	public static void bossDamage(LivingEntity boss, Player target, double damage) {
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

	/*
	 * Returns whether or not the player survived (true) or was killed (false)
	 */
	public static boolean bossDamagePercent(LivingEntity boss, Player target, double percentHealth) {
		if (target instanceof Player) {
			Player player = (Player) target;
			if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
				return true;
			}
		}

		int resistance = 0;
		if (target.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
			resistance = target.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE).getAmplifier() + 1;
		}

		// Resist 5 = no damage
		if (resistance >= 5) {
			return true;
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
				return false;
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
		return true;
	}
}
