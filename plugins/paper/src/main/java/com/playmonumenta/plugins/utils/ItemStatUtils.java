package com.playmonumenta.plugins.utils;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.ItemStat;
import com.playmonumenta.plugins.itemstats.attributes.Agility;
import com.playmonumenta.plugins.itemstats.attributes.Armor;
import com.playmonumenta.plugins.itemstats.attributes.AttackDamageAdd;
import com.playmonumenta.plugins.itemstats.attributes.AttackDamageMultiply;
import com.playmonumenta.plugins.itemstats.attributes.MagicDamageAdd;
import com.playmonumenta.plugins.itemstats.attributes.MagicDamageMultiply;
import com.playmonumenta.plugins.itemstats.attributes.ProjectileDamageAdd;
import com.playmonumenta.plugins.itemstats.attributes.ProjectileDamageMultiply;
import com.playmonumenta.plugins.itemstats.attributes.ProjectileSpeed;
import com.playmonumenta.plugins.itemstats.attributes.SpellPower;
import com.playmonumenta.plugins.itemstats.attributes.ThornsDamage;
import com.playmonumenta.plugins.itemstats.attributes.ThrowRate;
import com.playmonumenta.plugins.itemstats.enchantments.*;
import com.playmonumenta.plugins.itemstats.infusions.*;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTList;
import de.tr7zw.nbtapi.NBTListCompound;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

public class ItemStatUtils {

	static final String MONUMENTA_DUMMY_TOUGHNESS_ATTRIBUTE_NAME = "MMDummy";
	static final String MONUMENTA_KEY = "Monumenta";
	static final String LORE_KEY = "Lore";
	static final String STOCK_KEY = "Stock";
	static final String PLAYER_MODIFIED_KEY = "PlayerModified";
	static final String LEVEL_KEY = "Level";
	static final String INFUSER_KEY = "Infuser";
	static final String ATTRIBUTE_NAME_KEY = "AttributeName";
	static final String AMOUNT_KEY = "Amount";
	static final String SHATTERED_KEY = "Shattered";

	static final Component DUMMY_LORE_TO_REMOVE = Component.text("DUMMY LORE TO REMOVE", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);

	public static String getMonumentaKey() {
		return MONUMENTA_KEY;
	}

