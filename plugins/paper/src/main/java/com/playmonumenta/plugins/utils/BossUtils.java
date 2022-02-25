package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.enchantments.Shielding;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.Map;

public class BossUtils {

	/**
	 * Returns boss health scaling coefficient.
	 * Multiply base health to get final health.
	 * Inverse the coefficient to get damage reduction.
	 * Formula: coefficient = X ^ ((playerCount - 1) ^ Y)
	 * @param playerCount Player amount for health scaling
	 * @param x Base exponent, increase X to increase health scaling. range = [0 < X < 1]
	 * @param y Player count exponent, decrease Y to increase health scaling. range = [0 < Y < 1]
	 */
	public static double healthScalingCoef(int playerCount, double x, double y) {
		double scalingCoef = 0;
		// calculates smallest scaling increase first. largest scaling increase last.
		while (playerCount > 0) {
			double iterCoef = Math.pow(x, Math.pow(playerCount - 1, y));
			scalingCoef += iterCoef;
			playerCount--;
		}
		return scalingCoef;
	}

	public static boolean bossDamageBlocked(Player player, @Nullable Location location) {
		/*
		 * Attacks can only be blocked if:
		 * - They have a source location
		 * - Shield is not on cooldown
		 * - The player is facing towards the damage
		 */
		if (player.isBlocking() && location != null && player.getCooldown(Material.SHIELD) <= 0) {
			Vector playerDir = player.getEyeLocation().getDirection().setY(0).normalize();
			Vector toSourceVector = location.toVector().subtract(player.getLocation().toVector()).setY(0).normalize();
			return playerDir.dot(toSourceVector) > 0.33;
		}

		return false;
	}

