package com.playmonumenta.plugins.bosses.parameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffectType;

public class StringReader {
	private final String mStr;
	private int mIdx = 0;

	public StringReader(String str) {
		mStr = str;
	}

	public String getString() {
		return mStr;
	}

	public boolean canRead() {
		return mIdx < mStr.length();
	}

	public String peek() {
		if (!canRead()) {
			return "";
		}
		return mStr.substring(mIdx, mIdx + 1);
	}

	public boolean peek(String desired) {
		if (!canRead()) {
			return false;
		}
		return remaining().startsWith(desired);
	}

	/* Returns the part of the string processed so far */
	public String readSoFar() {
		return mStr.substring(0, mIdx);
	}

	/* Returns the remainder of the string */
	public String remaining() {
		if (mIdx >= mStr.length()) {
			return "";
		}
		return mStr.substring(mIdx);
	}

	/* Reads all characters up to the specified one, not including it
	 * Returns that substring or null if specified character doesn't exist
	 */
	public String readUntil(String c) {
		int index = mStr.indexOf(c, mIdx);
		if (index == -1) {
			return null;
		}
		String retval = mStr.substring(mIdx, index);
		mIdx = index;
		return retval;
	}

	public boolean advance() {
		return advance(1);
	}

	public boolean advance(int n) {
		mIdx = mIdx + n;
		if (mIdx > mStr.length()) {
			mIdx = mStr.length();
			return false;
		}
		return true;
	}

	public boolean advance(String next) {
		skipWhitespace();
		String remain = remaining();
		if (remain.startsWith(next)) {
			advance(next.length());
			return true;
		}
		return false;
	}

	public void skipWhitespace() {
		while (mIdx < mStr.length() && mStr.substring(mIdx, mIdx + 1).isBlank()) {
			mIdx += 1;
		}
	}

	public String readOneOf(Collection<String> validItems) {
		skipWhitespace();
		String remain = remaining();
		// Make a copy of the input collection and sort it by length,
		// so it works even when keys are entirely contained within other keys
		// For example "damage" and "damagepercent". Need to match the longer one first if possible
		List<String> sortedCopy = new ArrayList<>(validItems);
		Collections.sort(sortedCopy, (a, b) -> (Integer.compare(b.length(), a.length())));
		for (String item : sortedCopy) {
			if (remain.startsWith(item)) {
				advance(item.length());
				return item;
			}
		}
		return null;
	}

	public Boolean readBoolean() {
		skipWhitespace();
		if (advance("false")) {
			return false;
		} else if (advance("true")) {
			return true;
		}
		return null;
	}

	public Long readLong() {
		skipWhitespace();
		int origIdx = mIdx;
		int tmpIdx = mIdx;
		Long lastParse = null;

		if (peek("-")) {
			tmpIdx++;
		}

		while (tmpIdx < mStr.length()) {
			try {
				lastParse = Long.parseLong(mStr.substring(origIdx, tmpIdx + 1));
				tmpIdx += 1;
			} catch (NumberFormatException nfe) {
				break;
			}
		}
		if (lastParse != null) {
			mIdx = tmpIdx;
		}
		return lastParse;
	}

	public Double readDouble() {
		skipWhitespace();
		int origIdx = mIdx;
		int tmpIdx = mIdx;
		Double lastParse = null;

		if (peek("-")) {
			tmpIdx++;
		}

		while (tmpIdx < mStr.length()) {
			try {
				lastParse = Double.parseDouble(mStr.substring(origIdx, tmpIdx + 1));
				tmpIdx += 1;
			} catch (NumberFormatException nfe) {
				break;
			}
		}
		if (lastParse != null) {
			mIdx = tmpIdx;
		}
		return lastParse;
	}

	public PotionEffectType readPotionEffectType() {
		skipWhitespace();
		String remain = remaining();
		for (PotionEffectType type : PotionEffectType.values()) {
			if (remain.startsWith(type.getName())) {
				advance(type.getName());
				return type;
			}
		}
		return null;
	}

	public Sound readSound() {
		skipWhitespace();
		String remain = remaining();
		for (Sound type : Sound.values()) {
			if (remain.startsWith(type.name())) {
				advance(type.name());
				return type;
			}
		}
		return null;
	}

	public Particle readParticle() {
		skipWhitespace();
		String remain = remaining();
		for (Particle type : Particle.values()) {
			if (remain.startsWith(type.name())) {
				advance(type.name());
				return type;
			}
		}
		return null;
	}

	public Material readMaterial() {
		skipWhitespace();

		Material[] mats = Material.values();
		String remain = remaining();

		// Iterate through Material in reverse order, because hopefully the list is in sorted order already
		// Important so that things that are a subset of each other try longest match first
		for (int i = mats.length - 1; i >= 0; i--) {
			String mat = mats[i].name();
			if (remain.startsWith(mat)) {
				advance(mat.length());
				return mats[i];
			}
		}
		return null;
	}

	public static final Map<String, Color> COLOR_MAP = new LinkedHashMap<>();

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

	public Color readColor() {
		skipWhitespace();

		String remain = remaining();
		if (remain.startsWith("#")) {
			if (remain.length() >= 7) {
				try {
					Color ret = Color.fromRGB(Integer.parseInt(remain.substring(0, 8), 16));
					advance(7);
					return ret;
				} catch (NumberFormatException nfe) {
					return null;
				}
			}
		} else {
			for (Map.Entry<String, Color> color : COLOR_MAP.entrySet()) {
				if (remain.startsWith(color.getKey())) {
					advance(color.getKey());
					return color.getValue();
				}
			}
		}
		return null;
	}

	private static final Pattern QUOTED_STRING_PATTERN = Pattern.compile("^\"(?:[^\"\\\\]|\\\\.)*\"");
	private static final Pattern NON_QUOTED_STRING_PATTERN = Pattern.compile("^[^\",)][^,)]*");

	public String readString() {
		skipWhitespace();
		String remain = remaining();
		Matcher quotedStringMatcher = QUOTED_STRING_PATTERN.matcher(remain);
		if (quotedStringMatcher.find()) {
			String match = quotedStringMatcher.group();
			advance(match);
			// Remove the surrounding quotes and un-escape the contents
			match = StringEscapeUtils.unescapeJava(match.substring(1, match.length() - 1));
			return match;
		}

		Matcher nonQuotedMatcher = NON_QUOTED_STRING_PATTERN.matcher(remain);
		if (nonQuotedMatcher.find()) {
			String match = nonQuotedMatcher.group();
			advance(match);
			return match;
		}

		return null;
	}
}