	public enum Region {
		NONE("none", DUMMY_LORE_TO_REMOVE),
		VALLEY("valley", Component.text("King's Valley : ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)),
		ISLES("isles", Component.text("Celsian Isles : ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)),
		RING("ring", Component.text("Architect's Ring : ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)),
		SHULKER_BOX("shulker", Component.text("INVALID ENTRY", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));

		public static final String KEY = "Region";

		final String mName;
		final Component mDisplay;
		final String mPlainDisplay;

		Region(String name, Component display) {
			mName = name;
			mDisplay = display;
			mPlainDisplay = MessagingUtils.plainText(display);
		}

		public String getName() {
			return mName;
		}

		public Component getDisplay() {
			return mDisplay;
		}

		public String getPlainDisplay() {
			return mPlainDisplay;
		}

		public static Region getRegion(String name) {
			for (Region region : Region.values()) {
				if (region.getName().equals(name)) {
					return region;
				}
			}

			return NONE;
		}
	}

	public enum Tier {
		NONE("none", DUMMY_LORE_TO_REMOVE),
		ZERO("0", Component.text("Tier 0", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)),
		I("1", Component.text("Tier I", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)),
		II("2", Component.text("Tier II", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)),
		III("3", Component.text("Tier III", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)),
		IV("4", Component.text("Tier IV", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)),
		V("5", Component.text("Tier V", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)),
		UNCOMMON("uncommon", Component.text("Uncommon", TextColor.fromHexString("#C0C0C0")).decoration(TextDecoration.ITALIC, false)),
		RARE("rare", Component.text("Rare", TextColor.fromHexString("#4AC2E5")).decoration(TextDecoration.ITALIC, false)),
		ARTIFACT("artifact", Component.text("Artifact", TextColor.fromHexString("#D02E28")).decoration(TextDecoration.ITALIC, false)),
		EPIC("epic", Component.text("Epic", TextColor.fromHexString("#FFD700")).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true)),
		UNIQUE("unique", Component.text("Unique", TextColor.fromHexString("#C8A2C8")).decoration(TextDecoration.ITALIC, false)),
		PATRON("patron", Component.text("Patron Made", TextColor.fromHexString("#82DB17")).decoration(TextDecoration.ITALIC, false)),
		EVENT("event", Component.text("Event", TextColor.fromHexString("#7FFFD4")).decoration(TextDecoration.ITALIC, false)),
		LEGACY("legacy", Component.text("Legacy", TextColor.fromHexString("#EEE6D6")).decoration(TextDecoration.ITALIC, false)),
		CURRENCY("currency", Component.text("Currency", TextColor.fromHexString("#DCAE32")).decoration(TextDecoration.ITALIC, false)),
		KEYTIER("key", Component.text("Key", TextColor.fromHexString("#47B6B5")).decoration(TextDecoration.ITALIC, false)),
		TROPHY("trophy", Component.text("Trophy", TextColor.fromHexString("#CAFFFD")).decoration(TextDecoration.ITALIC, false)),
		OBFUSCATED("obfuscated", Component.text("Stick_:)", TextColor.fromHexString("#5D2D87")).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.OBFUSCATED, true)),
		SHULKER_BOX("shulker", Component.text("Invalid Type", TextColor.fromHexString("#EEE6D6")).decoration(TextDecoration.ITALIC, false)),
		QUEST_COMPASS("quest_compass", Component.text("Invalid Type", TextColor.fromHexString("#EEE6D6")).decoration(TextDecoration.ITALIC, false));

		static final String KEY = "Tier";

		final String mName;
		final Component mDisplay;
		final String mPlainDisplay;

		Tier(String name, Component display) {
			mName = name;
			mDisplay = display;
			mPlainDisplay = MessagingUtils.plainText(display);
		}

		public String getName() {
			return mName;
		}

		public Component getDisplay() {
			return mDisplay;
		}

		public String getPlainDisplay() {
			return mPlainDisplay;
		}

		public static Tier getTier(String name) {
			for (Tier tier : Tier.values()) {
				if (tier.getName().equals(name)) {
					return tier;
				}
			}

			return Tier.NONE;
		}
	}

	public enum Location {
		NONE("none", DUMMY_LORE_TO_REMOVE),
		OVERWORLD1("overworld1", Component.text("King's Valley Overworld", TextColor.fromHexString("#DCAE32")).decoration(TextDecoration.ITALIC, false)),
		OVERWORLD2("overworld2", Component.text("Celsian Isles Overworld", TextColor.fromHexString("#32D7DC")).decoration(TextDecoration.ITALIC, false)),
		CASINO1("casino1", Component.text("Rock's Little Casino", TextColor.fromHexString("#EDC863")).decoration(TextDecoration.ITALIC, false)),
		CASINO2("casino2", Component.text("Monarch's Cozy Casino", TextColor.fromHexString("#1773B1")).decoration(TextDecoration.ITALIC, false)),
		LABS("labs", Component.text("Alchemy Labs", TextColor.fromHexString("#B4ACC3")).decoration(TextDecoration.ITALIC, false)),
		WHITE("white", Component.text("Halls of Wind and Blood", TextColor.fromHexString("#FFFFFF")).decoration(TextDecoration.ITALIC, false)),
		ORANGE("orange", Component.text("Fallen Menagerie", TextColor.fromHexString("#FFAA00")).decoration(TextDecoration.ITALIC, false)),
		MAGENTA("magenta", Component.text("Plagueroot Temple", TextColor.fromHexString("#FF55FF")).decoration(TextDecoration.ITALIC, false)),
		LIGHTBLUE("lightblue", Component.text("Arcane Rivalry", TextColor.fromHexString("#4AC2E5")).decoration(TextDecoration.ITALIC, false)),
		YELLOW("yellow", Component.text("Vernal Nightmare", TextColor.fromHexString("#FFFF55")).decoration(TextDecoration.ITALIC, false)),
		LIME("lime", Component.text("Salazar's Folly", TextColor.fromHexString("#55FF55")).decoration(TextDecoration.ITALIC, false)),
		PINK("pink", Component.text("Harmonic Arboretum", TextColor.fromHexString("#FF69B4")).decoration(TextDecoration.ITALIC, false)),
		GRAY("gray", Component.text("Valley of Forgotten Pharaohs", TextColor.fromHexString("#555555")).decoration(TextDecoration.ITALIC, false)),
		LIGHTGRAY("lightgray", Component.text("Palace of Mirrors", TextColor.fromHexString("#AAAAAA")).decoration(TextDecoration.ITALIC, false)),
		CYAN("cyan", Component.text("The Scourge of Lunacy", TextColor.fromHexString("#00AAAA")).decoration(TextDecoration.ITALIC, false)),
		PURPLE("purple", Component.text("The Grasp of Avarice", TextColor.fromHexString("#AA00AA")).decoration(TextDecoration.ITALIC, false)),
		TEAL("teal", Component.text("Echoes of Oblivion", TextColor.fromHexString("#47B6B5")).decoration(TextDecoration.ITALIC, false)),
		WILLOWS("willows", Component.text("The Black Willows", TextColor.fromHexString("#006400")).decoration(TextDecoration.ITALIC, false)),
		WILLOWSKIN("willowskin", Component.text("Storied Skin", TextColor.fromHexString("#006400")).decoration(TextDecoration.ITALIC, false)),
		EPHEMERAL("ephemeral", Component.text("Ephemeral Corridors", TextColor.fromHexString("#8B0000")).decoration(TextDecoration.ITALIC, false)),
		SANCTUM("sanctum", Component.text("Forsworn Sanctum", TextColor.fromHexString("#52AA00")).decoration(TextDecoration.ITALIC, false)),
		VERDANT("verdant", Component.text("Verdant Remnants", TextColor.fromHexString("#158315")).decoration(TextDecoration.ITALIC, false)),
		VERDANTSKIN("verdantskin", Component.text("Threadwarped Skin", TextColor.fromHexString("#704C8A")).decoration(TextDecoration.ITALIC, false)),
		REVERIE("reverie", Component.text("Malevolent Reverie", TextColor.fromHexString("#790E47")).decoration(TextDecoration.ITALIC, false)),
		AZACOR("azacor", Component.text("Azacor's Malice", TextColor.fromHexString("#FF6F55")).decoration(TextDecoration.ITALIC, false)),
		KAUL("kaul", Component.text("Kaul's Judgment", TextColor.fromHexString("#00AA00")).decoration(TextDecoration.ITALIC, false)),
		DIVINE("divine", Component.text("Divine Skin", TextColor.fromHexString("#C6EFF1")).decoration(TextDecoration.ITALIC, false)),
		ROYAL("royal", Component.text("Royal Armory", TextColor.fromHexString("#CAFFFD")).decoration(TextDecoration.ITALIC, false)),
		SHIFTING("shifting", Component.text("City of Shifting Waters", TextColor.fromHexString("#7FFFD4")).decoration(TextDecoration.ITALIC, false)),
		FORUM("forum", Component.text("The Fallen Forum", TextColor.fromHexString("#808000")).decoration(TextDecoration.ITALIC, false)),
		MIST("mist", Component.text("The Black Mist", TextColor.fromHexString("#674C5B")).decoration(TextDecoration.ITALIC, false)),
		HOARD("hoard", Component.text("The Hoard", TextColor.fromHexString("#DAAD3E")).decoration(TextDecoration.ITALIC, false)),
		GREEDSKIN("greedskin", Component.text("Greed Skin", TextColor.fromHexString("#DAAD3E")).decoration(TextDecoration.ITALIC, false)),
		REMORSE("remorse", Component.text("Sealed Remorse", TextColor.fromHexString("#EEE6D6")).decoration(TextDecoration.ITALIC, false)),
		REMORSEFULSKIN("remorsefulskin", Component.text("Remorseful Skin", TextColor.fromHexString("#EEE6D6")).decoration(TextDecoration.ITALIC, false)),
		VIGIL("vigil", Component.text("The Eternal Vigil", TextColor.fromHexString("#72999C")).decoration(TextDecoration.ITALIC, false)),
		DEPTHS("depths", Component.text("Darkest Depths", TextColor.fromHexString("#5D2D87")).decoration(TextDecoration.ITALIC, false)),
		HORSEMAN("horseman", Component.text("The Headless Horseman", TextColor.fromHexString("#8E3418")).decoration(TextDecoration.ITALIC, false)),
		FROSTGIANT("frostgiant", Component.text("The Waking Giant", TextColor.fromHexString("#87CEFA")).decoration(TextDecoration.ITALIC, false)),
		TITANICSKIN("titanicskin", Component.text("Titanic Skin", TextColor.fromHexString("#87CEFA")).decoration(TextDecoration.ITALIC, false)),
		LICH("lich", Component.text("Hekawt's Fury", TextColor.fromHexString("#FFB43E")).decoration(TextDecoration.ITALIC, false)),
		ETERNITYSKIN("eternityskin", Component.text("Eternity Skin", TextColor.fromHexString("#FFB43E")).decoration(TextDecoration.ITALIC, false)),
		RUSH("rush", Component.text("Rush of Dissonance", TextColor.fromHexString("#C21E56")).decoration(TextDecoration.ITALIC, false)),
		TREASURE("treasure", Component.text("Treasures of Viridia", TextColor.fromHexString("#C8A2C8")).decoration(TextDecoration.ITALIC, false)),
		INTELLECT("intellect", Component.text("Intellect Crystallizer", TextColor.fromHexString("#82DB17")).decoration(TextDecoration.ITALIC, false)),
		DELVES("delves", Component.text("Dungeon Delves", TextColor.fromHexString("#B47028")).decoration(TextDecoration.ITALIC, false)),
		CARNIVAL("carnival", Component.text("Floating Carnival", TextColor.fromHexString("#D02E28")).decoration(TextDecoration.ITALIC, false)),
		LOWTIDE("lowtide", Component.text("Lowtide Smuggler", TextColor.fromHexString("#196383")).decoration(TextDecoration.ITALIC, false)),
		DOCKS("docks", Component.text("Expedition Docks", TextColor.fromHexString("#196383")).decoration(TextDecoration.ITALIC, false)),
		MYTHIC("mythic", Component.text("Mythic Reliquary", TextColor.fromHexString("#C4971A")).decoration(TextDecoration.ITALIC, false)),
		VALENTINE("valentine", Component.text("Valentine Event", TextColor.fromHexString("#FF7F7F")).decoration(TextDecoration.ITALIC, false)),
		VALENTINESKIN("valentineskin", Component.text("Valentine Skin", TextColor.fromHexString("#FF7F7F")).decoration(TextDecoration.ITALIC, false)),
		APRILFOOLS("aprilfools", Component.text("April Fools Event", TextColor.fromHexString("#D22AD2")).decoration(TextDecoration.ITALIC, false)),
		APRILFOOLSSKIN("aprilfoolsskin", Component.text("April Fools Skin", TextColor.fromHexString("#D22AD2")).decoration(TextDecoration.ITALIC, false)),
		EASTER("easter", Component.text("Easter Event", TextColor.fromHexString("#55FF55")).decoration(TextDecoration.ITALIC, false)),
		EASTERSKIN("easterskin", Component.text("Easter Skin", TextColor.fromHexString("#55FF55")).decoration(TextDecoration.ITALIC, false)),
		HALLOWEEN("halloween", Component.text("Halloween Event", TextColor.fromHexString("#FFAA00")).decoration(TextDecoration.ITALIC, false)),
		HALLOWEENSKIN("halloweenskin", Component.text("Halloween Skin", TextColor.fromHexString("#FFAA00")).decoration(TextDecoration.ITALIC, false)),
		TRICKSTER("trickster", Component.text("Trickster Challenge", TextColor.fromHexString("#FFAA00")).decoration(TextDecoration.ITALIC, false)),
		WINTER("winter", Component.text("Winter Event", TextColor.fromHexString("#AFC2E3")).decoration(TextDecoration.ITALIC, false)),
		HOLIDAYSKIN("holidayskin", Component.text("Holiday Skin", TextColor.fromHexString("#B00C2F")).decoration(TextDecoration.ITALIC, false)),
		TRANSMOG("transmogrifier", Component.text("Transmogrifier", TextColor.fromHexString("#6F2DA8")).decoration(TextDecoration.ITALIC, false)),
		UGANDA("uganda", Component.text("Uganda 2018", TextColor.fromHexString("#D02E28")).decoration(TextDecoration.ITALIC, false)),
		BLUE("blue", Component.text("You're only here to try and sneak leaks in the code", TextColor.fromHexString("#0C2CA2")).decoration(TextDecoration.ITALIC, false)),
		BROWN("brown", Component.text("Hellborn Skylands", TextColor.fromHexString("#65350F")).decoration(TextDecoration.ITALIC, false)),
		GREEN("green", Component.text("Skyborn Helllands", TextColor.fromHexString("#4D6E23")).decoration(TextDecoration.ITALIC, false)),
		RED("red", Component.text("Region 3 duh", TextColor.fromHexString("#D02E28")).decoration(TextDecoration.ITALIC, false)),
		BLACK("black", Component.text("Calder's House", TextColor.fromHexString("#000000")).decoration(TextDecoration.ITALIC, false)),
		LIGHT("light", Component.text("Arena of Terth", TextColor.fromHexString("#FFFFAA")).decoration(TextDecoration.ITALIC, false)),
		PASS("seasonpass", Component.text("Seasonal Pass", TextColor.fromHexString("#FFF63C")).decoration(TextDecoration.ITALIC, false)),
		BLITZ("blitz", Component.text("Plunderer's Blitz", TextColor.fromHexString("#DAAD3E")).decoration(TextDecoration.ITALIC, false)),
		SOULTHREAD("soul", Component.text("Soulwoven", TextColor.fromHexString("#7FFFD4")).decoration(TextDecoration.ITALIC, false)),
		AMBER("amber", Component.text("item name color", TextColor.fromHexString("#FFBF00")).decoration(TextDecoration.ITALIC, false)),
		GOLD("gold", Component.text("item name color", TextColor.fromHexString("#FFD700")).decoration(TextDecoration.ITALIC, false)),
		SILVER("silver", Component.text("item name color", TextColor.fromHexString("#C0C0C0")).decoration(TextDecoration.ITALIC, false)),
		DARKBLUE("darkblue", Component.text("itemnamecolor", TextColor.fromHexString("#FFFFAA")).decoration(TextDecoration.ITALIC, false)),
		INDIGO("indigo", Component.text("item name color", TextColor.fromHexString("#6F00FF")).decoration(TextDecoration.ITALIC, false)),
		MIDBLUE("midblue", Component.text("itemnamecolor", TextColor.fromHexString("#366EF8")).decoration(TextDecoration.ITALIC, false));

		static final String KEY = "Location";

		final String mName;
		final Component mDisplay;

		Location(String name, Component display) {
			mName = name;
			mDisplay = display;
		}

		public String getName() {
			return mName;
		}

		public Component getDisplay() {
			return mDisplay;
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

	/*
	 * TODO: converting these enums (EnchantmentType, InfusionType, AttributeType, AbilityAttributeType)
	 * into a proper class hierarchy with static final instances would cut down on code and improve logic
	 */

	public enum EnchantmentType {
		// Vanilla
		SWEEPING_EDGE(Enchantment.SWEEPING_EDGE, "Sweeping Edge", true, false, false),
		KNOCKBACK(Enchantment.KNOCKBACK, "Knockback", true, false, false),
		LOOTING(Enchantment.LOOT_BONUS_MOBS, "Looting", true, false, false),
		RIPTIDE(Enchantment.RIPTIDE, "Riptide", true, false, false),
		PUNCH(Enchantment.ARROW_KNOCKBACK, "Punch", true, false, false),
		QUICK_CHARGE(Enchantment.QUICK_CHARGE, "Quick Charge", true, false, false),
		PIERCING(Enchantment.PIERCING, "Piercing", true, false, false),
		MULTISHOT(Enchantment.MULTISHOT, "Multishot", false, false, false),
		INFINITY(Enchantment.ARROW_INFINITE, "Infinity", false, false, false),
		EFFICIENCY(Enchantment.DIG_SPEED, "Efficiency", true, false, false),
		FORTUNE(Enchantment.LOOT_BONUS_BLOCKS, "Fortune", true, false, false),
		SILK_TOUCH(Enchantment.SILK_TOUCH, "Silk Touch", false, false, false),
		LUCK_OF_THE_SEA(Enchantment.LUCK, "Luck of the Sea", true, false, false),
		LURE(Enchantment.LURE, "Lure", true, false, false),
		SOUL_SPEED(Enchantment.SOUL_SPEED, "Soul Speed", true, false, false),
		AQUA_AFFINITY(Enchantment.WATER_WORKER, "Aqua Affinity", false, false, false),
		RESPIRATION(Enchantment.OXYGEN, "Respiration", true, false, false),
		DEPTH_STRIDER(Enchantment.DEPTH_STRIDER, "Depth Strider", true, false, false),
		// Protections
		MELEE_PROTECTION(new MeleeProtection(), true, false, false),
		PROJECTILE_PROTECTION(new ProjectileProtection(), true, false, false),
		BLAST_PROTECTION(new BlastProtection(), true, false, false),
		MAGIC_PROTECTION(new MagicProtection(), true, false, false),
		FIRE_PROTECTION(new FireProtection(), true, false, false),
		FEATHER_FALLING(new FeatherFalling(), true, false, false),
		// Defense Scaling
		SHIELDING(new Shielding(), true, false, false),
		INURE(new Inure(), true, false, false),
		POISE(new Poise(), true, false, false),
		STEADFAST(new Steadfast(), true, false, false),
		TEMPO(new Tempo(), true, false, false),
		REFLEXES(new Reflexes(), true, false, false),
		ETHEREAL(new Ethereal(), true, false, false),
		EVASION(new Evasion(), true, false, false),
		ADAPTABILITY(new Adaptability(), false, false, false),
		// Custom Enchants
		ABYSSAL(new Abyssal(), true, false, false),
		ADRENALINE(new Adrenaline(), true, false, false),
		APTITUDE(new Aptitude(), true, false, false),
		ARCANE_THRUST(new ArcaneThrust(), true, false, false),
		ASHES_OF_ETERNITY(new AshesOfEternity(), false, false, false),
		BLEEDING(new Bleeding(), true, false, false),
		CHAOTIC(new Chaotic(), true, false, false),
		DARKSIGHT(new Darksight(), false, false, false),
		DECAY(new Decay(), true, false, false),
		DUELIST(new Duelist(), true, false, false),
		ERUPTION(new Eruption(), true, false, false),
		ICE_ASPECT(new IceAspect(), true, false, false),
		FIRE_ASPECT(new FireAspect(), true, false, false),
		THUNDER_ASPECT(new ThunderAspect(), true, false, false),
		FLAME(new Flame(), false, false, false),
		FROST(new Frost(), false, false, false),
		GILLS(new Gills(), false, false, false),
		HEX_EATER(new HexEater(), true, false, false),
		INFERNO(new Inferno(), true, false, false),
		INSTANT_DRINK(new InstantDrink(), false, false, false),
		INTUITION(new Intuition(), false, false, false),
		JUNGLES_NOURISHMENT(new JunglesNourishment(), false, false, false),
		LIFE_DRAIN(new LifeDrain(), true, false, false),
		MULTITOOL(new Multitool(), true, false, false),
		RADIANT(new Radiant(), false, false, false),
		REGENERATION(new Regeneration(), true, false, false),
		POINT_BLANK(new PointBlank(), true, false, false),
		PROTECTION_OF_THE_DEPTHS(new ProtectionOfTheDepths(), false, false, false),
		QUAKE(new Quake(), true, false, false),
		RAGE_OF_THE_KETER(new RageOfTheKeter(), false, false, false),
		RECOIL(new Recoil(), true, false, false),
		REGICIDE(new Regicide(), true, false, false),
		RESURRECTION(new Resurrection(), false, false, false),
		RETRIEVAL(new Retrieval(), true, false, false),
		SAPPER(new Sapper(), true, false, false),
		SECOND_WIND(new SecondWind(), true, false, false),
		SLAYER(new Slayer(), true, false, false),
		SMITE(new Smite(), true, false, false),
		SNIPER(new Sniper(), true, false, false),
		SPARK(new Spark(), false, false, false),
		STARVATION(new Starvation(), false, true, false),
		SUSTENANCE(new Sustenance(), true, false, false),
		WEIGHTLESS(new Weightless(), false, false, false),
		THROWING_KNIFE(new ThrowingKnife(), false, false, false),
		TRIAGE(new Triage(), true, false, false),
		VOID_TETHER(new VoidTether(), false, false, false),
		// Curses
		CURSE_OF_ANEMIA(new CurseOfAnemia(), true, true, false),
		CURSE_OF_BINDING(Enchantment.BINDING_CURSE, "Curse of Binding", false, true, false),
		CURSE_OF_VANISHING(Enchantment.VANISHING_CURSE, "Curse of Vanishing", false, true, false),
		CURSE_OF_IRREPARIBILITY(new CurseOfIrreparability(), false, true, false),
		CURSE_OF_CRIPPLING(new CurseOfCrippling(), false, true, false),
		CURSE_OF_CORRUPTION(new CurseOfCorruption(), false, true, false),
		CURSE_OF_EPHEMERALITY(new CurseOfEphemerality(), false, true, true),
		CURSE_OF_SHRAPNEL(new CurseOfShrapnel(), true, true, false),
		TWO_HANDED(new TwoHanded(), false, true, false),
		INEPTITUDE(new Ineptitude(), true, true, false),
		// Durability
		UNBREAKING(Enchantment.DURABILITY, "Unbreaking", true, false, false),
		UNBREAKABLE(null, "Unbreakable", false, false, false),
		MENDING(Enchantment.MENDING, "Mending", false, false, false),
		// Cosmetic Item Enchants
		BAAING(new Baaing(), false, false, true),
		CLUCKING(new Clucking(), false, false, true),
		DIVINE_AURA(new DivineAura(), false, false, false),
		OINKING(new Oinking(), false, false, true),
		MATERIAL(new MaterialEnch(), false, false, false),
		// Region Scaling
		OFFHAND_MAINHAND_DISABLE(new OffhandMainhandDisable(), false, false, false),
		MAINHAND_OFFHAND_DISABLE(new MainhandOffhandDisable(), false, false, false),
		REGION_SCALING_DAMAGE_DEALT(new RegionScalingDamageDealt(), false, false, false),
		REGION_SCALING_DAMAGE_TAKEN(new RegionScalingDamageTaken(), false, false, false),
		// Item Tags
		MAGIC_WAND(null, "Magic Wand", false, false, false),
		ALCHEMICAL_ALEMBIC(null, "Alchemical Utensil", false, false, false),
		//Random Stuff
		PESTILENCE_TESSERACT(new PestilenceTesseract(), false, false, true),
		// Crit Calcs (defaults to value of 1, always active. DO NOT GIVE TO PLAYERS VIA ENCHANT)
		HIDE_ATTRIBUTES(new HideAttributes(), false, false, false),
		HIDE_ENCHANTS(new HideEnchants(), false, false, false),
		HIDE_INFO(new HideInfo(), false, false, false),
		ANTI_CRIT_SCALING(new AntiCritScaling(), false, false, false),
		CRIT_SCALING(new CritScaling(), false, false, false),
		STRENGTH_APPLY(new StrengthApply(), false, false, false),
		STRENGTH_CANCEL(new StrengthCancel(), false, false, false);

		public static final Map<String, EnchantmentType> REVERSE_MAPPINGS = Arrays.stream(EnchantmentType.values())
			.collect(Collectors.toUnmodifiableMap(type -> type.getName().replace(" ", ""), type -> type));

		public static final Set<EnchantmentType> SPAWNABLE_ENCHANTMENTS = Arrays.stream(EnchantmentType.values())
			.filter(type -> type.mIsSpawnable)
			.collect(Collectors.toUnmodifiableSet());

		static final String KEY = "Enchantments";

		final @Nullable Enchantment mEnchantment;
		final @Nullable ItemStat mItemStat;
		final String mName;
		final boolean mUsesLevels;
		final boolean mIsCurse;
		final boolean mIsSpawnable;

		EnchantmentType(@Nullable Enchantment enchantment, String name, boolean usesLevels, boolean isCurse, boolean isSpawnable) {
			mEnchantment = enchantment;
			mItemStat = null;
			mName = name;
			mUsesLevels = usesLevels;
			mIsCurse = isCurse;
			mIsSpawnable = isSpawnable;
		}

		EnchantmentType(ItemStat itemStat, boolean usesLevels, boolean isCurse, boolean isSpawnable) {
			mEnchantment = null;
			mItemStat = itemStat;
			mName = itemStat.getName();
			mUsesLevels = usesLevels;
			mIsCurse = isCurse;
			mIsSpawnable = isSpawnable;
		}

		public @Nullable Enchantment getEnchantment() {
			return mEnchantment;
		}

		public @Nullable ItemStat getItemStat() {
			return mItemStat;
		}

		public String getName() {
			return mName;
		}

		public boolean isItemTypeEnchantment() {
			return this == MAGIC_WAND
				       || this == ALCHEMICAL_ALEMBIC;
		}

		public boolean isHidden() {
			return this == MAINHAND_OFFHAND_DISABLE
				       || this == OFFHAND_MAINHAND_DISABLE
				       || this == HIDE_ATTRIBUTES
				       || this == HIDE_ENCHANTS
				       || this == HIDE_INFO;
		}

		public Component getDisplay(int level) {
			if (this == JUNGLES_NOURISHMENT) {
				return Component.text("Jungle's Nourishment", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
			} else if (isItemTypeEnchantment()) {
				return Component.text("* " + mName + " *", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);
			} else if (!mUsesLevels && level == 1) {
				return Component.text(mName, mIsCurse ? NamedTextColor.RED : NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
			} else {
				return Component.text(mName + " " + toRomanNumerals(level), mIsCurse ? NamedTextColor.RED : NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
			}
		}

		public static @Nullable EnchantmentType getEnchantmentType(String name) {
			return REVERSE_MAPPINGS.get(name.replace(" ", ""));
		}
	}

	public enum InfusionType {
		// Infusions
		ACUMEN(new Acumen(), "", true, false, false),
		ARDOR(new Ardor(), "", true, false, false),
		AURA(new Aura(), "", true, false, false),
		CARAPACE(new Carapace(), "", true, false, false),
		CHOLER(new Choler(), "", true, false, false),
		EMPOWERED(new Empowered(), "", true, false, false),
		EPOCH(new Epoch(), "", true, false, false),
		EXECUTION(new Execution(), "", true, false, false),
		EXPEDITE(new Expedite(), "", true, false, false),
		FOCUS(new Focus(), "", true, false, false),
		MITOSIS(new Mitosis(), "", true, false, false),
		NATANT(new Natant(), "", true, false, false),
		NUTRIMENT(new Nutriment(), "", true, false, false),
		PENNATE(new Pennate(), "", true, false, false),
		PERSPICACITY(new Perspicacity(), "", true, false, false),
		REFLECTION(new Reflection(), "", true, false, false),
		TENACITY(new Tenacity(), "", true, false, false),
		UNDERSTANDING(new Understanding(), "", true, false, false),
		USURPER(new Usurper(), "", true, false, false),
		VIGOR(new Vigor(), "", true, false, false),
		VITALITY(new Vitality(), "", true, false, false),
		// Other Added Tags
		LOCKED(new Locked(), "", false, false, false),
		BARKING(new Barking(), "", true, true, false),
		DEBARKING(new Debarking(), "", false, false, false),
		HOPE(new Hope(), "Hoped", false, true, false),
		COLOSSAL(new Colossal(), "Reinforced", false, false, false),
		PHYLACTERY(new Phylactery(), "Embalmed", false, false, false),
		SOULBOUND(new Soulbound(), "Soulbound", false, false, false),
		FESTIVE(new Festive(), "Decorated", false, true, false),
		GILDED(new Gilded(), "Gilded", false, true, false),
		// Stat tracking stuff
		STAT_TRACK(new StatTrack(), "Tracked", false, false, false),
		STAT_TRACK_KILLS(new StatTrackKills(), "", true, false, true),
		STAT_TRACK_MELEE(new StatTrackMelee(), "", true, false, true),
		STAT_TRACK_BOSS(new StatTrackBoss(), "", true, false, true),
		STAT_TRACK_SPAWNER(new StatTrackSpawners(), "", true, false, true),
		STAT_TRACK_CONSUMED(new StatTrackConsumed(), "", true, false, true),
		STAT_TRACK_BLOCKS(new StatTrackBlocks(), "", true, false, true);

		public static final Map<String, InfusionType> REVERSE_MAPPINGS = Arrays.stream(InfusionType.values())
			.collect(Collectors.toUnmodifiableMap(type -> type.getName().replace(" ", ""), type -> type));

		public static final Set<InfusionType> STAT_TRACK_OPTIONS = Arrays.stream(InfusionType.values())
			.filter(type -> type.mIsStatTrackOption)
			.collect(Collectors.toUnmodifiableSet());

		public static final Set<InfusionType> SPAWNABLE_INFUSIONS = Arrays.stream(InfusionType.values())
			.filter(type -> type.mIsSpawnable)
			.collect(Collectors.toUnmodifiableSet());

		static final String KEY = "Infusions";

		final ItemStat mItemStat;
		final String mName;
		final String mMessage;
		final boolean mIsSpawnable;
		final boolean mHasLevels;
		final boolean mIsStatTrackOption;

		InfusionType(ItemStat itemStat, String message, boolean hasLevels, boolean isSpawnable, boolean isStatTrackOption) {
			mItemStat = itemStat;
			mName = itemStat.getName();
			mIsSpawnable = isSpawnable;
			mHasLevels = hasLevels;
			mMessage = message;
			mIsStatTrackOption = isStatTrackOption;
		}

		public ItemStat getItemStat() {
			return mItemStat;
		}

		public String getName() {
			return mName;
		}

		public String getMessage() {
			return mMessage;
		}

		public Component getDisplay(int level, String infuser) {
			if (!mHasLevels) {
				if (mMessage.isEmpty()) {
					return Component.text(mName, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
				} else if (this == SOULBOUND) {
					return Component.text(mName, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false).append(Component.text(" (" + mMessage + " to " + infuser + ")", NamedTextColor.DARK_GRAY));
				} else {
					return Component.text(mName, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false).append(Component.text(" (" + mMessage + " by " + infuser + ")", NamedTextColor.DARK_GRAY));
				}
			} else if (mIsStatTrackOption) {
				return Component.text(mName + ": " + (level - 1), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
			} else {
				if (mMessage.isEmpty()) {
					return Component.text(mName + " " + toRomanNumerals(level), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
				} else {
					return Component.text(mName + " " + toRomanNumerals(level), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false).append(Component.text(" (" + mMessage + " by " + infuser + ")", NamedTextColor.DARK_GRAY));
				}
			}
		}

		public Component getDisplay(int level) {
			if (!mHasLevels) {
				return Component.text(mName, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
			} else if (mIsStatTrackOption) {
				return Component.text(mName + ": " + (level - 1), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
			} else {
				return Component.text(mName + " " + toRomanNumerals(level), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
			}
		}

		public static @Nullable
		InfusionType getInfusionType(String name) {
			return REVERSE_MAPPINGS.get(name.replace(" ", ""));
		}
	}

	public enum AttributeType {
		ARMOR(new Armor()),
		AGILITY(new Agility()),
		MAX_HEALTH(Attribute.GENERIC_MAX_HEALTH, "Max Health"),
		ATTACK_DAMAGE_ADD(new AttackDamageAdd()),
		ATTACK_DAMAGE_MULTIPLY(new AttackDamageMultiply()),
		ATTACK_SPEED(Attribute.GENERIC_ATTACK_SPEED, "Attack Speed"),
		PROJECTILE_DAMAGE_ADD(new ProjectileDamageAdd()),
		PROJECTILE_DAMAGE_MULTIPLY(new ProjectileDamageMultiply()),
		PROJECTILE_SPEED(new ProjectileSpeed()),
		THROW_RATE(new ThrowRate()),
		SPELL_DAMAGE(new SpellPower()),
		MAGIC_DAMAGE_ADD(new MagicDamageAdd()),
		MAGIC_DAMAGE_MULTIPLY(new MagicDamageMultiply()),
		SPEED(Attribute.GENERIC_MOVEMENT_SPEED, "Speed"),
		KNOCKBACK_RESISTANCE(Attribute.GENERIC_KNOCKBACK_RESISTANCE, "Knockback Resistance"),
		THORNS(new ThornsDamage());

		static final Map<String, AttributeType> REVERSE_MAPPINGS = Arrays.stream(AttributeType.values())
			.collect(Collectors.toUnmodifiableMap(AttributeType::getCodeName, type -> type));

		static final ImmutableList<String> MAINHAND_ATTRIBUTE_TYPES = ImmutableList.of(
			ATTACK_DAMAGE_ADD.getName(),
			ATTACK_SPEED.getCodeName(),
			PROJECTILE_DAMAGE_ADD.getName(),
			PROJECTILE_SPEED.getName(),
			THROW_RATE.getName()
		);

		static final String KEY = "Attributes";

		final @Nullable Attribute mAttribute;
		final @Nullable ItemStat mItemStat;
		final String mName;
		final String mCodeName;

		AttributeType(Attribute attribute, String name) {
			mAttribute = attribute;
			mItemStat = null;
			mName = name;
			mCodeName = mName.replace(" ", "");
		}

		AttributeType(ItemStat itemStat) {
			this(itemStat, itemStat.getName().replace(" ", ""));
		}

		AttributeType(ItemStat itemStat, String codeName) {
			mAttribute = null;
			mItemStat = itemStat;
			mName = itemStat.getName();
			mCodeName = codeName;
		}

		public @Nullable Attribute getAttribute() {
			return mAttribute;
		}

		public @Nullable ItemStat getItemStat() {
			return mItemStat;
		}

		public String getName() {
			return mName;
		}

		public String getCodeName() {
			return mCodeName;
		}

		public static Component getDisplay(String name, double amount, Slot slot, Operation operation) {
			if (slot == Slot.MAINHAND && operation == Operation.ADD) {
				if (ATTACK_DAMAGE_ADD.getName().equals(name)) {
					return Component.text(String.format(" %s %s", NUMBER_FORMATTER.format(amount + 1), name.replace(" Add", "")), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false);
				} else if (ATTACK_SPEED.getName().equals(name)) {
					return Component.text(String.format(" %s %s", NUMBER_FORMATTER.format(amount + 4), name), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false);
				} else if (PROJECTILE_SPEED.getName().equals(name) || THROW_RATE.getName().equals(name)) {
					return Component.text(String.format(" %s %s", NUMBER_FORMATTER.format(amount), name), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false);
				} else if (PROJECTILE_DAMAGE_ADD.getName().equals(name)) {
					return Component.text(String.format(" %s %s", NUMBER_FORMATTER.format(amount), name.replace(" Add", "")), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false);
				}
			} else if (slot == Slot.MAINHAND && PROJECTILE_SPEED.getName().equals(name)) {
				return Component.text(String.format(" %s %s", NUMBER_FORMATTER.format(amount), name), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false);
			}

			if (ARMOR.getName().equals(name) || AGILITY.getName().equals(name)) {
				if (operation == Operation.ADD) {
					return Component.text(String.format("%s %s", NUMBER_CHANGE_FORMATTER.format(amount), name), amount > 0 ? TextColor.fromHexString("#33CCFF") : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
				} else {
					return Component.text(String.format("%s %s", PERCENT_CHANGE_FORMATTER.format(amount), name), amount > 0 ? TextColor.fromHexString("#33CCFF") : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
				}
			} else {
				if (operation == Operation.ADD && KNOCKBACK_RESISTANCE.getName().equals(name)) {
					return Component.text(String.format("%s %s", NUMBER_CHANGE_FORMATTER.format(amount * 10), name.replace(" Add", "")), amount > 0 ? NamedTextColor.BLUE : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
				} else if (operation == Operation.ADD) {
					return Component.text(String.format("%s %s", NUMBER_CHANGE_FORMATTER.format(amount), name.replace(" Add", "")), amount > 0 ? NamedTextColor.BLUE : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
				} else if (PROJECTILE_SPEED.getName().equals(name)) {
					return Component.text(String.format("%s %s", PERCENT_CHANGE_FORMATTER.format(amount), name.replace(" Multiply", "")), amount > 0 ? NamedTextColor.BLUE : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
				} else {
					return Component.text(String.format("%s %s", PERCENT_CHANGE_FORMATTER.format(amount), name.replace(" Multiply", "")), amount > 0 ? NamedTextColor.BLUE : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
				}
			}
		}

		public static @Nullable AttributeType getAttributeType(String name) {
			return REVERSE_MAPPINGS.get(name.replace(" ", ""));
		}

		public static List<String> getMainhandAttributeNames() {
			return MAINHAND_ATTRIBUTE_TYPES;
		}
	}

	public enum Operation {
		ADD(AttributeModifier.Operation.ADD_NUMBER, "add"),
		MULTIPLY(AttributeModifier.Operation.ADD_SCALAR, "multiply");

		static final String KEY = "Operation";

		final AttributeModifier.Operation mAttributeOperation;
		final String mName;

		Operation(AttributeModifier.Operation attributeOperation, String name) {
			mAttributeOperation = attributeOperation;
			mName = name;
		}

		public AttributeModifier.Operation getAttributeOperation() {
			return mAttributeOperation;
		}

		public String getName() {
			return mName;
		}

		public static @Nullable Operation getOperation(String name) {
			for (Operation operation : Operation.values()) {
				if (operation.getName().replace(" ", "").equals(name.replace(" ", ""))) {
					return operation;
				}
			}

			return null;
		}
	}

	public enum Slot {
		MAINHAND(EquipmentSlot.HAND, "mainhand", Component.text("When in Main Hand:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
		OFFHAND(EquipmentSlot.OFF_HAND, "offhand", Component.text("When in Off Hand:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
		HEAD(EquipmentSlot.HEAD, "head", Component.text("When on Head:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
		CHEST(EquipmentSlot.CHEST, "chest", Component.text("When on Chest:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
		LEGS(EquipmentSlot.LEGS, "legs", Component.text("When on Legs:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)),
		FEET(EquipmentSlot.FEET, "feet", Component.text("When on Feet:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
		static final String KEY = "Slot";

		final EquipmentSlot mEquipmentSlot;
		final String mName;
		final Component mDisplay;

		Slot(EquipmentSlot equipmentSlot, String name, Component display) {
			mEquipmentSlot = equipmentSlot;
			mName = name;
			mDisplay = display;
		}

		public EquipmentSlot getEquipmentSlot() {
			return mEquipmentSlot;
		}

		public String getName() {
			return mName;
		}

		public Component getDisplay() {
			return mDisplay;
		}

		public static @Nullable Slot getSlot(String name) {
			for (Slot slot : Slot.values()) {
				if (slot.getName().replace(" ", "").equals(name.replace(" ", ""))) {
					return slot;
				}
			}

			return null;
		}
	}

	static final DecimalFormat NUMBER_FORMATTER = new DecimalFormat("0.###");
	static final DecimalFormat NUMBER_CHANGE_FORMATTER = new DecimalFormat("+0.###;-0.###");
	static final DecimalFormat PERCENT_CHANGE_FORMATTER = new DecimalFormat("+0.###%;-0.###%");

	static final NavigableMap<Integer, String> ROMAN_NUMERAL_VALUES = new TreeMap<>();

	static {
		ROMAN_NUMERAL_VALUES.put(1000, "M");
		ROMAN_NUMERAL_VALUES.put(900, "CM");
		ROMAN_NUMERAL_VALUES.put(500, "D");
		ROMAN_NUMERAL_VALUES.put(400, "CD");
		ROMAN_NUMERAL_VALUES.put(100, "C");
		ROMAN_NUMERAL_VALUES.put(90, "XC");
		ROMAN_NUMERAL_VALUES.put(50, "L");
		ROMAN_NUMERAL_VALUES.put(40, "XL");
		ROMAN_NUMERAL_VALUES.put(10, "X");
		ROMAN_NUMERAL_VALUES.put(9, "IX");
		ROMAN_NUMERAL_VALUES.put(5, "V");
		ROMAN_NUMERAL_VALUES.put(4, "IV");
		ROMAN_NUMERAL_VALUES.put(1, "I");
	}

	static String toRomanNumerals(int value) {
		int nextNumeralValue = ROMAN_NUMERAL_VALUES.floorKey(value);
		if (value == nextNumeralValue) {
			return ROMAN_NUMERAL_VALUES.get(value);
		}

		return ROMAN_NUMERAL_VALUES.get(nextNumeralValue) + toRomanNumerals(value - nextNumeralValue);
	}

	public static void editItemInfo(final ItemStack item, final Region region, final Tier tier, final Location location) {
		if (item.getType() == Material.AIR) {
			return;
		}
		NBTItem nbt = new NBTItem(item);
		NBTCompound monumenta = nbt.addCompound(MONUMENTA_KEY);

		if (region == Region.NONE) {
			monumenta.removeKey(Region.KEY);
		} else {
			monumenta.setString(Region.KEY, region.getName());
		}

		if (tier == Tier.NONE) {
			monumenta.removeKey(Tier.KEY);
		} else {
			monumenta.setString(Tier.KEY, tier.getName());
		}

		if (location == Location.NONE) {
			monumenta.removeKey(Location.KEY);
		} else {
			monumenta.setString(Location.KEY, location.getName());
		}

		item.setItemMeta(nbt.getItem().getItemMeta());
	}

	public static void addLore(final ItemStack item, final int index, final Component line) {
		if (item.getType() == Material.AIR) {
			return;
		}
		NBTItem nbt = new NBTItem(item);
		NBTList<String> lore = nbt.addCompound(MONUMENTA_KEY).getStringList(LORE_KEY);
		String serializedLine = MessagingUtils.toGson(line).toString();
		if (index < lore.size()) {
			lore.add(index, serializedLine);
		} else {
			lore.add(serializedLine);
		}

		item.setItemMeta(nbt.getItem().getItemMeta());
	}

	public static void removeLore(final ItemStack item, final int index) {
		if (item.getType() == Material.AIR) {
			return;
		}
		NBTItem nbt = new NBTItem(item);
		NBTList<String> lore = nbt.addCompound(MONUMENTA_KEY).getStringList(LORE_KEY);
		if (lore.size() > 0 && index < lore.size()) {
			lore.remove(index);
		} else if (lore.size() > 0) {
			lore.remove(lore.size() - 1);
		}

		item.setItemMeta(nbt.getItem().getItemMeta());
	}

	public static List<Component> getLore(final NBTItem nbt) {
		List<Component> lore = new ArrayList<>();
		for (String serializedLine : nbt.addCompound(MONUMENTA_KEY).getStringList(LORE_KEY)) {
			lore.add(MessagingUtils.fromGson(serializedLine));
		}
		return lore;
	}

	public static List<String> getPlainLore(final NBTItem nbt) {
		List<String> plainLore = new ArrayList<>();
		for (Component line : getLore(nbt)) {
			plainLore.add(MessagingUtils.plainText(line));
		}
		return plainLore;
	}

	public static @Nullable NBTCompound getEnchantments(final NBTItem nbt) {
		NBTCompound monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return null;
		}

		NBTCompound stock = monumenta.getCompound(STOCK_KEY);
		if (stock == null) {
			return null;
		}

		return stock.getCompound(EnchantmentType.KEY);
	}

	public static int getEnchantmentLevel(final @Nullable ItemStack item, final EnchantmentType type) {
		if (item == null || item.getType() == Material.AIR) {
			return 0;
		}
		NBTItem nbt = new NBTItem(item);
		NBTCompound enchantments = ItemStatUtils.getEnchantments(nbt);
		return getEnchantmentLevel(enchantments, type);
	}

	public static int getEnchantmentLevel(final @Nullable NBTCompound enchantments, final EnchantmentType type) {
		if (enchantments == null) {
			return 0;
		}

		NBTCompound enchantment = enchantments.getCompound(type.getName());
		if (enchantment == null) {
			return 0;
		}

		return enchantment.getInteger(LEVEL_KEY);
	}

	public static void addEnchantment(final @Nullable ItemStack item, final EnchantmentType type, final int level) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		NBTItem nbt = new NBTItem(item);
		NBTCompound enchantment = nbt.addCompound(MONUMENTA_KEY).addCompound(STOCK_KEY).addCompound(EnchantmentType.KEY).addCompound(type.getName());
		enchantment.setInteger(LEVEL_KEY, level);

		item.setItemMeta(nbt.getItem().getItemMeta());

		if (type.getEnchantment() != null) {
			ItemMeta meta = item.getItemMeta();
			meta.addEnchant(type.getEnchantment(), level, true);
			item.setItemMeta(meta);
		} else if (type == EnchantmentType.UNBREAKABLE) {
			ItemMeta meta = item.getItemMeta();
			meta.setUnbreakable(true);
			item.setItemMeta(meta);
		}
	}

	public static void removeEnchantment(final @Nullable ItemStack item, final EnchantmentType type) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		NBTItem nbt = new NBTItem(item);
		NBTCompound enchantments = getEnchantments(nbt);
		if (enchantments == null) {
			return;
		}

		enchantments.removeKey(type.getName());

		item.setItemMeta(nbt.getItem().getItemMeta());

		if (type.getEnchantment() != null) {
			ItemMeta meta = item.getItemMeta();
			meta.removeEnchant(type.getEnchantment());
			item.setItemMeta(meta);
		} else if (type == EnchantmentType.UNBREAKABLE) {
			ItemMeta meta = item.getItemMeta();
			meta.setUnbreakable(false);
			item.setItemMeta(meta);
		}
	}

	public static @Nullable NBTCompound getPlayerModified(final NBTItem nbt) {
		NBTCompound monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return null;
		}
		return monumenta.getCompound(PLAYER_MODIFIED_KEY);
	}

	public static NBTCompound addPlayerModified(final NBTItem nbt) {
		return nbt.addCompound(MONUMENTA_KEY).addCompound(PLAYER_MODIFIED_KEY);
	}

	public static @Nullable NBTCompound getInfusions(final NBTItem nbt) {
		NBTCompound monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return null;
		}

		NBTCompound modified = monumenta.getCompound(PLAYER_MODIFIED_KEY);
		if (modified == null) {
			return null;
		}

		return modified.getCompound(InfusionType.KEY);
	}

	public static int getInfusionLevel(final @Nullable ItemStack item, final InfusionType type) {
		if (item == null || item.getType() == Material.AIR) {
			return 0;
		}
		NBTItem nbt = new NBTItem(item);
		NBTCompound infusions = ItemStatUtils.getInfusions(nbt);
		return getInfusionLevel(infusions, type);
	}

	public static int getInfusionLevel(final @Nullable NBTCompound infusions, final @Nullable InfusionType type) {
		if (type == null) {
			return 0;
		}
		if (infusions == null || type.getName() == null) {
			return 0;
		}

		NBTCompound infusion = infusions.getCompound(type.getName());
		if (infusion == null) {
			return 0;
		}

		return infusion.getInteger(LEVEL_KEY);
	}

	public static void addInfusion(final @Nullable ItemStack item, final InfusionType type, final int level, final UUID infuser) {
		addInfusion(item, type, level, infuser, true);
	}

	public static void addInfusion(final @Nullable ItemStack item, final InfusionType type, final int level, final UUID infuser, boolean updateItem) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		NBTItem nbt = new NBTItem(item);
		NBTCompound infusion = nbt.addCompound(MONUMENTA_KEY).addCompound(PLAYER_MODIFIED_KEY).addCompound(InfusionType.KEY).addCompound(type.getName());
		infusion.setInteger(LEVEL_KEY, level);
		infusion.setString(INFUSER_KEY, infuser.toString());

		item.setItemMeta(nbt.getItem().getItemMeta());
		if (updateItem) {
			generateItemStats(item);
		}
	}

	public static void removeInfusion(final ItemStack item, final InfusionType type) {
		removeInfusion(item, type, true);
	}

	public static void removeInfusion(final ItemStack item, final InfusionType type, boolean updateItem) {
		if (item.getType() == Material.AIR) {
			return;
		}
		NBTItem nbt = new NBTItem(item);
		NBTCompound infusions = getInfusions(nbt);
		if (infusions == null || type == null) {
			return;
		}

		infusions.removeKey(type.getName());

		item.setItemMeta(nbt.getItem().getItemMeta());
		if (updateItem) {
			generateItemStats(item);
		}
	}

	public static NBTCompoundList getAttributes(final NBTItem nbt) {
		NBTCompound monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return null;
		}

		NBTCompound stock = monumenta.getCompound(STOCK_KEY);
		if (stock == null) {
			return null;
		}

		return stock.getCompoundList(AttributeType.KEY);
	}

	public static boolean hasAttributeInSlot(final @Nullable ItemStack itemStack, final Slot slot) {
		if (itemStack == null || itemStack.getType() == Material.AIR) {
			return false;
		}
		NBTItem nbt = new NBTItem(itemStack);
		NBTCompoundList attributes = getAttributes(nbt);
		if (attributes == null) {
			return false;
		}
		for (NBTListCompound attribute : attributes) {
			if (attribute.getString(Slot.KEY).equals(slot.getName())) {
				return true;
			}
		}
		return false;
	}

	public static double getAttributeAmount(final @Nullable NBTCompoundList attributes, final AttributeType type, final Operation operation, final Slot slot) {
		if (attributes == null) {
			return 0;
		}

		for (NBTListCompound attribute : attributes) {
			if (attribute.getString(ATTRIBUTE_NAME_KEY).equals(type.getName()) && attribute.getString(Operation.KEY).equals(operation.getName()) && attribute.getString(Slot.KEY).equals(slot.getName())) {
				return attribute.getDouble(AMOUNT_KEY);
			}
		}

		return 0;
	}

	public static double getAttributeAmount(final @Nullable ItemStack itemStack, final AttributeType type, final Operation operation, final Slot slot) {
		if (itemStack == null || itemStack.getType() == Material.AIR) {
			return 0;
		}
		NBTItem nbt = new NBTItem(itemStack);
		NBTCompoundList compound = getAttributes(nbt);
		return getAttributeAmount(compound, type, operation, slot);
	}

	public static void addAttribute(final ItemStack item, final AttributeType type, final double amount, final Operation operation, final Slot slot) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		removeAttribute(item, type, operation, slot);

		NBTItem nbt = new NBTItem(item);
		NBTCompoundList attributes = nbt.addCompound(MONUMENTA_KEY).addCompound(STOCK_KEY).getCompoundList(AttributeType.KEY);
		NBTListCompound attribute = attributes.addCompound();
		attribute.setString(ATTRIBUTE_NAME_KEY, type.getName());
		attribute.setString(Operation.KEY, operation.getName());
		attribute.setDouble(AMOUNT_KEY, amount);
		attribute.setString(Slot.KEY, slot.getName());

		item.setItemMeta(nbt.getItem().getItemMeta());

		if (type.getAttribute() != null) {
			ItemMeta meta = item.getItemMeta();
			meta.addAttributeModifier(type.getAttribute(), new AttributeModifier(UUID.randomUUID(), "Modifier", amount, operation.getAttributeOperation(), slot.getEquipmentSlot()));
			item.setItemMeta(meta);
		}
	}

	public static void removeAttribute(final ItemStack item, final AttributeType type, final Operation operation, final Slot slot) {
		if (item.getType() == Material.AIR) {
			return;
		}
		NBTItem nbt = new NBTItem(item);
		NBTCompoundList attributes = getAttributes(nbt);
		if (attributes == null) {
			return;
		}

		attributes.removeIf((attribute) -> {
			return attribute.getString(ATTRIBUTE_NAME_KEY).equals(type.getName()) && attribute.getString(Operation.KEY).equals(operation.getName()) && attribute.getString(Slot.KEY).equals(slot.getName());
		});

		item.setItemMeta(nbt.getItem().getItemMeta());

		if (type.getAttribute() != null) {
			ItemMeta meta = item.getItemMeta();
			Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(slot.getEquipmentSlot()).get(type.getAttribute());
			if (modifiers == null) {
				return;
			}

			for (AttributeModifier modifier : modifiers) {
				if (modifier.getOperation() == operation.getAttributeOperation()) {
					meta.removeAttributeModifier(type.getAttribute(), modifier);
					break;
				}
			}

			item.setItemMeta(meta);
		}
	}

	public static Region getRegion(final @Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return Region.NONE;
		}
		NBTItem nbt = new NBTItem(item);
		NBTCompound monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return Region.NONE;
		}

		ItemMeta meta = item.getItemMeta();
		if (meta.hasDisplayName()) {
			String name = meta.getDisplayName();
			if (name.contains("Alchemist's Bag")) {
				return Region.VALLEY;
			} else if (name.contains("Experiencinator")) {
				return Region.VALLEY;
			} else if (name.contains("Crystallizer")) {
				return Region.ISLES;
			}
		}

		String regionString = monumenta.getString(Region.KEY);
		if (regionString != null) {
			return Region.getRegion(regionString);
		}

		if (ItemUtils.isShulkerBox(item.getType())) {
			return Region.SHULKER_BOX;
		}

		return Region.NONE;
	}

	public static Tier getTier(final @Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return Tier.NONE;
		}
		NBTItem nbt = new NBTItem(item);
		NBTCompound monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return Tier.NONE;
		}

		ItemMeta meta = item.getItemMeta();
		if (meta.hasDisplayName()) {
			String itemName = meta.getDisplayName();
			if (itemName.contains("Alchemist's Bag")) {
				return Tier.RARE;
			}
		}

		String tierString = monumenta.getString(Tier.KEY);
		if (tierString != null) {
			return Tier.getTier(tierString);
		}

		if (ItemUtils.isShulkerBox(item.getType())) {
			return Tier.SHULKER_BOX;
		}

		return Tier.NONE;
	}

	public static boolean isShattered(final @Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}
		NBTItem nbt = new NBTItem(item);

		NBTCompound monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return false;
		}

		NBTCompound modified = monumenta.getCompound(PLAYER_MODIFIED_KEY);
		if (modified == null) {
			return false;
		}

		return modified.hasKey(SHATTERED_KEY);
	}

	public static boolean shatter(final @Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}
		NBTItem nbt = new NBTItem(item);
		NBTCompound modified = nbt.addCompound(MONUMENTA_KEY).addCompound(PLAYER_MODIFIED_KEY);
		modified.setInteger(SHATTERED_KEY, 1);

		item.setItemMeta(nbt.getItem().getItemMeta());
		generateItemStats(item);
		return true;
	}

	public static void reforge(final ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		NBTItem nbt = new NBTItem(item);

		NBTCompound monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return;
		}

		NBTCompound modified = monumenta.getCompound(PLAYER_MODIFIED_KEY);
		if (modified == null) {
			return;
		}

		modified.removeKey(SHATTERED_KEY);

		item.setItemMeta(nbt.getItem().getItemMeta());
		generateItemStats(item);
	}

	public static void generateItemStats(final ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		NBTItem nbt = new NBTItem(item);
		NBTCompound monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null || monumenta.getKeys().isEmpty()) {
			return;
		} else {
			// There is probably a cleaner way to clean up unused NBT, not sure if recursion directly works due to the existence of both NBTCompounds and NBTCompoundLists
			// TODO: clean up other unused things from item (e.g. empty lore, reset hideflags if no NBT)
			Set<String> keys;

			NBTCompound stock = monumenta.getCompound(STOCK_KEY);
			if (stock != null) {
				NBTCompound enchantments = stock.getCompound(EnchantmentType.KEY);
				if (enchantments != null) {
					keys = enchantments.getKeys();
					if (keys == null || keys.isEmpty()) {
						stock.removeKey(EnchantmentType.KEY);
					}
				}

				NBTCompoundList attributes = stock.getCompoundList(AttributeType.KEY);
				if (attributes != null && attributes.isEmpty()) {
					stock.removeKey(AttributeType.KEY);
				}

				keys = stock.getKeys();
				if (keys == null || keys.isEmpty()) {
					monumenta.removeKey(STOCK_KEY);
				}
			}

			NBTCompound player = monumenta.getCompound(PLAYER_MODIFIED_KEY);
			if (player != null) {
				NBTCompound infusions = player.getCompound(InfusionType.KEY);
				if (infusions != null) {
					keys = infusions.getKeys();
					if (keys == null || keys.isEmpty()) {
						player.removeKey(InfusionType.KEY);
					}
				}

				keys = player.getKeys();
				if (keys == null || keys.isEmpty()) {
					monumenta.removeKey(PLAYER_MODIFIED_KEY);
				}
			}

			NBTList<String> lore = monumenta.getStringList(LORE_KEY);
			if (lore != null && lore.isEmpty()) {
				monumenta.removeKey(LORE_KEY);
			}
		}

		List<Component> lore = new ArrayList<>();

		// Checks for PI + Totem of Transposing
		if (ItemUtils.getPlainName(item).equals("Potion Injector") && ItemUtils.isShulkerBox(item.getType())) {
			List<String> plainLore = ItemUtils.getPlainLore(item);
			Component potionName = item.lore().get(1);
			lore.add(Component.text(plainLore.get(0), NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
			lore.add(potionName);
		} else if (ItemUtils.getPlainName(item).equals("Totem of Transposing")) {
			List<String> plainLore = ItemUtils.getPlainLore(item);
			lore.add(Component.text(plainLore.get(0), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
		}

		List<Component> tagsLater = new ArrayList<>();
		List<Component> unbreakingTags = new ArrayList<>();

		if (getEnchantmentLevel(item, EnchantmentType.HIDE_ENCHANTS) == 0) {
			NBTCompound enchantments = getEnchantments(nbt);
			if (enchantments != null) {
				for (EnchantmentType type : EnchantmentType.values()) {
					NBTCompound enchantment = enchantments.getCompound(type.getName());
					if (enchantment != null) {
						if (type.isItemTypeEnchantment()) {
							tagsLater.add(type.getDisplay(enchantment.getInteger(LEVEL_KEY)));
						} else if (type.getName().equals("Mending") || type.getName().equals("Unbreaking") || type.getName().equals("Unbreakable")) {
							unbreakingTags.add(type.getDisplay(enchantment.getInteger(LEVEL_KEY)));
						} else if (type.isHidden()) {
							continue;
						} else {
							lore.add(type.getDisplay(enchantment.getInteger(LEVEL_KEY)));
						}
					}
				}
			}
		}


		List<Component> statTrackLater = new ArrayList<>();
		List<Component> infusionTagsLater = new ArrayList<>();

		NBTCompound infusions = getInfusions(nbt);
		if (infusions != null) {
			for (InfusionType type : InfusionType.values()) {
				NBTCompound infusion = infusions.getCompound(type.getName());
				if (infusion != null) {
					if (InfusionType.STAT_TRACK_OPTIONS.contains(type) || type.getName().equals("Stat Track")) {
						statTrackLater.add(type.getDisplay(infusion.getInteger(LEVEL_KEY)));
					} else if (!type.getMessage().equals("") && !type.getMessage().equals("Tracked")) {
						infusionTagsLater.add(type.getDisplay(infusion.getInteger(LEVEL_KEY), MonumentaRedisSyncAPI.cachedUuidToName(UUID.fromString(infusion.getString(INFUSER_KEY)))));
					} else if (type.getMessage().equals("")) {
						lore.add(type.getDisplay(infusion.getInteger(LEVEL_KEY)));
					} else {
						lore.add(type.getDisplay(infusion.getInteger(LEVEL_KEY), MonumentaRedisSyncAPI.cachedUuidToName(UUID.fromString(infusion.getString(INFUSER_KEY)))));
					}
				}
			}
		}

		// Add unbreaking tags
		lore.addAll(unbreakingTags);

		// Add infusions with message
		lore.addAll(infusionTagsLater);

		// Add stat tracking lore
		lore.addAll(statTrackLater);

		// Add Magic Wand Tag *after* all other stats,
		lore.addAll(tagsLater);

		if (getEnchantmentLevel(item, EnchantmentType.HIDE_INFO) == 0) {
			String regionString = monumenta.getString(Region.KEY);
			if (regionString != null) {
				Region region = Region.getRegion(regionString);
				if (region != null) {
					Tier tier = Tier.getTier(monumenta.getString(Tier.KEY));
					if (tier != null && tier != Tier.NONE) {
						lore.add(region.getDisplay().append(tier.getDisplay()));
					}
				}

				Location location = Location.getLocation(monumenta.getString(Location.KEY));
				if (location != null) {
					lore.add(location.getDisplay());
				}
			}
		}

		NBTList<String> description = monumenta.getStringList(LORE_KEY);
		if (description != null) {
			for (String serializedLine : description) {
				Component lineAdd = Component.text("", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);
				lineAdd = lineAdd.append(MessagingUtils.fromGson(serializedLine));
				lore.add(lineAdd);
			}
		}

		if (ItemStatUtils.isShattered(item)) {
			lore.add(Component.text("* SHATTERED *", NamedTextColor.DARK_RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text("Maybe a Master Repairman", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text("could reforge it...", NamedTextColor.DARK_RED).decoration(TextDecoration.ITALIC, false));
		}

		NBTCompoundList attributes = getAttributes(nbt);
		EnumMap<Slot, List<NBTListCompound>> attributesBySlots = new EnumMap<>(Slot.class);
		List<NBTListCompound> mainhandAttributes = new ArrayList<>();

		for (Slot slot : Slot.values()) {
			attributesBySlots.put(slot, new ArrayList<>());
		}

		if (attributes != null) {
			for (NBTListCompound attribute : attributes) {
				Slot slot = Slot.getSlot(attribute.getString(Slot.KEY));
				if (slot == Slot.MAINHAND && Operation.getOperation(attribute.getString(Operation.KEY)) == Operation.ADD && (AttributeType.getMainhandAttributeNames().contains(attribute.getString(ATTRIBUTE_NAME_KEY)) || attribute.getString(ATTRIBUTE_NAME_KEY).equals("Attack Speed"))) {
					mainhandAttributes.add(attribute);
				} else {
					List<NBTListCompound> slotAttributes = attributesBySlots.get(slot);
					if (slotAttributes != null) {
						slotAttributes.add(attribute);
					}
				}
			}
		}

		if (getEnchantmentLevel(item, EnchantmentType.HIDE_ATTRIBUTES) == 0) {
			for (Slot slot : Slot.values()) {
				List<NBTListCompound> attributesBySlot = attributesBySlots.get(slot);
				if ((attributesBySlot == null || attributesBySlot.isEmpty()) && (slot != Slot.MAINHAND || mainhandAttributes.isEmpty())) {
					continue;
				}

				lore.add(Component.empty());
				lore.add(slot.getDisplay());

				if (slot == Slot.MAINHAND) {
					boolean needsAttackSpeed = false;
					int atksIndex = 0;
					for (String name : AttributeType.getMainhandAttributeNames()) {
						for (NBTListCompound attribute : mainhandAttributes) {
							if (name.equals(attribute.getString(ATTRIBUTE_NAME_KEY)) && name.equals("Attack Damage Add")) {
								needsAttackSpeed = true;
								atksIndex = lore.size() + 1;
							}
							if (name.equals(attribute.getString(ATTRIBUTE_NAME_KEY)) && !lore.contains(AttributeType.getDisplay(name, attribute.getDouble(AMOUNT_KEY), Slot.getSlot(attribute.getString(Slot.KEY)), Operation.getOperation(attribute.getString(Operation.KEY))))) {
								lore.add(AttributeType.getDisplay(name, attribute.getDouble(AMOUNT_KEY), Slot.getSlot(attribute.getString(Slot.KEY)), Operation.getOperation(attribute.getString(Operation.KEY))));
							} else if ((name.equals("AttackSpeed") && attribute.getString(ATTRIBUTE_NAME_KEY).equals("Attack Speed") && attribute.getDouble(AMOUNT_KEY) != 0) && !lore.contains(AttributeType.getDisplay(name, attribute.getDouble(AMOUNT_KEY), Slot.getSlot(attribute.getString(Slot.KEY)), Operation.getOperation(attribute.getString(Operation.KEY))))) {
								lore.add(AttributeType.getDisplay("Attack Speed", attribute.getDouble(AMOUNT_KEY), Slot.getSlot(attribute.getString(Slot.KEY)), Operation.getOperation(attribute.getString(Operation.KEY))));
								needsAttackSpeed = false;
							}
						}
					}
					if (needsAttackSpeed) {
						lore.add(atksIndex, AttributeType.getDisplay("Attack Speed", 0, Slot.MAINHAND, Operation.ADD));
					}
				}

				if (attributesBySlot != null) {
					for (AttributeType type : AttributeType.values()) {
						String name = type.getName();
						for (Operation operation : Operation.values()) {
							for (NBTListCompound attribute : attributesBySlot) {
								if (Operation.getOperation(attribute.getString(Operation.KEY)) == operation && name.equals(attribute.getString(ATTRIBUTE_NAME_KEY))) {
									if (!lore.contains(AttributeType.getDisplay(name, attribute.getDouble(AMOUNT_KEY), Slot.getSlot(attribute.getString(Slot.KEY)), operation))) {
										lore.add(AttributeType.getDisplay(name, attribute.getDouble(AMOUNT_KEY), Slot.getSlot(attribute.getString(Slot.KEY)), operation));
									}
									break;
								}
							}
						}
					}
				}
			}
		}

		Set<String> keys = monumenta.getKeys();
		if (keys == null || keys.isEmpty()) {
			nbt.removeKey(MONUMENTA_KEY);
		}

		item.setItemMeta(nbt.getItem().getItemMeta());

		lore.removeAll(Collections.singletonList(DUMMY_LORE_TO_REMOVE));
		item.lore(lore);

		ItemMeta meta = item.getItemMeta();
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.addItemFlags(ItemFlag.HIDE_DYE);
		meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
		if (item.getType().name().contains("PATTERN") || item.getType().name().contains("SHIELD")) {
			meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		}

		boolean hasDummyArmorToughnessAttribute = false;
		if (meta.hasAttributeModifiers()) {
			Collection<AttributeModifier> toughnessAttrs = meta.getAttributeModifiers(Attribute.GENERIC_ARMOR_TOUGHNESS);
			hasDummyArmorToughnessAttribute = toughnessAttrs != null && toughnessAttrs.size() == 1 && toughnessAttrs.iterator().next().getName().equals(MONUMENTA_DUMMY_TOUGHNESS_ATTRIBUTE_NAME);
		}

		if (!hasDummyArmorToughnessAttribute) {
			meta.removeAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS);
			meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS, new AttributeModifier(UUID.randomUUID(), MONUMENTA_DUMMY_TOUGHNESS_ATTRIBUTE_NAME, 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
		}

		if (item.getType() == Material.BOW || item.getType() == Material.CROSSBOW) {
			meta.addEnchant(Enchantment.WATER_WORKER, 1, true);
		} else {
			meta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
		}

		item.setItemMeta(meta);
		ItemUtils.setPlainLore(item);
	}

	public static void registerInfoCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editinfo");

		Region[] regionsRaw = Region.values();
		String[] regions = new String[regionsRaw.length];
		for (int i = 0; i < regions.length; i++) {
			regions[i] = regionsRaw[i].getName();
		}

		Tier[] tiersRaw = Tier.values();
		String[] tiers = new String[tiersRaw.length];
		for (int i = 0; i < tiers.length; i++) {
			tiers[i] = tiersRaw[i].getName();
		}

		Location[] locationsRaw = Location.values();
		String[] locations = new String[locationsRaw.length];
		for (int i = 0; i < locations.length; i++) {
			locations[i] = locationsRaw[i].getName();
		}

		List<Argument> arguments = new ArrayList<>();
		arguments.add(new StringArgument("region").overrideSuggestions(regions));
		arguments.add(new StringArgument("tier").overrideSuggestions(tiers));
		arguments.add(new StringArgument("location").overrideSuggestions(locations));

		new CommandAPICommand("editinfo").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
				return;
			}
			Region region = Region.getRegion((String) args[0]);
			Tier tier = Tier.getTier((String) args[1]);
			Location location = Location.getLocation((String) args[2]);
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item == null || item.getType() == Material.AIR) {
				player.sendMessage(ChatColor.RED + "Must be holding an item!");
				return;
			}

			editItemInfo(item, region, tier, location);

			generateItemStats(item);
			Plugin.getInstance().mItemStatManager.getPlayerItemStats(player).updateStats(true);
		}).register();
	}

	public static void registerLoreCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editlore");

		List<Argument> arguments = new ArrayList<>();
		arguments.add(new MultiLiteralArgument("add"));
		arguments.add(new IntegerArgument("index", 0));

		new CommandAPICommand("editlore").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
				return;
			}
			Integer index = (Integer) args[1];
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item == null || item.getType() == Material.AIR) {
				player.sendMessage(ChatColor.RED + "Must be holding an item!");
				return;
			}

			addLore(item, index, Component.empty());

			generateItemStats(item);
		}).register();

		arguments.add(new GreedyStringArgument("lore"));

		new CommandAPICommand("editlore").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
				return;
			}
			Integer index = (Integer) args[1];
			String lore = (String) args[2];
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item == null || item.getType() == Material.AIR) {
				player.sendMessage(ChatColor.RED + "Must be holding an item!");
				return;
			}

			addLore(item, index, MessagingUtils.fromMiniMessage(lore));

			generateItemStats(item);
		}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("del"));
		arguments.add(new IntegerArgument("index", 0));

		new CommandAPICommand("editlore").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
				return;
			}
			Integer index = (Integer) args[1];
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item == null || item.getType() == Material.AIR) {
				player.sendMessage(ChatColor.RED + "Must be holding an item!");
				return;
			}

			removeLore(item, index);

			generateItemStats(item);
		}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("register"));

		new CommandAPICommand("editlore").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
				return;
			}
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item == null || item.getType() == Material.AIR) {
				player.sendMessage(ChatColor.RED + "Must be holding an item!");
				return;
			}

			List<Component> oldLore = item.lore();
			if (oldLore == null || oldLore.isEmpty()) {
				player.sendMessage(ChatColor.RED + "Item has no lore!");
				return;
			}

			int loreIdx = 0;
			for (Component c : oldLore) {
				addLore(item, loreIdx, c);
				loreIdx++;
			}

			generateItemStats(item);
		}).register();
	}

