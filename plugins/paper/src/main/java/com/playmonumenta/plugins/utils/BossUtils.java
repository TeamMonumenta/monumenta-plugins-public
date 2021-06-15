package com.playmonumenta.plugins.utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.playmonumenta.plugins.Plugin;

import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
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
			double absorp = AbsorptionUtils.getAbsorption(target);
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
					if (target.getHealth() - toTake > target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) {
						target.setHealth(target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
					} else {
						target.setHealth(Math.max(target.getHealth() - toTake, 1));
					}
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

	/**
	 * @param s a string like "[damage=10, cooldown=60, detectionrange=80, singletarget=true....]
	 * @param map the map where the values ​​will be added
	 */
	public static void addModifiersFromString(Map<String, String> map, String s) {
		if (!s.startsWith("[")) {
			return;
		}
		s = s.replace("[", "").replace("]", "").replace(" ", "");
		String[] toMap;
		for (String mod : s.split(",")) {
			toMap = mod.split("=");
			if (toMap.length == 2) {
				map.put(toMap[0], toMap[1]);
			} else {
				Plugin.getInstance().getLogger().warning("Fail to load: " + mod + ". Illegal declaration");
			}
		}
	}

	public static Map<String, String> getModifiersFromIdentityTag(LivingEntity boss, String idTag) {
		String modTag = idTag + "[";
		Map<String, String> map = new HashMap<>();

		for (String tag : boss.getScoreboardTags()) {
			if (tag.startsWith(modTag)) {
				String found = tag.replace(idTag, "").toLowerCase();
				addModifiersFromString(map, found);
			}
		}

		return map;
	}

	private static String translateFieldNameToTag(String fieldName) {
		return fieldName.toLowerCase().replaceAll("[^a-z0-9]", "");
	}

	public static <T> T getParameters(LivingEntity boss, String identityTag, T parameters) {
		Map<String, String> modMap = BossUtils.getModifiersFromIdentityTag(boss, identityTag);

		for (Field field : parameters.getClass().getFields()) {
			Class<?> t = field.getType();
			String fieldName = field.getName();

			try {
				String fieldValueOrDefault = modMap.getOrDefault(translateFieldNameToTag(fieldName), field.get(parameters).toString());

				if (t.equals(boolean.class)) {
					field.set(parameters, Boolean.parseBoolean(fieldValueOrDefault));
				} else if (t.equals(int.class)) {
					field.set(parameters, Integer.parseInt(fieldValueOrDefault));
				} else if (t.equals(long.class)) {
					field.set(parameters, Long.parseLong(fieldValueOrDefault));
				} else if (t.equals(float.class)) {
					field.set(parameters, Float.parseFloat(fieldValueOrDefault));
				} else if (t.equals(double.class)) {
					field.set(parameters, Double.parseDouble(fieldValueOrDefault));
				} else if (t.equals(PotionEffectType.class)) {
					field.set(parameters, PotionEffectType.getByName(fieldValueOrDefault.toUpperCase()));
				} else if (t.equals(Particle.class)) {
					field.set(parameters, Particle.valueOf(fieldValueOrDefault.toUpperCase()));
				} else if (t.equals(Sound.class)) {
					field.set(parameters, Sound.valueOf(fieldValueOrDefault.toUpperCase()));
				} else if (t.equals(Color.class)) {
					field.set(parameters, colorFromString(fieldValueOrDefault));
				} else if (t.equals(String.class)) {
					field.set(parameters, fieldValueOrDefault);
				}
			} catch (Exception ex) {
				Plugin.getInstance().getLogger().warning("Failed to parse boss argument field " + fieldName + " for boss " + identityTag);
			}
		}

		return parameters;
	}

	public static final Map<String, Color> COLOR_MAP = new HashMap<>();

	static {
		//this is just because Color don't have the fuctions values() and getName()...
		COLOR_MAP.put("AQUA", Color.AQUA);
		COLOR_MAP.put("BLACK", Color.BLACK);
		COLOR_MAP.put("BLUE", Color.BLUE);
		COLOR_MAP.put("FUCHSIA", Color.FUCHSIA);
		COLOR_MAP.put("GRAY", Color.GRAY);
		COLOR_MAP.put("GREEN", Color.GREEN);
		COLOR_MAP.put("LIME", Color.LIME);
		COLOR_MAP.put("MAROON", Color.MAROON);
		COLOR_MAP.put("NAVY", Color.NAVY);
		COLOR_MAP.put("OLIVE", Color.OLIVE);
		COLOR_MAP.put("ORANGE", Color.ORANGE);
		COLOR_MAP.put("PURPLE", Color.PURPLE);
		COLOR_MAP.put("RED", Color.RED);
		COLOR_MAP.put("SILVER", Color.SILVER);
		COLOR_MAP.put("TEAL", Color.TEAL);
		COLOR_MAP.put("WHITE", Color.WHITE);
		COLOR_MAP.put("YELLOW", Color.YELLOW);
	}

	public static Color colorFromString(String hexStringOrName) throws Exception {
		Color color = COLOR_MAP.get(hexStringOrName);
		if (color == null) {
			if (hexStringOrName.startsWith("#")) {
				hexStringOrName = hexStringOrName.substring(1);
			}
			color = Color.fromRGB(Integer.parseInt(hexStringOrName, 16));
		}

		if (color == null) {
			throw new Exception("Unable to parse color: " + hexStringOrName);
		}
		return color;
	}
}
