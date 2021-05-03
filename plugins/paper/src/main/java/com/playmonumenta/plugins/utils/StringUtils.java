package com.playmonumenta.plugins.utils;

import org.bukkit.attribute.Attribute;

import java.util.TreeMap;

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

		int l = romanMap.floorKey(number);
		if (number == l) {
			return romanMap.get(number);
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

	public static String ticksToTime(int ticks) {
		int minutes = ((ticks / 20) / 60);
		int seconds = ((ticks - ((minutes * 60) * 20))) / 20;

		String time = "";
		if (minutes > 0) {
			time = minutes + " minutes ";
		}

		time += seconds + " seconds";


		return time;
	}

	public static String convertToInvisibleLoreLine(String s) {
		StringBuilder hidden = new StringBuilder();
		for (char c : s.toCharArray()) {
			hidden.append("§");
			hidden.append(c);
		}
		return hidden.toString();
	}

	public static String convertToVisibleLoreLine(String s) {
		return s.replace("§", "");
	}

	public static String getAttributeName(Attribute attribute) {
		//WARNING THIS IS GONNA NEED A CHANGE WHEN GOING 1.16 (attribute names should change)
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
}
