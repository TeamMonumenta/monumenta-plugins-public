package com.playmonumenta.plugins.utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class BossUtils {

	public static boolean bossDamageBlocked(@Nonnull Player target, double damage, @Nullable Location source) {
		/*
		 * Attacks can only be blocked if:
		 * - They have a source location
		 * - Shield is not on cooldown
		 * - The damage is less than 100
		 * - The player is facing towards the damage
		 */
		if (target.isBlocking() && source != null && target.getCooldown(Material.SHIELD) <= 0 && damage < 100) {
			Vector playerDir = target.getEyeLocation().getDirection().setY(0).normalize();
			Vector toSourceVector = source.toVector().subtract(target.getLocation().toVector()).setY(0).normalize();
			return playerDir.dot(toSourceVector) > 0.33;
		}

		return false;
	}

	public static void bossDamage(@Nonnull LivingEntity boss, @Nonnull Player target, double damage) {
		bossDamage(boss, target, damage, boss.getLocation());
	}

	/*
	 * Returns true if the damage was applied to the player and
	 */
	public static void bossDamage(@Nonnull LivingEntity boss, @Nonnull Player target, double damage, @Nullable Location source) {
		int resistance = 0;
		if (target.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
			resistance = target.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE).getAmplifier() + 1;
		}

		// Resist 5 = no damage
		if (resistance >= 5) {
			return;
		}

		if (bossDamageBlocked(target, damage, source)) {
			/* One second of cooldown for every 2 points of damage */
			target.setCooldown(Material.SHIELD, (int)(20 * damage / 2.5));
			target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
			ItemUtils.damageShield(target, (int)(damage / 5));
		} else {
			// Don't adjust damage to account for resistance, because target.damage() already does this
			// Apply the damage using a custom damage source that can not be blocked
			NmsUtils.unblockableEntityDamageEntity(target, damage, boss);
		}
	}

	public static boolean bossDamagePercent(@Nonnull LivingEntity boss, @Nonnull Player target, double percentHealth) {
		return bossDamagePercent(boss, target, percentHealth, null, false);
	}

	public static boolean bossDamagePercent(@Nonnull LivingEntity boss, @Nonnull Player target, double percentHealth, @Nullable Location source) {
		return bossDamagePercent(boss, target, percentHealth, source, false);
	}

	/*
	 * Returns whether or not the player survived (true) or was killed (false)
	 */
	public static boolean bossDamagePercent(@Nonnull LivingEntity boss, @Nonnull Player target, double percentHealth, @Nullable Location source, boolean raw) {
		if (target instanceof Player) {
			Player player = target;
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

		double toTake = (target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * percentHealth);
		if (raw) {
			toTake = percentHealth;
		}

		if (bossDamageBlocked(target, 0, source)) {
			/*
			 * One second of cooldown for every 2 points of damage
			 * Since this is % based, compute cooldown based on "Normal" health
			 */
			if (raw) {
				if (toTake > 1) {
					target.setCooldown(Material.SHIELD, (int) Math.ceil(toTake * 0.5));
				}
				target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
				ItemUtils.damageShield(target, (int) Math.ceil(toTake / 2.5));
			} else {
				target.setCooldown(Material.SHIELD, (int)(20 * percentHealth * 20));
				target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
				ItemUtils.damageShield(target, (int)(percentHealth * 20 / 2.5));
			}
		} else {
			float absorp = AbsorptionUtils.getAbsorption(target);
			double adjustedHealth = (target.getHealth() + absorp) - toTake;

			if (adjustedHealth <= 0) {
				// Kill the player, but allow totems to trigger
				NmsUtils.unblockableEntityDamageEntity(target, 1000, boss);
				return false;
			} else {
				if (absorp > 0) {
					if (absorp - toTake > 0) {
						AbsorptionUtils.setAbsorption(target, (float) (absorp - toTake), -1);
						toTake = 0;
					} else {
						AbsorptionUtils.setAbsorption(target, 0f, -1);
						toTake -= absorp;
					}
				}
				if (toTake > 0) {
					target.setHealth(target.getHealth() - toTake);
				}
				NmsUtils.unblockableEntityDamageEntity(target, 1, boss);
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
