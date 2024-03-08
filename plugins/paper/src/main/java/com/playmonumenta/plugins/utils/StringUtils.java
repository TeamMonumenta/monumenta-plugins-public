package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.events.DamageEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.jetbrains.annotations.Nullable;

public class StringUtils {
	public static final Pattern UNNATURAL_CHARACTERS_RE = Pattern.compile("[^a-zA-Z0-9]+");
	public static final TreeMap<Integer, String> TO_ROMAN_MAP = new TreeMap<>();
	public static final TreeSet<String> NAME_PREFIXES;
	public static final TreeSet<String> LOWERCASE_NAME_PREFIXES;

	static {
		TO_ROMAN_MAP.put(1000, "M");
		TO_ROMAN_MAP.put(900, "CM");
		TO_ROMAN_MAP.put(500, "D");
		TO_ROMAN_MAP.put(400, "CD");
		TO_ROMAN_MAP.put(100, "C");
		TO_ROMAN_MAP.put(90, "XC");
		TO_ROMAN_MAP.put(50, "L");
		TO_ROMAN_MAP.put(40, "XL");
		TO_ROMAN_MAP.put(10, "X");
		TO_ROMAN_MAP.put(9, "IX");
		TO_ROMAN_MAP.put(5, "V");
		TO_ROMAN_MAP.put(4, "IV");
		TO_ROMAN_MAP.put(1, "I");

		NAME_PREFIXES = new TreeSet<>(Set.of(
			// Tlaxan
			"C'",
			"R'",
			"Ta'",
			"T'",
			"Z'",

			// English articles
			"An ",
			"A ",
			"The ",

			// English honorifics (wikipedia: https://en.wikipedia.org/wiki/English_honorifics)
			// Common titles
			"Master ",
			"Mr ",
			"Mr. ",
			"Mister ", // For some reason not explicitly listed
			"Miss ",
			"Mrs ",
			"Mrs. ",
			"Ms ",
			"Ms. ",
			"Mx ",
			"Mx. ",

			// Formal titles
			"Sir ",
			"Gentleman ",
			"Sire ",
			"Mistress ",
			"Madam ",
			"Ma'am ",
			"Dame ",
			"Lord ",
			"Baron ",
			"Viscount ",
			"Count ",
			"Earl ",
			"Marquess ",
			"Lady ",
			"Baroness ",
			"Viscountess ",
			"Countess ",
			"Marchioness ",
			"Esq ",
			"Excellency ",
			"His Honor ",
			"His Honour ",
			"Her Honor ",
			"Her Honour ",
			"The Honorable ",
			"The Honourable ",
			"The Right Honorable ",
			"The Right Honourable ",
			"The Most Honorable ",
			"The Most Honourable ",

			// Academic and professional titles
			"Dr ",
			"Dr. ",
			"Doctor ",
			"Doc ",
			"PhD ",
			"Ph.D. ",
			"MD ",
			"M.D. ",
			"Professor ",
			"Prof ",
			"Cl ",
			"SCl ",
			"Chancellor ",
			"Vice-Chancellor ",
			"Principal ",
			"Vice-Principal ",
			"President ",
			"Vice-President ",
			// "Master ", already listed above
			"Warden ",
			"Dean ",
			"Regent ",
			"Rector ",
			"Provost ",
			"Director ",
			"Chief Executive ",

			// How are these not listed?
			"King ",
			"Queen ",
			"Duchess "
		));
		LOWERCASE_NAME_PREFIXES = NAME_PREFIXES
			.stream()
			.map(s -> s.toLowerCase(Locale.ROOT))
			.collect(Collectors.toCollection(TreeSet::new));
	}

