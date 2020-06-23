package com.playmonumenta.plugins.itemindex;

import org.bukkit.ChatColor;

public enum ItemLocation {

	NONE(""),
	WHITE(ChatColor.WHITE + "Halls of Wind and Blood"),
	ORANGE(ChatColor.GOLD + "Fallen Menagerie"),
	MAGENTA(ChatColor.DARK_PURPLE + "Plagueroot Temple"),
	LIGHT_BLUE(ChatColor.BLUE + "Arcane Rivalry"),
	YELLOW(ChatColor.YELLOW + "Vernal Nightmare"),
	LIME(ChatColor.GREEN + "Salazar's Folly"),
	PINK(ChatColor.LIGHT_PURPLE + "Harmonic Arboretum"),
	GRAY(ChatColor.DARK_GRAY + "Valley of Forgotten Pharaohs"),
	LIGHT_GRAY(ChatColor.GRAY + "Palace of Mirrors"),
	CYAN(ChatColor.DARK_AQUA + "Scourge of Lunacy"),
	PURPLE(ChatColor.DARK_PURPLE + "Grasp of Avarice"),
	LABS(ChatColor.GRAY + "Alchemy Labs"),
	WILLOWS(ChatColor.GRAY + "The Black Willows"),
	REVERIE(ChatColor.DARK_RED + "Malevolent Reverie"),
	ARENA(ChatColor.DARK_RED + "Arena of Terth"),
	SANCTUM(ChatColor.DARK_GREEN + "Forsworn Sanctum"),
	EPHEMERAL(ChatColor.RED + "Ephemeral Corridors"),
	SHIFTING_CITY(ChatColor.BLUE + "City of Shifting Waters"),
	RUSH(ChatColor.RED + "Rush of Dissonance"),
	AZACOR(ChatColor.RED + "Azacor's Malice"),
	KAUL(ChatColor.DARK_GREEN + "Kaul's Judgement"),
	DIVINE(ChatColor.AQUA + "Divine Skin"),
	HORSEMAN(ChatColor.GOLD + "The Headless Horseman"),
	LOWTIDE_SMUGGLER(ChatColor.BLUE + "Lowtide Smuggler"),
	VALLEY_MIMICS(ChatColor.GOLD + "Valley Mimics"),
	KINGS_VALLEY(ChatColor.BLUE + "King's Valley"),
	ROCK_CASINO(ChatColor.GOLD + "Rock's Little Casino"),
	CARNIVAL(ChatColor.RED + "Floating " + ChatColor.WHITE + "Carnival"),
	HALLOWEEN_SKIN(ChatColor.GOLD + "Halloween Skin"),
	TRICKSTER_CHALLENGE(ChatColor.GOLD + "Trickster Challenge"),
	MONARCH_CASINO(ChatColor.AQUA + "Monarch's Cozy Casino"),
	CELSIAN_ISLES(ChatColor.AQUA + "Celsian Isles"),
	EVT_HALLOWEEN_2017(ChatColor.DARK_GREEN + "Halloween 2017"),
	EVT_WINTER_2017(ChatColor.BLUE + "Winter 2017"),
	EVT_VALENTINE_2018(ChatColor.LIGHT_PURPLE + "Valentine's 2018"),
	EVT_EASTER_2018(ChatColor.YELLOW + "Easter 2018"),
	EVT_WINTER_2018(ChatColor.BLUE + "Winter 2018"),
	EVT_HALLOWEEN_2019(ChatColor.DARK_GREEN + "Halloween 2019"),
	EVT_WINTER_2019(ChatColor.BLUE + "Winter 2019"),
	DEVELOPERS_PLAYGROUND(ChatColor.DARK_RED + "" + ChatColor.BOLD + "Developers Playground"),
	;

	String mReadableString;

	ItemLocation(String str) {
		this.mReadableString = str;
	}

	public String getReadableString() {
		return this.mReadableString;
	}

	public static String[] valuesLowerCase() {
		ItemLocation[] vals = ItemLocation.values();
		String[] out = new String[vals.length];
		for (int i = 0; i < vals.length; i++) {
			out[i] = vals[i].toString().toLowerCase();
		}
		return out;
	}
}