	public static void blockableDamage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type, double damage) {
		Location location = null;
		if (damager != null) {
			location = damager.getLocation();
		}
		blockableDamage(damager, damagee, type, damage, null, location);
	}

	public static void blockableDamage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type, double damage, @Nullable Location location) {
		blockableDamage(damager, damagee, type, damage, null, location);
	}

	public static void blockableDamage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type, double damage, @Nullable String cause, @Nullable Location location) {
		// One second of cooldown for every 2.5 points of damage
		blockableDamage(damager, damagee, type, damage, cause, location, (int) (20 * damage / 2.5));
	}

	public static void blockableDamage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type, double damage, @Nullable String cause, @Nullable Location location, int stunTicks) {
		// One shield durability damage for every 5 points of damage
		blockableDamage(damager, damagee, type, damage, false, true, cause, location, stunTicks, (int) (damage / 5));
	}

	public static void blockableDamage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type, double damage, boolean bypassIFrames, boolean causeKnockback, @Nullable String cause, @Nullable Location location, int stunTicks, int durability) {
		if (DamageUtils.isImmuneToDamage(damagee)) {
			return;
		}

		if (damagee instanceof Player player && bossDamageBlocked(player, location)) {
			if (stunTicks > 0) {
				NmsUtils.getVersionAdapter().stunShield(player, stunTicks);
				damagee.getWorld().playSound(damagee.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
			}
			ItemUtils.damageShield(player, durability);
			if (Shielding.doesShieldingApply(player, damager)) {
				Shielding.disable(player);
			}
		} else {
			DamageUtils.damage(damager, damagee, new DamageEvent.Metadata(type, null), damage, bypassIFrames, causeKnockback, false, cause);
			if (damagee instanceof Player player && Shielding.doesShieldingApply(player, damager)) {
				Shielding.disable(player);
			}
		}
	}

	/*
	TODO - fix dualTypeDamage not working
	public static void dualTypeBlockableDamage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type1, DamageType type2, double damage, double percentType1) {
		Location location = null;
		if (damager != null) {
			location = damager.getLocation();
		}
		dualTypeBlockableDamage(damager, damagee, type1, type2, damage, percentType1, null, location);
	}

	public static void dualTypeBlockableDamage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type1, DamageType type2, double damage, double percentType1, @Nullable String cause, @Nullable Location location) {
		dualTypeBlockableDamage(damager, damagee, type1, type2, damage, percentType1, false, true, cause, location, (int) (20 * damage / 2.5), (int) (damage / 5));
	}

	public static void dualTypeBlockableDamage(@Nullable LivingEntity damager, LivingEntity damagee, DamageType type1, DamageType type2, double damage, double percentType1, boolean bypassIFrames, boolean causeKnockback, @Nullable String cause, @Nullable Location location, int stunTicks, int durability) {
		// If they have resistance 5 or are in stasis, do not deal damage or stun shield
		if ((damagee.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE) && damagee.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE).getAmplifier() >= 4) || StasisListener.isInStasis(damagee)) {
			return;
		}

		if (damagee instanceof Player player && bossDamageBlocked(player, location)) {
			if (stunTicks > 0) {
				NmsUtils.getVersionAdapter().stunShield(player, stunTicks);
				damagee.getWorld().playSound(damagee.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
			}
			ItemUtils.damageShield(player, durability);
		} else {
			DamageUtils.dualTypeDamage(damager, damagee, type1, type2, damage, percentType1, null, bypassIFrames, causeKnockback, cause);
		}
	}*/

	public static boolean bossDamagePercent(LivingEntity boss, LivingEntity target, double percentHealth) {
		return bossDamagePercent(boss, target, percentHealth, null, false, null);
	}

	public static boolean bossDamagePercent(LivingEntity boss, LivingEntity target, double percentHealth, @Nullable Location location) {
		return bossDamagePercent(boss, target, percentHealth, location, false, null);
	}

	public static boolean bossDamagePercent(LivingEntity boss, LivingEntity target, double percentHealth, @Nullable Location location, boolean raw) {
		return bossDamagePercent(boss, target, percentHealth, location, raw, null);
	}

	public static boolean bossDamagePercent(LivingEntity boss, LivingEntity target, double percentHealth, String cause) {
		return bossDamagePercent(boss, target, percentHealth, null, false, cause);
	}

	public static boolean bossDamagePercent(LivingEntity boss, LivingEntity target, double percentHealth, @Nullable Location location, @Nullable String cause) {
		return bossDamagePercent(boss, target, percentHealth, location, false, cause);
	}

	/*
	 * Returns whether or not the player survived (true) or was killed (false)
	 */
	public static boolean bossDamagePercent(LivingEntity boss, LivingEntity target, double percentHealth, @Nullable Location location, boolean raw, @Nullable String cause) {
		if (percentHealth <= 0) {
			return true;
		}

		if (DamageUtils.isImmuneToDamage(target)) {
			return true;
		}

		double toTake = raw ? percentHealth : EntityUtils.getMaxHealth(target) * percentHealth;

		if (target instanceof Player player && bossDamageBlocked(player, location)) {
			/*
			 * One second of cooldown for every 2 points of damage
			 * Since this is % based, compute cooldown based on "Normal" health
			 */
			if (raw) {
				if (toTake > 1) {
					NmsUtils.getVersionAdapter().stunShield(player, (int) Math.ceil(toTake * 0.5));
				}
				target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
				ItemUtils.damageShield(player, (int) Math.ceil(toTake / 2.5));
			} else {
				NmsUtils.getVersionAdapter().stunShield(player, (int) (20 * percentHealth * 20));
				target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
				ItemUtils.damageShield(player, (int)(percentHealth * 20 / 2.5));
			}

			if (Shielding.doesShieldingApply(player, boss)) {
				Shielding.disable(player);
			}
		} else {
			double absorp = AbsorptionUtils.getAbsorption(target);
			double adjustedHealth = (target.getHealth() + absorp) - toTake;

			if (adjustedHealth <= 0) {
				// Kill the player, but allow totems to trigger
				target.setNoDamageTicks(0);
				DamageUtils.damage(boss, target, new DamageEvent.Metadata(DamageType.OTHER, null), 1000, false, true, false, cause);
				return false;
			} else {
				double originalDamage = toTake;
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
					if (target.getHealth() - toTake > EntityUtils.getMaxHealth(target)) {
						target.setHealth(EntityUtils.getMaxHealth(target));
					} else {
						target.setHealth(Math.max(target.getHealth() - toTake, 1));
					}
				}

				// deal a small amount of damage for abilities to trigger and the hurt effect to play

				double lastDamage = target.getLastDamage();
				int noDamageTicks = target.getNoDamageTicks();
				target.setNoDamageTicks(0);

				DamageUtils.damage(boss, target, new DamageEvent.Metadata(DamageType.OTHER, null), 0.001, false, true, false, cause);

				if (noDamageTicks <= target.getMaximumNoDamageTicks() / 2f) {
					// had iframes: increase iframes by dealt damage, but keep length
					target.setNoDamageTicks(noDamageTicks);
					target.setLastDamage(lastDamage + originalDamage);
				} else {
					// had no iframes: add iframes for the damage dealt
					target.setNoDamageTicks(target.getMaximumNoDamageTicks());
					target.setLastDamage(originalDamage);
				}
			}

			if (target instanceof Player player && Shielding.doesShieldingApply(player, boss)) {
				Shielding.disable(player);
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

	/**
	 * Adds modifiers to the existing map from the string
	 * @param s a string like "[damage=10, cooldown=60, detectionrange=80, singletarget=true....]
	 * @param map the map where the values ​​will be added
	 */
	public static void addModifiersFromString(Map<String, String> map, String s) {
		if (!s.startsWith("[")) {
			return;
		}
		s = s.substring(1);

		if (s.endsWith("]")) {
			s = s.substring(0, s.length() - 1);
		}
		String[] toMap;
		int lastSplitIndex = 0;
		int squareBrackets = 0;
		int roundBrackets = 0;
		int quoteCount = 0;
		char charAtI;

		for (int i = 0; i < s.length(); i++) {
			charAtI = s.charAt(i);
			switch (charAtI) {
				case '[':
					squareBrackets++;
					break;
				case ']':
					squareBrackets--;
					break;
				case '(':
					roundBrackets++;
					break;
				case ')':
					roundBrackets--;
					break;
				case '"':
					quoteCount = (quoteCount + 1) % 2;
					break;
				default:
			}

			if (squareBrackets == 0 && roundBrackets == 0 && quoteCount == 0 && charAtI == ',') {
				toMap = s.substring(lastSplitIndex, i).split("=");
				if (toMap.length == 2) {
					map.put(toMap[0].replace(" ", "").toLowerCase(), toMap[1]);
				} else {
					Plugin.getInstance().getLogger().warning("Fail to load: " + s.substring(lastSplitIndex, i) + ". Illegal declaration");
				}
				lastSplitIndex = i + 1;
			}
		}

		if (squareBrackets == 0 && roundBrackets == 0 && quoteCount == 0 && lastSplitIndex != s.length()) {
			toMap = s.substring(lastSplitIndex, s.length()).split("=");
			if (toMap.length == 2) {
				map.put(toMap[0].replace(" ", "").toLowerCase(), toMap[1]);
			} else {
				Plugin.getInstance().getLogger().warning("Fail to load: [" + String.join(",", toMap) + "]. Illegal declaration");
			}
		} else {
			Plugin.getInstance().getLogger().warning("Fail too many brackets/quote inside: " + s);
		}
	}

	public static String translateFieldNameToTag(String fieldName) {
		return fieldName.toLowerCase().replaceAll("[^a-z0-9]", "");
	}
}
