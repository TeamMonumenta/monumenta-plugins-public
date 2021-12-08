package com.playmonumenta.plugins.utils;

import java.util.Map;
import java.util.NavigableSet;

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

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.Stasis;

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

	public static void bossDamage(@Nonnull LivingEntity boss, @Nonnull LivingEntity target, double damage) {
		bossDamage(boss, target, damage, boss.getLocation(), null);
	}

	public static void bossDamage(@Nonnull LivingEntity boss, @Nonnull LivingEntity target, double damage, @Nullable Location source) {
		bossDamage(boss, target, damage, source, null);
	}

	public static void bossDamage(@Nonnull LivingEntity boss, @Nonnull LivingEntity target, double damage, @Nullable Location source, String cause) {
		int resistance = 0;
		if (target.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
			resistance = target.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE).getAmplifier() + 1;
		}

		// Resist 5 = no damage
		if (resistance >= 5) {
			return;
		}

		if ((target instanceof Player) && bossDamageBlocked((Player)target, damage, source)) {
			/* One second of cooldown for every 2 points of damage */
			((Player) target).setCooldown(Material.SHIELD, (int)(20 * damage / 2.5));
			target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
			ItemUtils.damageShield((Player) target, (int)(damage / 5));
		} else {
			// Don't adjust damage to account for resistance, because target.damage() already does this
			// Apply the damage using a custom damage source that can not be blocked
			NmsUtils.unblockableEntityDamageEntity(target, damage, boss, cause);
		}
	}

	public static boolean bossDamagePercent(@Nonnull LivingEntity boss, @Nonnull LivingEntity target, double percentHealth) {
		return bossDamagePercent(boss, target, percentHealth, null, false, null);
	}

	public static boolean bossDamagePercent(@Nonnull LivingEntity boss, @Nonnull LivingEntity target, double percentHealth, @Nullable Location source) {
		return bossDamagePercent(boss, target, percentHealth, source, false, null);
	}

	public static boolean bossDamagePercent(@Nonnull LivingEntity boss, @Nonnull LivingEntity target, double percentHealth, @Nullable Location source, boolean raw) {
		return bossDamagePercent(boss, target, percentHealth, source, raw, null);
	}

	public static boolean bossDamagePercent(@Nonnull LivingEntity boss, @Nonnull LivingEntity target, double percentHealth, String cause) {
		return bossDamagePercent(boss, target, percentHealth, null, false, cause);
	}

	public static boolean bossDamagePercent(@Nonnull LivingEntity boss, @Nonnull LivingEntity target, double percentHealth, @Nullable Location source, String cause) {
		return bossDamagePercent(boss, target, percentHealth, source, false, cause);
	}

	/*
	 * Returns whether or not the player survived (true) or was killed (false)
	 */
	public static boolean bossDamagePercent(@Nonnull LivingEntity boss, @Nonnull LivingEntity target, double percentHealth, @Nullable Location source, boolean raw, String cause) {
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

		// Do not damage players currently in stasis
		Plugin plugin = Plugin.getInstance();
		String s = "Stasis";
		NavigableSet<Effect> effects = plugin.mEffectManager.getEffects(target, s);
		if (effects != null && plugin.mEffectManager.getEffects(target, s) != null && (plugin.mEffectManager.getEffects(target, s)).contains(new Stasis(120))) {
			return true;
		}

		double toTake = (target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() * percentHealth);
		if (raw) {
			toTake = percentHealth;
		}

		if ((target instanceof Player) && bossDamageBlocked((Player) target, 0, source)) {
			/*
			 * One second of cooldown for every 2 points of damage
			 * Since this is % based, compute cooldown based on "Normal" health
			 */
			if (raw) {
				if (toTake > 1) {
					((Player) target).setCooldown(Material.SHIELD, (int) Math.ceil(toTake * 0.5));
				}
				target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
				ItemUtils.damageShield((Player) target, (int) Math.ceil(toTake / 2.5));
			} else {
				((Player) target).setCooldown(Material.SHIELD, (int)(20 * percentHealth * 20));
				target.getWorld().playSound(target.getLocation(), Sound.ITEM_SHIELD_BREAK, SoundCategory.PLAYERS, 1.0f, 1.0f);
				ItemUtils.damageShield((Player) target, (int)(percentHealth * 20 / 2.5));
			}
		} else {
			double absorp = AbsorptionUtils.getAbsorption(target);
			double adjustedHealth = (target.getHealth() + absorp) - toTake;

			if (adjustedHealth <= 0) {
				// Kill the player, but allow totems to trigger
				NmsUtils.unblockableEntityDamageEntity(target, 1000, boss, cause);
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
					if (target.getHealth() - toTake > target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) {
						target.setHealth(target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
					} else {
						target.setHealth(Math.max(target.getHealth() - toTake, 1));
					}
				}
				//TODO B#9334: test if this is doing more damage than the provided percentage - the intended amount
				// Also test if iframes can eat this part of the damage and prevent events from triggering
				NmsUtils.unblockableEntityDamageEntity(target, 1, boss, cause);
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
				Plugin.getInstance().getLogger().warning("Fail to load: " + toMap + ". Illegal declaration");
			}
		} else {
			Plugin.getInstance().getLogger().warning("Fail too many brackets/quote inside: " + s);
		}
	}

	public static String translateFieldNameToTag(String fieldName) {
		return fieldName.toLowerCase().replaceAll("[^a-z0-9]", "");
	}
}
