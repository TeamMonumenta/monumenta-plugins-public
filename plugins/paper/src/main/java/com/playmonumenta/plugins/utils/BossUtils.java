package com.playmonumenta.plugins.utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class BossUtils {
	public static class BossAbilityDamageEvent extends Event implements Cancellable {
		private static final HandlerList handlers = new HandlerList();
		private boolean isCancelled = false;
		private final LivingEntity mBoss;
		private final Player mDamaged;
		private double mDamage;
		private boolean mIsPlayerBlocking;

		public BossAbilityDamageEvent(LivingEntity boss, Player damaged, double damage, boolean isPlayerBlocking) {
			mBoss = boss;
			mDamaged = damaged;
			mDamage = damage;
			mIsPlayerBlocking = isPlayerBlocking;
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

		public boolean isPlayerBlocking() {
			return mIsPlayerBlocking;
		}

		public void setPlayerBlocking(boolean blocking) {
			mIsPlayerBlocking = blocking;
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

	@FunctionalInterface
	public interface BossAbilityDamageEventModifier {
		void run(BossAbilityDamageEvent event);
	}

	public static void bossDamage(@Nonnull LivingEntity boss, Player target, double damage) {
		bossDamage(boss, target, damage, (BossAbilityDamageEventModifier)null);
	}

	public static void bossDamage(@Nonnull LivingEntity boss, @Nonnull Player target, double damage, @Nullable BossAbilityDamageEventModifier modifier) {
		bossDamage(boss, target, damage, boss.getLocation(), modifier);
	}

	public static void bossDamage(@Nonnull LivingEntity boss, @Nonnull Player target, double damage, @Nullable Location source) {
		bossDamage(boss, target, damage, source, null);
	}

	/*
	 * Add an optional lambda argument which has the last chance to modify the event (and potentially do other things)
	 * This is useful for skills with non-standard shield blocking behavior
	 *
	 * This handler will not be called if the ability damage event is cancelled (since every use case involves only non-cancelled events)
	 */
	public static void bossDamage(@Nonnull LivingEntity boss, @Nonnull Player target, double damage, @Nullable Location source, @Nullable BossAbilityDamageEventModifier modifier) {
		int resistance = 0;
		if (target.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
			resistance = target.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE).getAmplifier() + 1;
		}

		// Resist 5 = no damage
		if (resistance >= 5) {
			return;
		}

		boolean blocked = false;
		if (target.isBlocking() && source != null && target.getCooldown(Material.SHIELD) <= 0 && damage < 100) {
			/*
			 * Attacks can only be blocked if:
			 * - They have a source location
			 * - Shield is not on cooldown
			 * - The damage is less than 100
			 * - The player is facing towards the damage
			 */
			Vector playerDir = target.getEyeLocation().getDirection().setY(0).normalize();
			Vector toSourceVector = source.toVector().subtract(target.getLocation().toVector()).setY(0).normalize();
			if (playerDir.dot(toSourceVector) > 0.33) {
				blocked = true;
			}
		}

		BossAbilityDamageEvent event = new BossAbilityDamageEvent(boss, target, damage, blocked);
		Bukkit.getPluginManager().callEvent(event);
		if (!event.isCancelled() && modifier != null) {
			modifier.run(event);
		}
		if (!event.isCancelled()) {
			if (event.isPlayerBlocking()) {
				/* One second of cooldown for every 2 points of damage */
				target.setCooldown(Material.SHIELD, (int)(20 * event.getDamage() / 2.5));
				target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
				ItemUtils.damageShield(target, (int)(event.getDamage() / 5));
			} else {
				// Don't adjust damage to account for resistance, because target.damage() already does this
				// Apply the damage using a custom damage source that can not be blocked
				NmsUtils.unblockableEntityDamageEntity(target, event.getDamage(), boss);
			}
		}
	}

	public static boolean bossDamagePercent(@Nonnull LivingEntity boss, @Nonnull Player target, double percentHealth) {
		return bossDamagePercent(boss, target, percentHealth, (BossAbilityDamageEventModifier)null);
	}

	public static boolean bossDamagePercent(@Nonnull LivingEntity boss, @Nonnull Player target, double percentHealth, @Nullable BossAbilityDamageEventModifier modifier) {
		return bossDamagePercent(boss, target, percentHealth, boss.getLocation(), modifier);
	}

	public static boolean bossDamagePercent(@Nonnull LivingEntity boss, @Nonnull Player target, double percentHealth, @Nullable Location source) {
		return bossDamagePercent(boss, target, percentHealth, source, null);
	}

	/*
	 * Returns whether or not the player survived (true) or was killed (false)
	 */
	public static boolean bossDamagePercent(@Nonnull LivingEntity boss, @Nonnull Player target, double percentHealth, @Nullable Location source, @Nullable BossAbilityDamageEventModifier modifier) {
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

		boolean blocked = false;
		if (target.isBlocking() && source != null && target.getCooldown(Material.SHIELD) <= 0) {
			/* Attacks can only be blocked if they have a source direction and the player is facing that way */
			Vector playerDir = target.getEyeLocation().getDirection().setY(0).normalize();
			Vector toSourceVector = source.toVector().subtract(target.getLocation().toVector()).setY(0).normalize();
			if (playerDir.dot(toSourceVector) > 0.33) {
				blocked = true;
			}
		}

		// Resistance reduces percent HP damage
		percentHealth *= (1 - 0.2*resistance);

		double toTake = (target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * percentHealth);
		BossAbilityDamageEvent event = new BossAbilityDamageEvent(boss, target, toTake, blocked);
		Bukkit.getPluginManager().callEvent(event);
		if (!event.isCancelled() && modifier != null) {
			modifier.run(event);
		}
		if (!event.isCancelled()) {
			if (event.isPlayerBlocking()) {
				/*
				 * One second of cooldown for every 2 points of damage
				 * Since this is % based, compute cooldown based on "Normal" health
				 */
				target.setCooldown(Material.SHIELD, (int)(20 * percentHealth * 20));
				target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
				ItemUtils.damageShield(target, (int)(percentHealth * 20 / 2.5));
			} else {
				toTake = event.getDamage();
				float absorp = AbsorptionUtils.getAbsorption(target);
				double adjustedHealth = (target.getHealth() + absorp) - toTake;

				if (adjustedHealth <= 0) {
					// Kill the player, but allow totems to trigger
					NmsUtils.unblockableEntityDamageEntity(target, 1000, boss);
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
					NmsUtils.unblockableEntityDamageEntity(target, 1, boss);
				}
			}
		}
		return true;
	}

	public static int getPlayersInRangeForHealthScaling(Entity entity, double radius) {
		return getPlayersInRangeForHealthScaling(entity.getLocation(), radius);
	}

	public static int getPlayersInRangeForHealthScaling(Location loc, double radius) {
		return PlayerUtils.playersInRange(loc, radius, true).size();
	}
}
