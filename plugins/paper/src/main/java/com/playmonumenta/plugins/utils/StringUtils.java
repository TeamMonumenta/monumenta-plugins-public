package com.playmonumenta.plugins.utils;

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

		int l =  romanMap.floorKey(number);
		if (number == l) {
			return romanMap.get(number);
		}
		return romanMap.get(l) + toRoman(number - l);
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
}
