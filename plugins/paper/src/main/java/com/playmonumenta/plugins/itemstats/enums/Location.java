package com.playmonumenta.plugins.itemstats.enums;

import com.playmonumenta.plugins.classes.*;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkillShopGUI;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

public enum Location {
	NONE("none", ItemStatUtils.DUMMY_LORE_TO_REMOVE),
	OVERWORLD1("overworld1", "King's Valley Overworld", TextColor.fromHexString("#DCAE32")),
	OVERWORLD2("overworld2", "Celsian Isles Overworld", TextColor.fromHexString("#32D7DC")),
	FOREST("forest", "The Wolfswood", TextColor.fromHexString("#4C8F4D")),
	KEEP("keep", "Pelias' Keep", TextColor.fromHexString("#C4BBA5")),
	CASINO1("casino1", "Rock's Little Casino", TextColor.fromHexString("#EDC863")),
	CASINO2("casino2", "Monarch's Cozy Casino", TextColor.fromHexString("#1773B1")),
	CASINO3("casino3", "Sticks and Stones Tavern", TextColor.fromHexString("#C6C2B6")),
	QUEST("quest", "Quest Reward", TextColor.fromHexString("#C8A2C8")),
	LABS("labs", "Alchemy Labs", TextColor.fromHexString("#B4ACC3")),
	WHITE("white", "Halls of Wind and Blood", TextColor.fromHexString("#FFFFFF")),
	ORANGE("orange", "Fallen Menagerie", TextColor.fromHexString("#FFAA00")),
	MAGENTA("magenta", "Plagueroot Temple", TextColor.fromHexString("#FF55FF")),
	LIGHTBLUE("lightblue", "Arcane Rivalry", TextColor.fromHexString("#4AC2E5")),
	YELLOW("yellow", "Vernal Nightmare", TextColor.fromHexString("#FFFF55")),
	LIME("lime", "Salazar's Folly", TextColor.fromHexString("#55FF55")),
	PINK("pink", "Harmonic Arboretum", TextColor.fromHexString("#FF69B4")),
	GRAY("gray", "Valley of Forgotten Pharaohs", TextColor.fromHexString("#555555")),
	LIGHTGRAY("lightgray", "Palace of Mirrors", TextColor.fromHexString("#AAAAAA")),
	CYAN("cyan", "The Scourge of Lunacy", TextColor.fromHexString("#00AAAA")),
	PURPLE("purple", "The Grasp of Avarice", TextColor.fromHexString("#AA00AA")),
	TEAL("teal", "Echoes of Oblivion", TextColor.fromHexString("#47B6B5")),
	WILLOWS("willows", "The Black Willows", TextColor.fromHexString("#006400")),
	WILLOWSKIN("willowskin", "Storied Skin", TextColor.fromHexString("#006400")),
	EPHEMERAL("ephemeral", "Ephemeral Corridors", TextColor.fromHexString("#8B0000")),
	EPHEMERAL_ENHANCEMENTS("ephemeralenhancements", "Ephemeral Enhancements", TextColor.fromHexString("#8B0000")),
	REVERIE("reverie", "Malevolent Reverie", TextColor.fromHexString("#790E47")),
	SANCTUM("sanctum", "Forsworn Sanctum", TextColor.fromHexString("#52AA00")),
	VERDANT("verdant", "Verdant Remnants", TextColor.fromHexString("#158315")),
	VERDANTSKIN("verdantskin", "Threadwarped Skin", TextColor.fromHexString("#704C8A")),
	AZACOR("azacor", "Azacor's Malice", TextColor.fromHexString("#FF6F55")),
	KAUL("kaul", "Kaul's Judgment", TextColor.fromHexString("#00AA00")),
	DIVINE("divine", "Divine Skin", TextColor.fromHexString("#C6EFF1")),
	ROYAL("royal", "Royal Armory", TextColor.fromHexString("#CAFFFD")),
	SHIFTING("shifting", "City of Shifting Waters", TextColor.fromHexString("#7FFFD4")),
	FORUM("forum", "The Fallen Forum", TextColor.fromHexString("#808000")),
	MIST("mist", "The Black Mist", TextColor.fromHexString("#674C5B")),
	HOARD("hoard", "The Hoard", TextColor.fromHexString("#DAAD3E")),
	GREEDSKIN("greedskin", "Greed Skin", TextColor.fromHexString("#DAAD3E")),
	REMORSE("remorse", "Sealed Remorse", TextColor.fromHexString("#EEE6D6")),
	REMORSEFULSKIN("remorsefulskin", "Remorseful Skin", TextColor.fromHexString("#EEE6D6")),
	VIGIL("vigil", "The Eternal Vigil", TextColor.fromHexString("#72999C")),
	DEPTHS("depths", "Darkest Depths", TextColor.fromHexString("#5D2D87")),
	HORSEMAN("horseman", "The Headless Horseman", TextColor.fromHexString("#8E3418")),
	FROSTGIANT("frostgiant", "The Waking Giant", TextColor.fromHexString("#87CEFA")),
	TITANICSKIN("titanicskin", "Titanic Skin", TextColor.fromHexString("#87CEFA")),
	LICH("lich", "Hekawt's Fury", TextColor.fromHexString("#FFB43E")),
	ETERNITYSKIN("eternityskin", "Eternity Skin", TextColor.fromHexString("#FFB43E")),
	RUSH("rush", "Rush of Dissonance", TextColor.fromHexString("#C21E56")),
	TREASURE("treasure", "Treasures of Viridia", TextColor.fromHexString("#C8A2C8")),
	INTELLECT("intellect", "Intellect Crystallizer", TextColor.fromHexString("#82DB17")),
	DELVES("delves", "Dungeon Delves", TextColor.fromHexString("#B47028")),
	MYTHIC("mythic", "Mythic Reliquary", TextColor.fromHexString("#C4971A")),
	CHALLENGER_SKIN("challenger", "Challenger Skin", CosmeticSkillShopGUI.PRESTIGE_COLOR),
	CARNIVAL("carnival", "Floating Carnival", TextColor.fromHexString("#D02E28")),
	LOWTIDE("lowtide", "Lowtide Smuggler", TextColor.fromHexString("#196383")),
	DOCKS("docks", "Expedition Docks", TextColor.fromHexString("#196383")),
	VALENTINE("valentine", "Valentine Event", TextColor.fromHexString("#FF7F7F")),
	VALENTINESKIN("valentineskin", "Valentine Skin", TextColor.fromHexString("#FF7F7F")),
	APRILFOOLS("aprilfools", "April Fools Event", TextColor.fromHexString("#D22AD2")),
	APRILFOOLSSKIN("aprilfoolsskin", "April Fools Skin", TextColor.fromHexString("#D22AD2")),
	EASTER("easter", "Easter Event", TextColor.fromHexString("#55FF55")),
	EASTERSKIN("easterskin", "Easter Skin", TextColor.fromHexString("#55FF55")),
	HALLOWEEN("halloween", "Halloween Event", TextColor.fromHexString("#FFAA00")),
	HALLOWEENSKIN("halloweenskin", "Halloween Skin", TextColor.fromHexString("#FFAA00")),
	TRICKSTER("trickster", "Trickster Challenge", TextColor.fromHexString("#FFAA00")),
	WINTER("winter", "Winter Event", TextColor.fromHexString("#AFC2E3")),
	HOLIDAYSKIN("holidayskin", "Holiday Skin", TextColor.fromHexString("#B00C2F")),
	TRANSMOG("transmogrifier", "Transmogrifier", TextColor.fromHexString("#6F2DA8")),
	UGANDA("uganda", "Uganda 2018", TextColor.fromHexString("#D02E28")),
	SILVER("silver", "Silver Knight's Tomb", TextColor.fromHexString("#C0C0C0")),
	BLUE("blue", "Coven's Gambit", TextColor.fromHexString("#0C2CA2")),
	BROWN("brown", "Cradle of the Broken God", TextColor.fromHexString("#703608")),
	GREEN("green", "Green Dungeon", TextColor.fromHexString("#4D6E23")),
	RED("red", "Red Dungeon", TextColor.fromHexString("#D02E28")),
	BLACK("black", "Black Dungeon", TextColor.fromHexString("#454040")),
	LIGHT("light", "Arena of Terth", TextColor.fromHexString("#FFFFAA")),
	PASS("seasonpass", "Seasonal Pass", TextColor.fromHexString("#FFF63C")),
	SKETCHED("sketched", "Sketched Skin", TextColor.fromHexString("#FFF63C")),
	BLITZ("blitz", "Plunderer's Blitz", TextColor.fromHexString("#DAAD3E")),
	SOULTHREAD("soul", "Soulwoven", TextColor.fromHexString("#7FFFD4")),
	SCIENCE("science", "P.O.R.T.A.L.", TextColor.fromHexString("#DCE8E3")),
	BLUESTRIKE("bluestrike", "Masquerader's Ruin", TextColor.fromHexString("#326DA8")),
	GODSPORE("godspore", "The Godspore's Domain", TextColor.fromHexString("#426B29")),
	GALLERYOFFEAR("gallerybase", "Gallery of Fear", TextColor.fromHexString("#39B14E")),
	SANGUINEHALLS("gallery1", "Sanguine Halls", TextColor.fromHexString("#AB0000")),
	MARINANOIR("gallery2", "Marina Noir", TextColor.fromHexString("#324150")),
	FALLENSTAR("fallenstar", "Shadow of a Fallen Star", TextColor.fromHexString("#00C0A3")),
	PERIWINKLE("periwinkle", "Voidrun Warrens", TextColor.fromHexString("#BE93E4")),
	CHARTREUSE("chartreuse", "Investigator's Gambade", TextColor.fromHexString("#60B476")),
	SOLARIUM("solarium", "Solarium of the Silent", TextColor.fromHexString("#E6CC25")),
	PROMENADE("promenade", "Mecha-Pelias' Mecha-Promenade", TextColor.fromHexString("#B87333")),
	AMBER("amber", "item name color", TextColor.fromHexString("#FFBF00")),
	GOLD("gold", "item name color", TextColor.fromHexString("#FFD700")),
	TRUENORTH("truenorth", "True North", TextColor.fromHexString("#FFD700")),
	DARKBLUE("darkblue", "itemnamecolor", TextColor.fromHexString("#FFFFAA")),
	INDIGO("indigo", "item name color", TextColor.fromHexString("#6F00FF")),
	MIDBLUE("midblue", "itemnamecolor", TextColor.fromHexString("#366EF8")),
	STARPOINT("starpoint", "new expansion :pog:", TextColor.fromHexString("#342768")),
	FISHING("fishing", "Architect's Ring Fishing", TextColor.fromHexString("#A9D1D0")),
	SKR("skr", "Silver Knight's Remnants", TextColor.fromHexString("#E8C392")),
	SIRIUS("sirius", "The Final Blight", TextColor.fromHexString("#34CFBC")),
	ALCHEMIST(new Alchemist()),
	CLERIC(new Cleric()),
	MAGE(new Mage()),
	ROGUE(new Rogue()),
	SCOUT(new Scout()),
	SHAMAN(new Shaman()),
	WARLOCK(new Warlock()),
	WARRIOR(new Warrior()),
	DAWNBRINGER(DepthsTree.DAWNBRINGER),
	EARTHBOUND(DepthsTree.EARTHBOUND),
	FLAMECALLER(DepthsTree.FLAMECALLER),
	FROSTBORN(DepthsTree.FROSTBORN),
	SHADOWDANCER(DepthsTree.SHADOWDANCER),
	STEELSAGE(DepthsTree.STEELSAGE),
	WINDWALKER(DepthsTree.WINDWALKER),
	;

	public static final String KEY = "Location";

	final String mName;
	final String mDisplayName;
	final Component mDisplay;
	final TextColor mColor;

	Location(String name, String display, TextColor color) {
		mName = name;
		mDisplayName = display;
		mDisplay = Component.text(display, color).decoration(TextDecoration.ITALIC, false);
		mColor = color;
	}

	Location(String name, Component display) {
		mName = name;
		mDisplayName = MessagingUtils.plainText(display);
		mDisplay = display;
		mColor = display.color();
	}

	Location(PlayerClass cls) {
		this(cls.mClassName.toLowerCase(), cls.mClassName, cls.mClassColor);
	}

	Location(DepthsTree tree) {
		this(tree.getDisplayName().toLowerCase(), tree.getDisplayName(), tree.getColor());
	}

	public String getName() {
		return mName;
	}

	public String getDisplayName() {
		return mDisplayName;
	}

	public Component getDisplay() {
		return mDisplay;
	}

	public TextColor getColor() {
		return mColor;
	}

	public static Location getLocation(String name) {
		for (Location location : Location.values()) {
			if (location.getName().replace(" ", "").equals(name.replace(" ", ""))) {
				return location;
			}
		}

		return Location.NONE;
	}
}