	public static void registerNameCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editname");

		Location[] locationsRaw = Location.values();
		String[] locations = new String[locationsRaw.length];
		for (int i = 0; i < locations.length; i++) {
			locations[i] = locationsRaw[i].getName();
		}

		List<Argument> arguments = new ArrayList<>();
		arguments.add(new StringArgument("location").replaceSuggestions(info -> locations));
		arguments.add(new BooleanArgument("bold"));
		arguments.add(new BooleanArgument("underline"));
		arguments.add(new GreedyStringArgument("name"));

		new CommandAPICommand("editname").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
				return;
			}
			Location location = Location.getLocation((String) args[0]);
			Boolean bold = (Boolean) args[1];
			Boolean underline = (Boolean) args[2];
			String name = (String) args[3];
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item == null || item.getType() == Material.AIR) {
				player.sendMessage(ChatColor.RED + "Must be holding an item!");
				return;
			}

			ItemMeta itemMeta = item.getItemMeta();
			itemMeta.displayName(Component.text(name, TextColor.fromHexString(location.getDisplay().color().asHexString())).decoration(TextDecoration.BOLD, bold).decoration(TextDecoration.UNDERLINED, underline).decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(itemMeta);

		}).register();
	}

	public static void registerEnchCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editench");

		String[] enchantments = new String[EnchantmentType.values().length + InfusionType.values().length];
		int i = 0;

		for (EnchantmentType enchantment : EnchantmentType.values()) {
			if (enchantment != null && enchantment.getName() != null) {
				enchantments[i] = enchantment.getName().replace(" ", "");
				i++;
			}
		}

		for (InfusionType enchantment : InfusionType.values()) {
			if (enchantment != null && enchantment.getName() != null) {
				enchantments[i] = enchantment.getName().replace(" ", "");
				i++;
			}
		}

		List<Argument> arguments = new ArrayList<>();
		arguments.add(new StringArgument("enchantment").replaceSuggestions(info -> enchantments));
		arguments.add(new IntegerArgument("level", 0));

		new CommandAPICommand("editench").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
				return;
			}
			String enchantment = (String) args[0];
			Integer level = (Integer) args[1];
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item == null || item.getType() == Material.AIR) {
				player.sendMessage(ChatColor.RED + "Must be holding an item!");
				return;
			}

			EnchantmentType type1 = EnchantmentType.getEnchantmentType(enchantment);
			if (type1 != null) {
				if (level > 0) {
					addEnchantment(item, type1, level);
				} else {
					removeEnchantment(item, type1);
				}
			}

			InfusionType type2 = InfusionType.getInfusionType(enchantment);
			if (type2 != null) {
				if (level > 0) {
					addInfusion(item, type2, level, player.getUniqueId(), false);
				} else {
					removeInfusion(item, type2, false);
				}
			}

			generateItemStats(item);
			Plugin.getInstance().mItemStatManager.getPlayerItemStats(player).updateStats(true);
		}).register();
	}

	public static void registerAttrCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editattr");

		String[] attributes = new String[AttributeType.values().length];
		int i = 0;

		for (AttributeType attribute : AttributeType.values()) {
			attributes[i] = attribute.getCodeName().replace(" ", "");
			i++;
		}

		List<Argument> arguments = new ArrayList<>();
		arguments.add(new StringArgument("attribute").replaceSuggestions(info -> attributes));
		arguments.add(new DoubleArgument("amount"));
		arguments.add(new MultiLiteralArgument(Operation.ADD.getName(), Operation.MULTIPLY.getName()));
		arguments.add(new MultiLiteralArgument(Slot.MAINHAND.getName(), Slot.OFFHAND.getName(), Slot.HEAD.getName(), Slot.CHEST.getName(), Slot.LEGS.getName(), Slot.FEET.getName()));

		new CommandAPICommand("editattr").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
				return;
			}
			String attribute = (String) args[0];
			Double amount = (Double) args[1];
			Operation operation = Operation.getOperation((String) args[2]);
			Slot slot = Slot.getSlot((String) args[3]);
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item == null || item.getType() == Material.AIR) {
				player.sendMessage(ChatColor.RED + "Must be holding an item!");
				return;
			}

			if ((args[3] == "add" && attribute.contains("Multiply")) || (args[3] == "multiply" && attribute.contains("Add"))) {
				return;
			}

			if (args[3] == "add" && attribute.contains("ProjectileSpeed")) {
				player.sendMessage("You are using the wrong type of Proj Speed, do multiply");
				return;
			}

			AttributeType type1 = AttributeType.getAttributeType(attribute);
			if (type1 != null) {
				if (amount != 0) {
					addAttribute(item, type1, amount, operation, slot);
				} else {
					removeAttribute(item, type1, operation, slot);
				}
			}

			generateItemStats(item);
			Plugin.getInstance().mItemStatManager.getPlayerItemStats(player).updateStats(true);
		}).register();
	}
}
