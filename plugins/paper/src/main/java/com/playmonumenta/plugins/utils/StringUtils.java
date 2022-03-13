package com.playmonumenta.plugins.utils;

import java.text.DecimalFormat;
import java.util.TreeMap;
import org.bukkit.attribute.Attribute;

public class StringUtils {

	public static String toRoman(int number) {
		TreeMap<Integer, String> romanMap = new TreeMap<>();

		romanMap.put(1000, "M");
		romanMap.put(900, "CM");
		romanMap.put(500, "D");
		romanMap.put(400, "CD");
		romanMap.put(100, "C");
		romanMap.put(90, "XC");
		romanMap.put(50, "L");
		romanMap.put(40, "XL");
		romanMap.put(10, "X");
		romanMap.put(9, "IX");
		romanMap.put(5, "V");
		romanMap.put(4, "IV");
		romanMap.put(1, "I");

		Integer l = romanMap.floorKey(number);
		if (l == null) {
			return "" + number;
		}
		if (l == number) {
			return "" + romanMap.get(l);
		}
		return romanMap.get(l) + toRoman(number - l);
	}

	public static int toArabic(String number) {
		if (number.startsWith("M")) {
			return 1000 + toArabic(number.substring(1));
		} else if (number.startsWith("CM")) {
			return 900 + toArabic(number.substring(2));
		} else if (number.startsWith("D")) {
			return 500 + toArabic(number.substring(1));
		} else if (number.startsWith("CD")) {
			return 400 + toArabic(number.substring(2));
		} else if (number.startsWith("C")) {
			return 100 + toArabic(number.substring(1));
		} else if (number.startsWith("XC")) {
			return 90 + toArabic(number.substring(2));
		} else if (number.startsWith("L")) {
			return 50 + toArabic(number.substring(1));
		} else if (number.startsWith("XL")) {
			return 40 + toArabic(number.substring(2));
		} else if (number.startsWith("X")) {
			return 10 + toArabic(number.substring(1));
		} else if (number.startsWith("IX")) {
			return 9 + toArabic(number.substring(2));
		} else if (number.startsWith("V")) {
			return 5 + toArabic(number.substring(1));
		} else if (number.startsWith("IV")) {
			return 4 + toArabic(number.substring(2));
		} else if (number.startsWith("I")) {
			return 1 + toArabic(number.substring(1));
		}
		return 0;
	}

	public static String intToMinuteAndSeconds(int i) {
		int minutes = i / 60;
		int seconds = i % 60;
		if (seconds < 10) {
			return minutes + ":0" + seconds;
		} else {
			return minutes + ":" + seconds;
		}
	}

	public static String longToHoursMinuteAndSeconds(long i) {
		long seconds = i % 60;
		long minutes = i / 60;
		long hours = minutes / 60;
		minutes %= 60;
		return String.format("%d:%02d:%02d", hours, minutes, seconds);
	}

	public static String ticksToTime(int ticks) {
		int minutes = (ticks / 20) / 60;
		int seconds = (ticks - ((minutes * 60) * 20)) / 20;

		String time = "";
		if (minutes > 0) {
			time = minutes + " minutes ";
		}

		time += seconds + " seconds";


		return time;
	}

	public static String getAttributeName(Attribute attribute) {
		switch (attribute) {
			case GENERIC_ATTACK_SPEED:
				return "generic.attackSpeed";
			case GENERIC_LUCK:
				return "generic.luck";
			case GENERIC_MAX_HEALTH:
				return "generic.maxHealth";
			case GENERIC_ARMOR:
				return "generic.armor";
			case HORSE_JUMP_STRENGTH:
				return "horse.jumpStrength";
			case GENERIC_FLYING_SPEED:
				return "generic.flyingSpeed";
			case GENERIC_FOLLOW_RANGE:
				return "generic.followRange";
			case GENERIC_ATTACK_DAMAGE:
				return "generic.attackDamage";
			case GENERIC_MOVEMENT_SPEED:
				return "generic.movementSpeed";
			case GENERIC_ARMOR_TOUGHNESS:
				return "generic.armorToughness";
			case ZOMBIE_SPAWN_REINFORCEMENTS:
				return "zombie.spawnReinforcements";
			case GENERIC_KNOCKBACK_RESISTANCE:
				return "generic.knockbackResistance";
			default:
				return attribute.toString();
		}
	}

	// 0 to 0%
	// 0.05 to 5%
	// 0.1 to 10%
	// 0.333333 to 33.33%
	// 0.25789 to 25.79%
	// 12.345678 to 1234.57%
	public static String multiplierToPercentage(double multiplier) {
		return to2DP(multiplier * 100);
	}

	// 0 to 0s
	// 5 to 0.25s
	// 10 to 0.5s
	// 20 to 1s
	// 1000 to 50s
	public static String ticksToSeconds(int ticks) {
		return to2DP(ticks / 20d);
	}

	// Converts to 2dp with no trailing zeros, for display purposes
	public static String to2DP(double value) {
		return new DecimalFormat("#.##").format(value);
	}
}
