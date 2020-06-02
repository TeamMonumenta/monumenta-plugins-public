package com.playmonumenta.plugins.enums;

import net.md_5.bungee.api.ChatColor;

public enum Region {
	NONE(-1, ""),
	MONUMENTA(0, ChatColor.DARK_GRAY + "Monumenta"),
	KINGS_VALLEY(1, ChatColor.DARK_GRAY + "King's Valley"),
	CELSIAN_ISLES(2, ChatColor.DARK_GRAY + "Celsian Isles");

	int mInt;
	String mReadableString;

	Region(int i, String str) {
		this.mInt = i;
		this.mReadableString = str;
	}

	public int getInt() {
		return mInt;
	}

	public String getReadableString() {
		return mReadableString;
	}

	public static String[] valuesLowerCase() {
		Region[] vals = Region.values();
		String[] out = new String[vals.length];
		for (int i = 0; i < vals.length; i++) {
			out[i] = vals[i].toString().toLowerCase();
		}
		return out;
	}
}
