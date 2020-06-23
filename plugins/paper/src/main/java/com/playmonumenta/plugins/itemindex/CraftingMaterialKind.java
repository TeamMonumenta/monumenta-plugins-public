package com.playmonumenta.plugins.itemindex;

import org.bukkit.ChatColor;

import java.util.ArrayList;

public enum CraftingMaterialKind {
	NONE(""),
	NORMAL(ChatColor.GRAY + "Material"),
	EPIC(ChatColor.GRAY + "Epic Material"),
	;

	private String mReadableString;

	CraftingMaterialKind(String s) {
		this.mReadableString = s;
	}

	String getReadableString() {
		return this.mReadableString;
	}

	public static String[] valuesAsStringArray() {
		ArrayList<String> out = new ArrayList<>();
		for (CraftingMaterialKind s : CraftingMaterialKind.values()) {
			out.add(s.toString().toLowerCase());
		}
		return out.toArray(new String[0]);
	}
}
