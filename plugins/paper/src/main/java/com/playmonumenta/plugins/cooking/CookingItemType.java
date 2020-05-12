package com.playmonumenta.plugins.cooking;

import org.bukkit.ChatColor;

public enum CookingItemType {
	MEAL(""),
	BASE(ChatColor.GREEN + "Base"),
	TOOL(ChatColor.AQUA + "Tool"),
	SECONDARY(ChatColor.YELLOW + "Secondary");

	private final String mLoreLine;

	CookingItemType(String loreLineID) {
		this.mLoreLine = CookingConsts.NEUTER_COLOR + "Cooking Ingredient : " + loreLineID;
	}

	public String getLoreLine() {
		return this.mLoreLine;
	}
}
