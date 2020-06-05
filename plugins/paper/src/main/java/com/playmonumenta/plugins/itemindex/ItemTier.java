package com.playmonumenta.plugins.itemindex;

import net.md_5.bungee.api.ChatColor;

public enum ItemTier {
	NONE(""),
	UNKNOWN(ChatColor.BLACK + "Unknown"),
	ONE(ChatColor.DARK_GRAY + "Tier I"),
	TWO(ChatColor.DARK_GRAY + "Tier II"),
	THREE(ChatColor.DARK_GRAY + "Tier III"),
	FOUR(ChatColor.DARK_GRAY + "Tier IV"),
	FIVE(ChatColor.DARK_GRAY + "Tier V"),
	MEME(ChatColor.DARK_GRAY + "Meme"),
	UNCOMMON(ChatColor.DARK_GRAY + "Uncommon"),
	ENHANCED_UNCOMMON(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Enhanced Uncommon"),
	PATRON_MADE(ChatColor.GOLD + "Patron Made"),
	RARE(ChatColor.YELLOW + "Rare"),
	ENHANCED_RARE(ChatColor.YELLOW + "" + ChatColor.BOLD + "Enhanced Rare"),
	ARTIFACT(ChatColor.DARK_RED + "Artifact"),
	RELIC(ChatColor.GREEN + "Relic"),
	EPIC(ChatColor.GOLD + "" + ChatColor.BOLD + "Patron Made"),
	UNIQUE(ChatColor.DARK_PURPLE + "Unique"),
	UNIQUE_EVENT(ChatColor.DARK_PURPLE + "Unique Event"),
	DISH(ChatColor.GREEN + "Dish"),
	INJECTOR_POTION(ChatColor.YELLOW + "Injector Potion"),
	KEY(ChatColor.AQUA + "Key"),
	DEV(ChatColor.LIGHT_PURPLE + "Dev Item"),
	SHULKER_BOX("Shulker Box"),
	QUEST_COMPASS("Quest Compass");

	String mReadableString;

	ItemTier(String s) {
		this.mReadableString = s;
	}

	public String getReadableString() {
		return this.mReadableString;
	}

	public static String[] valuesLowerCase() {
		ItemTier[] vals = ItemTier.values();
		String[] out = new String[vals.length];
		for (int i = 0; i < vals.length; i++) {
			out[i] = vals[i].toString().toLowerCase();
		}
		return out;
	}
}