	public static String toRoman(int number) {
		Map.Entry<Integer, String> entry = TO_ROMAN_MAP.floorEntry(number);
		Integer l = entry.getKey();
		if (l == null) {
			return String.valueOf(number);
		}
		if (l == number) {
			return entry.getValue();
		}
		return entry.getValue() + toRoman(number - l);
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

	public static String ticksToMilliseconds(int i) {
		int ticks = i % 20;
		return ticks == 0 ? "000" : ticks == 1 ? "050" : String.valueOf(ticks * 50);
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

	// 0 to 0
	// 0.05 to 5
	// 0.1 to 10
	// 0.333333 to 33.33
	// 0.25789 to 25.79
	// 12.345678 to 1234.57
	public static String multiplierToPercentage(double multiplier) {
		return to2DP(multiplier * 100);
	}

	public static String multiplierToPercentageWithSign(double multiplier) {
		return multiplierToPercentage(multiplier) + "%";
	}

	// 0 to 0
	// 5 to 0.25
	// 10 to 0.5
	// 20 to 1
	// 1000 to 50
	public static String ticksToSeconds(int ticks) {
		return to2DP(ticks / 20d);
	}

	public static String ticksToSeconds(double ticks) {
		return ticksToSeconds((int) ticks);
	}

	// Converts to 2dp with no trailing zeros, for display purposes
	public static String to2DP(double value) {
		return new DecimalFormat("#.##").format(value);
	}

	public static Component doubleToColoredAndSignedPercentage(double d) {
		String percent = multiplierToPercentage(d);
		if (d < 0) {
			return Component.text(percent + "%", NamedTextColor.RED);
		} else {
			return Component.text("+" + percent + "%", NamedTextColor.GREEN);
		}
	}

	public static String formatDecimal(double d) {
		if (d == (int) d) {
			return Integer.toString((int) d);
		} else {
			return Double.toString(d);
		}
	}

	public static String getDamageTypeString(@Nullable EnumSet<DamageEvent.DamageType> types) {
		String string = "";

		if (types != null) {
			for (DamageEvent.DamageType type : types) {
				String typeString;
				switch (type) {
					case MELEE, MELEE_ENCH, MELEE_SKILL -> typeString = "Melee";
					case PROJECTILE, PROJECTILE_SKILL -> typeString = "Projectile";
					case MAGIC -> typeString = "Magic";
					case BLAST -> typeString = "Blast";
					case FALL -> typeString = "Fall";
					case FIRE -> typeString = "Fire";
					// We don't care about other types for now
					default -> typeString = null;
				}
				if (typeString != null) {
					if (!string.isEmpty() && !typeString.equals(string)) {
						// There's two different types here - don't specify
						string = "";
						break;
					}
					string = typeString;
				}
			}
		}

		// Add a space before
		if (!string.isEmpty()) {
			string = " " + string;
		}

		return string;
	}

	public static String capitalizeWords(String str) {
		char[] chars = str.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if (i == 0 || chars[i - 1] == ' ') {
				if (Character.isLowerCase(chars[i])) {
					chars[i] = Character.toUpperCase(chars[i]);
				}
			} else {
				if (Character.isUpperCase(chars[i])) {
					chars[i] = Character.toLowerCase(chars[i]);
				}
			}
		}
		return String.valueOf(chars);
	}

	public static String intToOrdinal(int i) {
		int rightDigit = i % 10;
		switch (rightDigit) {
			case 1 -> {
				return i + "st";
			}
			case 2 -> {
				return i + "nd";
			}
			case 3 -> {
				return i + "rd";
			}
			default -> {
				return i + "th";
			}
		}
	}

	public static @Nullable String getLongestPrefix(NavigableSet<String> prefixMap, String strWithPrefix) {
		while (!prefixMap.isEmpty()) {
			String previousPrefix = prefixMap.floor(strWithPrefix);
			if (previousPrefix == null) {
				return null;
			}
			if (strWithPrefix.startsWith(previousPrefix)) {
				return previousPrefix;
			}
			prefixMap = prefixMap.headSet(previousPrefix, false);
		}
		return null;
	}

	public static String getNaturalSortKey(String str) {
		String sortPrefix = str.toLowerCase(Locale.ROOT);
		List<String> prefixes = new ArrayList<>();
		while (true) {
			String prefix = getLongestPrefix(LOWERCASE_NAME_PREFIXES, sortPrefix);
			if (prefix == null) {
				break;
			}
			prefixes.add(prefix);
			sortPrefix = sortPrefix.substring(prefix.length());
		}
		StringBuilder stringBuilder = new StringBuilder(sortPrefix);
		for (String prefix : prefixes) {
			stringBuilder.append(prefix);
		}

		String naturalSameCase = UNNATURAL_CHARACTERS_RE
			.matcher(stringBuilder
				.toString())
			.replaceAll("");
		return naturalSameCase + " " + str;
	}

	public static List<String> sortedStrings(Collection<String> unsorted) {
		TreeMap<String, String> sortedPlayers = new TreeMap<>();
		for (String string : unsorted) {
			String sortKey = StringUtils.getNaturalSortKey(string);
			sortedPlayers.put(sortKey, string);
		}
		return new ArrayList<>(sortedPlayers.values());
	}

	private static final Set<Character> DIGITS = Set.of('0', '1', '2', '3', '4', '5', '6', '7', '8', '9');

	// Returns the positive integer that is at the end of s; if there is no integer, returns -1.
	public static int getEndingInteger(String s) {
		char[] chars = s.toCharArray();
		for (int i = chars.length - 1; i > 0; i--) {
			if (!DIGITS.contains(chars[i])) {
				if (i == chars.length - 1) {
					return -1;
				}
				return Integer.parseInt(s.substring(i + 1));
			}
		}
		// We only get to this point if every character is an integer
		return Integer.parseInt(s);
	}
}
