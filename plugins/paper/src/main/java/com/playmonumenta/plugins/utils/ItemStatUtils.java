package com.playmonumenta.plugins.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.Alchemist;
import com.playmonumenta.plugins.classes.MonumentaClasses;
import com.playmonumenta.plugins.classes.PlayerClass;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.inventories.CustomContainerItemManager;
import com.playmonumenta.plugins.itemstats.EffectType;
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
import com.playmonumenta.plugins.listeners.QuiverListener;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTList;
import de.tr7zw.nbtapi.NBTListCompound;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Nullable;

public class ItemStatUtils {

	static final String MONUMENTA_DUMMY_TOUGHNESS_ATTRIBUTE_NAME = "MMDummy";
	public static final String MONUMENTA_KEY = "Monumenta";
	public static final String LORE_KEY = "Lore";
	public static final String STOCK_KEY = "Stock";
	public static final String PLAYER_MODIFIED_KEY = "PlayerModified";
	static final String LEVEL_KEY = "Level";
	static final String INFUSER_KEY = "Infuser";
	static final String ATTRIBUTE_NAME_KEY = "AttributeName";
	static final String CHARM_KEY = "CharmText";
	static final String CHARM_POWER_KEY = "CharmPower";
	static final String AMOUNT_KEY = "Amount";
	public static final String EFFECT_TYPE_KEY = "EffectType";
	static final String EFFECT_DURATION_KEY = "EffectDuration";
	public static final String EFFECT_STRENGTH_KEY = "EffectStrength";
	static final String EFFECT_SOURCE_KEY = "EffectSource";
	static final String DIRTY_KEY = "Dirty";
	static final String SHULKER_SLOTS_KEY = "ShulkerSlots";
	static final String CUSTOM_INVENTORY_TYPES_LIMIT_KEY = "CustomInventoryTypesLimit";
	static final String CUSTOM_INVENTORY_TOTAL_ITEMS_LIMIT_KEY = "CustomInventoryTotalItemsLimit";
	static final String CUSTOM_INVENTORY_ITEMS_PER_TYPE_LIMIT_KEY = "CustomInventoryItemsPerTypeLimit";
	static final String IS_QUIVER_KEY = "IsQuiver";
	static final String QUIVER_ARROW_TRANSFORM_MODE_KEY = "ArrowTransformMode";
	static final String CHARGES_KEY = "Charges";
	public static final String ITEMS_KEY = "Items";
	public static final String VANITY_ITEMS_KEY = "VanityItems";
	public static final String PLAYER_CUSTOM_NAME_KEY = "PlayerCustomName";
	public static final String CUSTOM_SKIN_KEY = "CustomSkin";

	static final Component DUMMY_LORE_TO_REMOVE = Component.text("DUMMY LORE TO REMOVE", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false);

	public enum Region {
		NONE("none", DUMMY_LORE_TO_REMOVE),
		SHULKER_BOX("shulker", Component.text("INVALID ENTRY", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)),
		VALLEY("valley", Component.text("King's Valley : ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)),
		ISLES("isles", Component.text("Celsian Isles : ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)),
		RING("ring", Component.text("Architect's Ring : ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));

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
		EPIC("epic", Component.text("Epic", TextColor.fromHexString("#B314E3")).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true)),
		LEGENDARY("legendary", Component.text("Legendary", TextColor.fromHexString("#FFD700")).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true)),
		UNIQUE("unique", Component.text("Unique", TextColor.fromHexString("#C8A2C8")).decoration(TextDecoration.ITALIC, false)),
		PATRON("patron", Component.text("Patron Made", TextColor.fromHexString("#82DB17")).decoration(TextDecoration.ITALIC, false)),
		EVENT("event", Component.text("Event", TextColor.fromHexString("#7FFFD4")).decoration(TextDecoration.ITALIC, false)),
		LEGACY("legacy", Component.text("Legacy", TextColor.fromHexString("#EEE6D6")).decoration(TextDecoration.ITALIC, false)),
		CURRENCY("currency", Component.text("Currency", TextColor.fromHexString("#DCAE32")).decoration(TextDecoration.ITALIC, false)),
		EVENT_CURRENCY("event_currency", Component.text("Event Currency", TextColor.fromHexString("#DCAE32")).decoration(TextDecoration.ITALIC, false)),
		KEYTIER("key", Component.text("Key", TextColor.fromHexString("#47B6B5")).decoration(TextDecoration.ITALIC, false)),
		TROPHY("trophy", Component.text("Trophy", TextColor.fromHexString("#CAFFFD")).decoration(TextDecoration.ITALIC, false)),
		OBFUSCATED("obfuscated", Component.text("Stick_:)", TextColor.fromHexString("#5D2D87")).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.OBFUSCATED, true)),
		SHULKER_BOX("shulker", Component.text("Invalid Type", TextColor.fromHexString("#EEE6D6")).decoration(TextDecoration.ITALIC, false)),
		CHARM("charm", Component.text("Charm", TextColor.fromHexString("#FFFA75")).decoration(TextDecoration.ITALIC, false)),
		RARE_CHARM("rarecharm", Component.text("Rare Charm", TextColor.fromHexString("#4AC2E5")).decoration(TextDecoration.ITALIC, false)),
		EPIC_CHARM("epiccharm", Component.text("Epic Charm", TextColor.fromHexString("#B314E3")).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true)),
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

	public enum Masterwork {
		NONE("none", DUMMY_LORE_TO_REMOVE),
		ZERO("0", Component.text("", NamedTextColor.DARK_GRAY).append(Component.text("☆☆☆", NamedTextColor.DARK_GRAY)).decoration(TextDecoration.ITALIC, false)),
		I("1", Component.text("★", TextColor.fromHexString("#FFB43E")).append(Component.text("☆☆", NamedTextColor.DARK_GRAY)).decoration(TextDecoration.ITALIC, false)),
		II("2", Component.text("★★", TextColor.fromHexString("#FFB43E")).append(Component.text("☆", NamedTextColor.DARK_GRAY)).decoration(TextDecoration.ITALIC, false)),
		III("3", Component.text("★★★", TextColor.fromHexString("#FFB43E")).append(Component.text("", NamedTextColor.DARK_GRAY)).decoration(TextDecoration.ITALIC, false)),
		IV("4", Component.text("★★★★", TextColor.fromHexString("#FFB43E")).append(Component.text("", NamedTextColor.DARK_GRAY)).decoration(TextDecoration.ITALIC, false)),
		V("5", Component.text("★★★★★", TextColor.fromHexString("#FFB43E")).append(Component.text("", NamedTextColor.DARK_GRAY)).decoration(TextDecoration.ITALIC, false)),
		VI("6", Component.text("★★★★★★", TextColor.fromHexString("#FFB43E")).append(Component.text("", NamedTextColor.DARK_GRAY)).decoration(TextDecoration.ITALIC, false)),
		VIIA("7a", Component.text("★★★★★★★", TextColor.fromHexString("#D02E28")).append(Component.text("", NamedTextColor.DARK_GRAY)).decoration(TextDecoration.ITALIC, false)),
		VIIB("7b", Component.text("★★★★★★★", TextColor.fromHexString("#4AC2E5")).append(Component.text("", NamedTextColor.DARK_GRAY)).decoration(TextDecoration.ITALIC, false)),
		VIIC("7c", Component.text("★★★★★★★", TextColor.fromHexString("#FFFA75")).append(Component.text("", NamedTextColor.DARK_GRAY)).decoration(TextDecoration.ITALIC, false)),
		ERROR("error", Component.text("ERROR", TextColor.fromHexString("#704C8A")).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.OBFUSCATED, true));

		static final String KEY = "Masterwork";

		final String mName;
		final Component mDisplay;
		final String mPlainDisplay;

		Masterwork(String name, Component display) {
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

		public static Masterwork getMasterwork(String name) {
			for (Masterwork m : Masterwork.values()) {
				if (m.getName().equals(name)) {
					return m;
				}
			}

			return Masterwork.NONE;
		}
	}

	public enum Location {
		NONE("none", DUMMY_LORE_TO_REMOVE),
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
		BLITZ("blitz", "Plunderer's Blitz", TextColor.fromHexString("#DAAD3E")),
		SOULTHREAD("soul", "Soulwoven", TextColor.fromHexString("#7FFFD4")),
		SCIENCE("science", "P.O.R.T.A.L.", TextColor.fromHexString("#DCE8E3")),
		BLUESTRIKE("bluestrike", "Masquerader's Ruin", TextColor.fromHexString("#326DA8")),
		GODSPORE("godspore", "The Godspore's Domain", TextColor.fromHexString("#426B29")),
		GALLERYOFFEAR("gallerybase", "Gallery of Fear", TextColor.fromHexString("#39B14E")),
		GOFMAPONE("gallery1", "Sanguine Halls", TextColor.fromHexString("#AB0000")),
		FALLENSTAR("fallenstar", "Shadow of a Fallen Star", TextColor.fromHexString("#00C0A3")),
		AMBER("amber", "item name color", TextColor.fromHexString("#FFBF00")),
		GOLD("gold", "item name color", TextColor.fromHexString("#FFD700")),
		DARKBLUE("darkblue", "itemnamecolor", TextColor.fromHexString("#FFFFAA")),
		INDIGO("indigo", "item name color", TextColor.fromHexString("#6F00FF")),
		MIDBLUE("midblue", "itemnamecolor", TextColor.fromHexString("#366EF8"));

		static final String KEY = "Location";

		final String mName;
		final Component mDisplay;
		final TextColor mColor;

		Location(String name, String display, TextColor color) {
			mName = name;
			mDisplay = Component.text(display, color).decoration(TextDecoration.ITALIC, false);
			mColor = color;
		}

		Location(String name, Component display) {
			mName = name;
			mDisplay = display;
			mColor = display.color();
		}

		public String getName() {
			return mName;
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

	/*
	 * TODO: converting these enums (EnchantmentType, InfusionType, AttributeType, AbilityAttributeType)
	 * into a proper class hierarchy with static final instances would cut down on code and improve logic
	 */

	public enum EnchantmentType {
		// Region Scaling
		OFFHAND_MAINHAND_DISABLE(new OffhandMainhandDisable(), false, false, false, false),
		MAINHAND_OFFHAND_DISABLE(new MainhandOffhandDisable(), false, false, false, false),
		REGION_SCALING_DAMAGE_DEALT(new RegionScalingDamageDealt(), false, false, false, false),
		REGION_SCALING_DAMAGE_TAKEN(new RegionScalingDamageTaken(), false, false, false, false),
		SKT_DAMAGE_DEALT(new SKTQuestDamageDealt(), false, false, false, false),
		SKT_DAMAGE_TAKEN(new SKTQuestDamageTaken(), false, false, false, false),
		// Vanilla
		SWEEPING_EDGE(Enchantment.SWEEPING_EDGE, "Sweeping Edge", true, false, false, false),
		KNOCKBACK(Enchantment.KNOCKBACK, "Knockback", true, false, false, false),
		LOOTING(Enchantment.LOOT_BONUS_MOBS, "Looting", true, false, false, false),
		RIPTIDE(Enchantment.RIPTIDE, "Riptide", true, false, false, false),
		PUNCH(Enchantment.ARROW_KNOCKBACK, "Punch", true, false, false, false),
		QUICK_CHARGE(Enchantment.QUICK_CHARGE, "Quick Charge", true, false, false, false),
		MULTISHOT(Enchantment.MULTISHOT, "Multishot", false, false, false, false),
		INFINITY(Enchantment.ARROW_INFINITE, "Infinity", false, false, false, false),
		EFFICIENCY(Enchantment.DIG_SPEED, "Efficiency", true, false, false, false),
		FORTUNE(Enchantment.LOOT_BONUS_BLOCKS, "Fortune", true, false, false, false),
		SILK_TOUCH(Enchantment.SILK_TOUCH, "Silk Touch", false, false, false, false),
		LUCK_OF_THE_SEA(Enchantment.LUCK, "Luck of the Sea", true, false, false, false),
		LURE(Enchantment.LURE, "Lure", true, false, false, false),
		SOUL_SPEED(Enchantment.SOUL_SPEED, "Soul Speed", true, false, false, false),
		AQUA_AFFINITY(Enchantment.WATER_WORKER, "Aqua Affinity", false, false, false, false),
		RESPIRATION(Enchantment.OXYGEN, "Respiration", true, false, false, false),
		DEPTH_STRIDER(Enchantment.DEPTH_STRIDER, "Depth Strider", true, false, false, false),
		// Protections
		MELEE_PROTECTION(new MeleeProtection(), true, false, false, true),
		PROJECTILE_PROTECTION(new ProjectileProtection(), true, false, false, true),
		BLAST_PROTECTION(new BlastProtection(), true, false, false, true),
		MAGIC_PROTECTION(new MagicProtection(), true, false, false, true),
		FIRE_PROTECTION(new FireProtection(), true, false, false, true),
		FEATHER_FALLING(new FeatherFalling(), true, false, false, true),
		// Defense Scaling
		SHIELDING(new Shielding(), true, false, false, true),
		INURE(new Inure(), true, false, false, true),
		POISE(new Poise(), true, false, false, true),
		STEADFAST(new Steadfast(), true, false, false, true),
		GUARD(new Guard(), true, false, false, true),
		TEMPO(new Tempo(), true, false, false, true),
		REFLEXES(new Reflexes(), true, false, false, true),
		ETHEREAL(new Ethereal(), true, false, false, true),
		EVASION(new Evasion(), true, false, false, true),
		CLOAKED(new Cloaked(), true, false, false, true),
		ADAPTABILITY(new Adaptability(), false, false, false, true),
		// Custom Enchants
		ABYSSAL(new Abyssal(), true, false, false, true),
		ADRENALINE(new Adrenaline(), true, false, false, true),
		APTITUDE(new Aptitude(), true, false, false, true),
		ARCANE_THRUST(new ArcaneThrust(), true, false, false, true),
		ASHES_OF_ETERNITY(new AshesOfEternity(), false, false, false, true),
		BLEEDING(new Bleeding(), true, false, false, true),
		BROOMSTICK(new Broomstick(), false, false, false, false),
		CHAOTIC(new Chaotic(), true, false, false, true),
		DARKSIGHT(new Darksight(), false, false, false, false),
		DECAY(new Decay(), true, false, false, true),
		DUELIST(new Duelist(), true, false, false, true),
		ERUPTION(new Eruption(), true, false, false, true),
		EXCAVATOR(new Excavator(), false, false, false, true),
		EXPLODING(new Explosive(), true, false, false, true),
		ICE_ASPECT(new IceAspect(), true, false, false, true),
		FIRE_ASPECT(new FireAspect(), true, false, false, true),
		THUNDER_ASPECT(new ThunderAspect(), true, false, false, true),
		WIND_ASPECT(new WindAspect(), true, false, false, true),
		EARTH_ASPECT(new EarthAspect(), true, false, false, true),
		FIRST_STRIKE(new FirstStrike(), true, false, false, true),
		GILLS(new Gills(), false, false, false, false),
		HEX_EATER(new HexEater(), true, false, false, true),
		INFERNO(new Inferno(), true, false, false, true),
		INTOXICATING_WARMTH(new IntoxicatingWarmth(), false, false, false, false),
		INSTANT_DRINK(new InstantDrink(), false, false, false, false),
		INTUITION(new Intuition(), false, false, false, true),
		JUNGLES_NOURISHMENT(new JunglesNourishment(), false, false, false, false),
		LIFE_DRAIN(new LifeDrain(), true, false, false, true),
		LIQUID_COURAGE(new LiquidCourage(), false, false, false, false),
		MULTILOAD(new Multiload(), true, false, false, true),
		MULTITOOL(new Multitool(), true, false, false, false),
		RADIANT(new Radiant(), false, false, false, false),
		REGENERATION(new Regeneration(), true, false, false, true),
		POINT_BLANK(new PointBlank(), true, false, false, true),
		PIERCING(new Piercing(), true, false, false, true),
		WORLDLY_PROTECTION(new WorldlyProtection(), true, false, false, false),
		QUAKE(new Quake(), true, false, false, true),
		RAGE_OF_THE_KETER(new RageOfTheKeter(), false, false, false, false),
		RECOIL(new Recoil(), true, false, false, false),
		REGICIDE(new Regicide(), true, false, false, true),
		RESURRECTION(new Resurrection(), false, false, false, false),
		RETRIEVAL(new Retrieval(), true, false, false, true),
		SAPPER(new Sapper(), true, false, false, true),
		SECOND_WIND(new SecondWind(), true, false, false, true),
		SLAYER(new Slayer(), true, false, false, true),
		SMITE(new Smite(), true, false, false, true),
		SNIPER(new Sniper(), true, false, false, true),
		STAMINA(new Stamina(), true, false, false, true),
		STARVATION(new Starvation(), false, true, false, false),
		SUSTENANCE(new Sustenance(), true, false, false, true),
		WEIGHTLESS(new Weightless(), false, false, false, false),
		TEMPORAL_BENDER(new TemporalBender(), false, false, false, false),
		THROWING_KNIFE(new ThrowingKnife(), false, false, false, false),
		TRIAGE(new Triage(), true, false, false, true),
		TRIVIUM(new Trivium(), true, false, false, true),

		// Curses
		CURSE_OF_ANEMIA(new CurseOfAnemia(), true, true, false, false),
		CURSE_OF_BINDING(Enchantment.BINDING_CURSE, "Curse of Binding", false, true, false, false),
		CURSE_OF_VANISHING(Enchantment.VANISHING_CURSE, "Curse of Vanishing", false, true, false, false),
		CURSE_OF_INSTABILITY(new CurseOfInstability(), false, true, false, false),
		CURSE_OF_IRREPARIBILITY(new CurseOfIrreparability(), false, true, false, false),
		CURSE_OF_CRIPPLING(new CurseOfCrippling(), true, true, false, false),
		CURSE_OF_CORRUPTION(new CurseOfCorruption(), false, true, false, false),
		CURSE_OF_EPHEMERALITY(new CurseOfEphemerality(), false, true, true, false),
		CURSE_OF_SHRAPNEL(new CurseOfShrapnel(), true, true, false, false),
		TWO_HANDED(new TwoHanded(), false, true, false, false),
		INEPTITUDE(new Ineptitude(), true, true, false, false),
		MELEE_FRAGILITY(new MeleeFragility(), true, true, false, false),
		BLAST_FRAGILITY(new BlastFragility(), true, true, false, false),
		PROJECTILE_FRAGILITY(new ProjectileFragility(), true, true, false, false),
		MAGIC_FRAGILITY(new MagicFragility(), true, true, false, false),
		FIRE_FRAGILITY(new FireFragility(), true, true, false, false),
		FALL_FRAGILITY(new FallFragility(), true, true, false, false),
		// Durability
		UNBREAKING(Enchantment.DURABILITY, "Unbreaking", true, false, false, false),
		UNBREAKABLE(null, "Unbreakable", false, false, false, false),
		MENDING(Enchantment.MENDING, "Mending", false, false, false, false),
		// Cosmetic Item Enchants
		BAAING(new Baaing(), false, false, true, false),
		CLUCKING(new Clucking(), false, false, true, false),
		DIVINE_AURA(new DivineAura(), false, false, false, false),
		OINKING(new Oinking(), false, false, true, false),
		MATERIAL(new MaterialEnch(), false, false, false, false),
		// Item Tags
		MAGIC_WAND(new MagicWandEnch(), false, false, false, false),
		ALCHEMICAL_ALEMBIC(null, "Alchemical Utensil", false, false, false, false),
		//Random Stuff
		PESTILENCE_TESSERACT(new PestilenceTesseract(), false, false, true, false),
		// Hidden enchantments that affect display
		HIDE_ATTRIBUTES(new HideAttributes(), false, false, false, false),
		HIDE_ENCHANTS(new HideEnchants(), false, false, false, false),
		HIDE_INFO(new HideInfo(), false, false, false, false),
		NO_GLINT(new NoGlint(), false, false, false, false),
		// Crit Calcs (defaults to value of 1, always active. DO NOT GIVE TO PLAYERS VIA ENCHANT)
		ANTI_CRIT_SCALING(new AntiCritScaling(), false, false, false, false),
		CRIT_SCALING(new CritScaling(), false, false, false, false),
		STRENGTH_APPLY(new StrengthApply(), false, false, false, false),
		STRENGTH_CANCEL(new StrengthCancel(), false, false, false, false);

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
		final boolean mIsRegionScaled;

		EnchantmentType(@Nullable Enchantment enchantment, String name, boolean usesLevels, boolean isCurse, boolean isSpawnable, boolean isRegionScaled) {
			mEnchantment = enchantment;
			mItemStat = null;
			mName = name;
			mUsesLevels = usesLevels;
			mIsCurse = isCurse;
			mIsSpawnable = isSpawnable;
			mIsRegionScaled = isRegionScaled;
		}

		EnchantmentType(ItemStat itemStat, boolean usesLevels, boolean isCurse, boolean isSpawnable, boolean isRegionScaled) {
			mEnchantment = null;
			mItemStat = itemStat;
			mName = itemStat.getName();
			mUsesLevels = usesLevels;
			mIsCurse = isCurse;
			mIsSpawnable = isSpawnable;
			mIsRegionScaled = isRegionScaled;
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

		public boolean isRegionScaled() {
			return mIsRegionScaled;
		}

		public boolean isHidden() {
			return this == MAINHAND_OFFHAND_DISABLE
				       || this == OFFHAND_MAINHAND_DISABLE
				       || this == HIDE_ATTRIBUTES
				       || this == HIDE_ENCHANTS
				       || this == HIDE_INFO
				       || this == NO_GLINT;
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
		ACUMEN(new Acumen(), "", true, false, false, true),
		ANTIGRAV(new AntiGrav(), "", true, false, false, true),
		ARDOR(new Ardor(), "", true, false, false, true),
		AURA(new Aura(), "", true, false, false, true),
		CARAPACE(new Carapace(), "", true, false, false, true),
		CHOLER(new Choler(), "", true, false, false, true),
		DECAPITATION(new Decapitation(), "", true, false, false, true),
		EMPOWERED(new Empowered(), "", true, false, false, true),
		ENERGIZE(new Energize(), "", true, false, false, true),
		EPOCH(new Epoch(), "", true, false, false, true),
		EXECUTION(new Execution(), "", true, false, false, true),
		EXPEDITE(new Expedite(), "", true, false, false, true),
		FUELED(new Fueled(), "", true, false, false, true),
		FOCUS(new Focus(), "", true, false, false, true),
		GALVANIC(new Galvanic(), "", true, false, false, true),
		GRACE(new Grace(), "", true, false, false, true),
		MITOSIS(new Mitosis(), "", true, false, false, true),
		NATANT(new Natant(), "", true, false, false, true),
		NUTRIMENT(new Nutriment(), "", true, false, false, true),
		PENNATE(new Pennate(), "", true, false, false, true),
		PERSPICACITY(new Perspicacity(), "", true, false, false, true),
		QUENCH(new Quench(), "", true, false, false, true),
		REFLECTION(new Reflection(), "", true, false, false, true),
		REFRESH(new Refresh(), "", true, false, false, true),
		SOOTHING(new Soothing(), "", true, false, false, true),
		TENACITY(new Tenacity(), "", true, false, false, true),
		UNDERSTANDING(new Understanding(), "", true, false, false, true),
		UNYIELDING(new Unyielding(), "", true, false, false, true),
		USURPER(new Usurper(), "", true, false, false, true),
		VENGEFUL(new Vengeful(), "", true, false, false, true),
		VIGOR(new Vigor(), "", true, false, false, true),
		VITALITY(new Vitality(), "", true, false, false, true),
		// Other Added Tags
		LOCKED(new Locked(), "", false, false, false, false),
		BARKING(new Barking(), "", true, true, false, false),
		DEBARKING(new Debarking(), "", false, false, false, false),
		HOPE(new Hope(), "Hoped", false, true, false, false),
		COLOSSAL(new Colossal(), "Reinforced", false, false, false, false),
		PHYLACTERY(new Phylactery(), "Embalmed", false, false, false, false),
		SOULBOUND(new Soulbound(), "Soulbound", false, false, false, false),
		FESTIVE(new Festive(), "Decorated", false, true, false, false),
		GILDED(new Gilded(), "Gilded", false, true, false, false),
		SHATTERED(new Shattered(), "", true, false, false, false),
		// Stat tracking stuff
		STAT_TRACK(new StatTrack(), "Tracked", false, false, false, false),
		STAT_TRACK_KILLS(new StatTrackKills(), "", true, false, true, false),
		STAT_TRACK_DAMAGE(new StatTrackDamage(), "", true, false, true, false),
		STAT_TRACK_MELEE(new StatTrackMelee(), "", true, false, true, false),
		STAT_TRACK_PROJECTILE(new StatTrackProjectile(), "", true, false, true, false),
		STAT_TRACK_MAGIC(new StatTrackMagic(), "", true, false, true, false),
		STAT_TRACK_BOSS(new StatTrackBoss(), "", true, false, true, false),
		STAT_TRACK_SPAWNER(new StatTrackSpawners(), "", true, false, true, false),
		STAT_TRACK_CONSUMED(new StatTrackConsumed(), "", true, false, true, false),
		STAT_TRACK_BLOCKS(new StatTrackBlocks(), "", true, false, true, false),
		STAT_TRACK_RIPTIDE(new StatTrackRiptide(), "", true, false, true, false),
		STAT_TRACK_BLOCKS_BROKEN(new StatTrackBlocksBroken(), "", true, false, true, false),
		STAT_TRACK_SHIELD_BLOCKED(new StatTrackShielded(), "", true, false, true, false),
		STAT_TRACK_DEATH(new StatTrackDeath(), "", true, false, true, false),
		STAT_TRACK_REPAIR(new StatTrackRepair(), "", true, false, true, false),
		STAT_TRACK_CONVERT(new StatTrackConvert(), "", true, false, true, false),
		STAT_TRACK_DAMAGE_TAKEN(new StatTrackDamageTaken(), "", true, false, true, false);

		public static final Map<String, InfusionType> REVERSE_MAPPINGS = Arrays.stream(InfusionType.values())
			                                                                 .collect(Collectors.toUnmodifiableMap(type -> type.getName().replace(" ", ""), type -> type));

		public static final Set<InfusionType> STAT_TRACK_OPTIONS = Arrays.stream(InfusionType.values())
			                                                           .filter(type -> type.mIsStatTrackOption)
			                                                           .collect(Collectors.toUnmodifiableSet());

		public static final Set<InfusionType> SPAWNABLE_INFUSIONS = Arrays.stream(InfusionType.values())
			                                                            .filter(type -> type.mIsSpawnable)
			                                                            .collect(Collectors.toUnmodifiableSet());

		public static final ImmutableSet<InfusionType> DELVE_INFUSIONS = ImmutableSet.of(
			ARDOR, AURA, CARAPACE, CHOLER, EMPOWERED, EPOCH, EXECUTION, EXPEDITE, MITOSIS, NATANT,
			NUTRIMENT, PENNATE, REFLECTION, UNDERSTANDING, UNYIELDING, USURPER, VENGEFUL,
			REFRESH, SOOTHING, QUENCH, GRACE, GALVANIC, DECAPITATION, FUELED
		);

		static final String KEY = "Infusions";

		final ItemStat mItemStat;
		final String mName;
		final String mMessage;
		final boolean mIsSpawnable;
		final boolean mHasLevels;
		final boolean mIsStatTrackOption;
		final boolean mIsRegionScaled;

		InfusionType(ItemStat itemStat, String message, boolean hasLevels, boolean isSpawnable, boolean isStatTrackOption, boolean isRegionScaled) {
			mItemStat = itemStat;
			mName = itemStat.getName();
			mIsSpawnable = isSpawnable;
			mHasLevels = hasLevels;
			mMessage = message;
			mIsStatTrackOption = isStatTrackOption;
			mIsRegionScaled = isRegionScaled;
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

		public boolean isRegionScaled() {
			return mIsRegionScaled;
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

		public boolean isHidden() {
			return this == SHATTERED;
		}

		public static @Nullable InfusionType getInfusionType(String name) {
			return REVERSE_MAPPINGS.get(name.replace(" ", ""));
		}
	}

	public enum AttributeType {
		ARMOR(new Armor(), false, true),
		AGILITY(new Agility(), false, true),
		MAX_HEALTH(Attribute.GENERIC_MAX_HEALTH, "Max Health", false, false),
		ATTACK_DAMAGE_ADD(new AttackDamageAdd(), true, false),
		ATTACK_DAMAGE_MULTIPLY(new AttackDamageMultiply(), true, false),
		ATTACK_SPEED(Attribute.GENERIC_ATTACK_SPEED, "Attack Speed", false, false),
		PROJECTILE_DAMAGE_ADD(new ProjectileDamageAdd(), true, false),
		PROJECTILE_DAMAGE_MULTIPLY(new ProjectileDamageMultiply(), true, false),
		PROJECTILE_SPEED(new ProjectileSpeed(), true, false),
		THROW_RATE(new ThrowRate(), false, false),
		SPELL_DAMAGE(new SpellPower(), false, false),
		MAGIC_DAMAGE_ADD(new MagicDamageAdd(), true, false),
		MAGIC_DAMAGE_MULTIPLY(new MagicDamageMultiply(), true, false),
		SPEED(Attribute.GENERIC_MOVEMENT_SPEED, "Speed", false, false),
		KNOCKBACK_RESISTANCE(Attribute.GENERIC_KNOCKBACK_RESISTANCE, "Knockback Resistance", false, false),
		THORNS(new ThornsDamage(), true, true);

		static final Map<String, AttributeType> REVERSE_MAPPINGS = Arrays.stream(AttributeType.values())
			                                                           .collect(Collectors.toUnmodifiableMap(AttributeType::getCodeName, type -> type));

		public static final ImmutableList<AttributeType> MAINHAND_ATTRIBUTE_TYPES = ImmutableList.of(
			ATTACK_DAMAGE_ADD,
			ATTACK_SPEED,
			PROJECTILE_DAMAGE_ADD,
			PROJECTILE_SPEED,
			THROW_RATE
		);

		static final String KEY = "Attributes";

		final @Nullable Attribute mAttribute;
		final @Nullable ItemStat mItemStat;
		final String mName;
		final String mCodeName;
		final boolean mIsRegionScaled;
		final boolean mIsMainhandRegionScaled;

		AttributeType(Attribute attribute, String name, boolean isRegionScaled, boolean isMainhandRegionScaled) {
			mAttribute = attribute;
			mItemStat = null;
			mName = name;
			mCodeName = mName.replace(" ", "");
			mIsRegionScaled = isRegionScaled;
			mIsMainhandRegionScaled = isMainhandRegionScaled;
		}

		AttributeType(ItemStat itemStat, boolean isRegionScaled, boolean isMainhandRegionScaled) {
			this(itemStat, itemStat.getName().replace(" ", ""), isRegionScaled, isMainhandRegionScaled);
		}

		AttributeType(ItemStat itemStat, String codeName, boolean isRegionScaled, boolean isMainhandRegionScaled) {
			mAttribute = null;
			mItemStat = itemStat;
			mName = itemStat.getName();
			mCodeName = codeName;
			mIsRegionScaled = isRegionScaled;
			mIsMainhandRegionScaled = isMainhandRegionScaled;
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

		public boolean isRegionScaled() {
			return mIsRegionScaled;
		}

		public boolean isMainhandRegionScaled() {
			return mIsMainhandRegionScaled;
		}

		public static Component getDisplay(AttributeType attribute, double amount, Slot slot, Operation operation) {
			String name = attribute.getName();
			if (slot == Slot.MAINHAND && operation == Operation.ADD) {
				if (attribute == ATTACK_DAMAGE_ADD) {
					return Component.text(String.format(" %s %s", NUMBER_FORMATTER.format(amount + 1), name.replace(" Add", "")), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false);
				} else if (attribute == ATTACK_SPEED) {
					return Component.text(String.format(" %s %s", NUMBER_FORMATTER.format(amount + 4), name), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false);
				} else if (attribute == PROJECTILE_SPEED || attribute == THROW_RATE) {
					return Component.text(String.format(" %s %s", NUMBER_FORMATTER.format(amount), name), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false);
				} else if (PROJECTILE_DAMAGE_ADD.getName().equals(name)) {
					return Component.text(String.format(" %s %s", NUMBER_FORMATTER.format(amount), name.replace(" Add", "")), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false);
				}
			} else if (slot == Slot.MAINHAND && attribute == PROJECTILE_SPEED) {
				// Hack for mainhands using projectile speed multiply instead of add
				return Component.text(String.format(" %s %s", NUMBER_FORMATTER.format(amount), name), NamedTextColor.DARK_GREEN).decoration(TextDecoration.ITALIC, false);
			}

			if (attribute == ARMOR || attribute == AGILITY) {
				if (operation == Operation.ADD) {
					return Component.text(String.format("%s %s", NUMBER_CHANGE_FORMATTER.format(amount), name), amount > 0 ? TextColor.fromHexString("#33CCFF") : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
				} else {
					return Component.text(String.format("%s %s", PERCENT_CHANGE_FORMATTER.format(amount), name), amount > 0 ? TextColor.fromHexString("#33CCFF") : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
				}
			} else {
				if (operation == Operation.ADD && attribute == KNOCKBACK_RESISTANCE) {
					return Component.text(String.format("%s %s", NUMBER_CHANGE_FORMATTER.format(amount * 10), name.replace(" Add", "")), amount > 0 ? NamedTextColor.BLUE : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
				} else if (operation == Operation.ADD) {
					return Component.text(String.format("%s %s", NUMBER_CHANGE_FORMATTER.format(amount), name.replace(" Add", "")), amount > 0 ? NamedTextColor.BLUE : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
				} else if (attribute == PROJECTILE_SPEED) {
					return Component.text(String.format("%s %s", PERCENT_CHANGE_FORMATTER.format(amount), name.replace(" Multiply", "")), amount > 0 ? NamedTextColor.BLUE : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
				} else {
					return Component.text(String.format("%s %s", PERCENT_CHANGE_FORMATTER.format(amount), name.replace(" Multiply", "")), amount > 0 ? NamedTextColor.BLUE : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false);
				}
			}
		}

		public static @Nullable AttributeType getAttributeType(String name) {
			return REVERSE_MAPPINGS.get(name.replace(" ", ""));
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

	public static void applyCustomEffects(Plugin plugin, Player player, ItemStack item) {
		applyCustomEffects(plugin, player, item, true);
	}

	public static void applyCustomEffects(Plugin plugin, Player player, ItemStack item, boolean applySickness) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}

		if (player.isDead() || !player.isValid()) {
			return;
		}

		NBTCompoundList effects = getEffects(new NBTItem(item));

		// Ensure other effects don't apply
		if (item.getItemMeta() instanceof PotionMeta potionMeta) {
			if (ItemStatUtils.hasConsumeEffect(item)) {
				// If it's a custom potion, remove all effects
				potionMeta.clearCustomEffects();
				potionMeta.setBasePotionData(new PotionData(PotionType.AWKWARD));
			} else {
				// If it's a vanilla potion, remove positive effects (Ensure legacies stay unusable)
				PotionUtils.removePositiveEffects(potionMeta);
				if (PotionUtils.hasPositiveEffects(potionMeta.getBasePotionData().getType().getEffectType())) {
					// If base potion is vanilla positive potion, set to AWKWARD, otherwise keep (ensures negative effects remain)
					potionMeta.setBasePotionData(new PotionData(PotionType.AWKWARD));
				}
			}
			item.setItemMeta(potionMeta);
		}

		if (effects == null || effects.isEmpty()) {
			return;
		}

		double quenchScale = Quench.getDurationScaling(plugin, player);

		for (NBTListCompound effect : effects) {
			String type = effect.getString(EFFECT_TYPE_KEY);
			int duration = effect.getInteger(EFFECT_DURATION_KEY);
			double strength = effect.getDouble(EFFECT_STRENGTH_KEY);
			String source = effect.getString(EFFECT_SOURCE_KEY);

			int modifiedDuration = (int) (duration * quenchScale);

			EffectType effectType = EffectType.fromType(type);
			if (effectType != null) {
				if (effectType == EffectType.ABSORPTION) {
					double sicknessPenalty = 0;
					NavigableSet<Effect> sicks = plugin.mEffectManager.getEffects(player, "AbsorptionSickness");
					if (sicks != null) {
						Effect sick = sicks.last();
						sicknessPenalty = sick.getMagnitude();
					}
					EffectType.applyEffect(effectType, player, modifiedDuration, strength * (1 - sicknessPenalty), source, applySickness);
				} else if (effectType == EffectType.INSTANT_HEALTH || effectType == EffectType.VANILLA_HEAL) {
					double sicknessPenalty = 0;
					NavigableSet<Effect> sicks = plugin.mEffectManager.getEffects(player, "HealingSickness");
					if (sicks != null) {
						Effect sick = sicks.last();
						sicknessPenalty = sick.getMagnitude();
					}
					EffectType.applyEffect(effectType, player, modifiedDuration, strength * (1 - sicknessPenalty), source, applySickness);
				} else {
					EffectType.applyEffect(effectType, player, modifiedDuration, strength, source, applySickness);
				}
			}
		}
	}

	public static void changeEffectsDuration(Player player, ItemStack item, int durationChange) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}

		NBTCompoundList effects = getEffects(new NBTItem(item));

		if (effects == null || effects.isEmpty()) {
			return;
		}

		for (NBTListCompound effect : effects) {
			String type = effect.getString(EFFECT_TYPE_KEY);
			int duration = effect.getInteger(EFFECT_DURATION_KEY);
			double strength = effect.getDouble(EFFECT_STRENGTH_KEY);
			String source = effect.getString(EFFECT_SOURCE_KEY);
			EffectType effectType = EffectType.fromType(type);
			if (effectType != null) {
				EffectType.applyEffect(effectType, player, duration + durationChange, strength, source, false);
			}
		}
	}

	public static void changeEffectsDurationSplash(Player player, ItemStack item, double scale) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}

		NBTCompoundList effects = getEffects(new NBTItem(item));

		if (effects == null || effects.isEmpty()) {
			return;
		}

		for (NBTListCompound effect : effects) {
			String type = effect.getString(EFFECT_TYPE_KEY);
			int duration = effect.getInteger(EFFECT_DURATION_KEY);
			double strength = effect.getDouble(EFFECT_STRENGTH_KEY);
			String source = effect.getString(EFFECT_SOURCE_KEY);
			EffectType effectType = EffectType.fromType(type);
			if (effectType != null) {
				EffectType.applyEffect(effectType, player, (int) (duration * scale), strength, source, false);
			}
		}
	}

	public static boolean hasConsumeEffect(ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}

		NBTCompoundList effects = getEffects(new NBTItem(item));

		if (effects == null || effects.isEmpty()) {
			return false;
		}

		for (NBTListCompound effect : effects) {
			String type = effect.getString(EFFECT_TYPE_KEY);

			EffectType effectType = EffectType.fromType(type);
			if (effectType != null) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasNegativeEffect(ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}

		NBTCompoundList effects = getEffects(new NBTItem(item));

		if (effects == null || effects.isEmpty()) {
			return false;
		}

		for (NBTListCompound effect : effects) {
			String type = effect.getString(EFFECT_TYPE_KEY);

			EffectType effectType = EffectType.fromType(type);
			if (effectType != null) {
				if (!effectType.isPositive()) {
					return true;
				}
			}
		}
		return false;
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

	public static String toRomanNumerals(int value) {
		if (value == 0) {
			return "0";
		}
		Map.Entry<Integer, String> nextNumeralValue = ROMAN_NUMERAL_VALUES.floorEntry(value);
		if (value == nextNumeralValue.getKey()) {
			return nextNumeralValue.getValue();
		}

		return nextNumeralValue.getValue() + toRomanNumerals(value - nextNumeralValue.getKey());
	}

	private static Optional<NBTCompound> getCompound(@Nullable NBTCompound compound, String... path) {
		if (compound == null) {
			return Optional.empty();
		}
		for (String p : path) {
			compound = compound.getCompound(p);
			if (compound == null) {
				return Optional.empty();
			}
		}
		return Optional.of(compound);
	}

	private static Optional<NBTCompound> getCompound(@Nullable ItemStack item, String... path) {
		if (item == null || item.getType() == Material.AIR) {
			return Optional.empty();
		}
		return getCompound(new NBTItem(item), path);
	}

	private static <T extends Enum<T>> @Nullable T getEnum(NBTCompound compound, String key, Class<T> enumClass) {
		String value = compound.getString(key);
		if (value == null) {
			return null;
		}
		try {
			return Enum.valueOf(enumClass, value.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public static void editItemInfo(final ItemStack item, final Region region, final Tier tier, final Masterwork masterwork, final Location location) {
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

		if (masterwork == Masterwork.NONE) {
			monumenta.removeKey(Masterwork.KEY);
		} else {
			monumenta.setString(Masterwork.KEY, masterwork.getName());
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
		if (!lore.isEmpty()) {
			if (index < lore.size()) {
				lore.remove(index);
			} else {
				lore.remove(lore.size() - 1);
			}
		}

		if (lore.isEmpty()) {
			clearLore(item);
		}

		item.setItemMeta(nbt.getItem().getItemMeta());
	}

	public static void clearLore(final ItemStack item) {
		NBTItem nbt = new NBTItem(item);
		nbt.getCompound(MONUMENTA_KEY).removeKey(LORE_KEY);
		item.setItemMeta(nbt.getItem().getItemMeta());
		item.lore(Collections.emptyList());
	}

	public static List<Component> getLore(final ItemStack item) {
		return getLore(new NBTItem(item));
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

	public static void addCharmEffect(final ItemStack item, final int index, final Component line) {
		if (item.getType() == Material.AIR) {
			return;
		}
		NBTItem nbt = new NBTItem(item);
		NBTList<String> charmLore = nbt.addCompound(MONUMENTA_KEY).getStringList(CHARM_KEY);
		String serializedLine = MessagingUtils.toGson(line).toString();
		if (index < charmLore.size()) {
			charmLore.add(index, serializedLine);
		} else {
			charmLore.add(serializedLine);
		}

		item.setItemMeta(nbt.getItem().getItemMeta());
	}

	public static void removeCharmEffect(final ItemStack item, final int index) {
		if (item.getType() == Material.AIR) {
			return;
		}
		NBTItem nbt = new NBTItem(item);
		NBTList<String> lore = nbt.addCompound(MONUMENTA_KEY).getStringList(CHARM_KEY);
		if (lore.size() > 0 && index < lore.size()) {
			lore.remove(index);
		} else if (lore.size() > 0) {
			lore.remove(lore.size() - 1);
		}

		item.setItemMeta(nbt.getItem().getItemMeta());
	}

	public static List<Component> getCharmEffects(final ItemStack item) {
		return getCharmEffects(new NBTItem(item));
	}

	public static List<Component> getCharmEffects(final NBTItem nbt) {
		List<Component> lore = new ArrayList<>();
		for (String serializedLine : nbt.addCompound(MONUMENTA_KEY).getStringList(CHARM_KEY)) {
			lore.add(MessagingUtils.fromGson(serializedLine));
		}
		return lore;
	}

	public static List<String> getPlainCharmLore(final NBTItem nbt) {
		List<String> plainLore = new ArrayList<>();
		for (Component line : getCharmEffects(nbt)) {
			plainLore.add(MessagingUtils.plainText(line));
		}
		return plainLore;
	}

	public static void setCharmPower(final ItemStack item, final int level) {
		if (item.getType() == Material.AIR) {
			return;
		}
		NBTItem nbt = new NBTItem(item);
		NBTCompound charmPower = nbt.addCompound(MONUMENTA_KEY);
		charmPower.setInteger(CHARM_POWER_KEY, level);

		item.setItemMeta(nbt.getItem().getItemMeta());
	}

	public static void removeCharmPower(final ItemStack item) {
		if (item.getType() == Material.AIR) {
			return;
		}
		NBTItem nbt = new NBTItem(item);
		NBTCompound charmPower = nbt.getCompound(CHARM_POWER_KEY);
		if (charmPower == null) {
			return;
		}
		charmPower.removeKey(CHARM_POWER_KEY);

		item.setItemMeta(nbt.getItem().getItemMeta());
	}

	public static int getCharmPower(final ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return 0;
		}
		return getCharmPower(new NBTItem(item));
	}

	public static int getCharmPower(final NBTItem nbt) {
		NBTCompound monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return 0;
		}
		return monumenta.getInteger(CHARM_POWER_KEY);
	}

	public static @Nullable PlayerClass getCharmClass(NBTList<String> charmLore) {
		List<PlayerClass> classes = new MonumentaClasses().getClasses();

		for (String line : charmLore) {
			for (PlayerClass playerClass : classes) {
				List<AbilityInfo<?>> abilities = new ArrayList<>();
				abilities.addAll(playerClass.mAbilities);
				abilities.addAll(playerClass.mSpecOne.mAbilities);
				abilities.addAll(playerClass.mSpecTwo.mAbilities);

				List<String> abilityNames = new ArrayList<>();
				abilityNames.add(playerClass.mClassPassiveName);
				abilities.forEach(a -> abilityNames.add(a.getDisplayName()));

				for (String name : abilityNames) {
					if (line.contains(name)) {
						return playerClass;
					}
				}
				if (line.contains(playerClass.mClassPassiveName)) {
					return playerClass;
				}
			}
			// The real ability name is "Alchemist Potions", but charms don't use the "s"
			if (line.contains("Alchemist Potion")) {
				return new Alchemist();
			}
		}
		return null;
	}

	private static Component getCharmClassComponent(NBTList<String> charmLore) {
		PlayerClass playerClass = getCharmClass(charmLore);
		if (playerClass != null) {
			return Component.text(playerClass.mClassName, playerClass.mClassColor).decoration(TextDecoration.ITALIC, false);
		}
		return Component.text("Generalist", TextColor.fromHexString("#9F8F91"));
	}

	public static void addConsumeEffect(final ItemStack item, final EffectType type, final double strength, final int duration, @Nullable String source) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}

		NBTItem nbt = new NBTItem(item);
		NBTCompoundList effects = nbt.addCompound(MONUMENTA_KEY).addCompound(STOCK_KEY).getCompoundList(EffectType.KEY);
		NBTListCompound effect = effects.addCompound();
		effect.setString(EFFECT_TYPE_KEY, type.getType());
		effect.setDouble(EFFECT_STRENGTH_KEY, strength);
		effect.setInteger(EFFECT_DURATION_KEY, duration);
		if (source != null) {
			effect.setString(EFFECT_SOURCE_KEY, source);
		}

		item.setItemMeta(nbt.getItem().getItemMeta());

		generateItemStats(item);
	}

	public static void removeConsumeEffect(final ItemStack item, final EffectType type) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		NBTItem nbt = new NBTItem(item);
		NBTCompoundList effects = getEffects(nbt);

		if (effects == null || effects.isEmpty()) {
			return;
		}

		int i = 0;
		for (NBTListCompound effect : effects) {
			if (type.getType().equals(effect.getString(EFFECT_TYPE_KEY))) {
				effects.remove(i);
				break;
			}
			i++;
		}

		item.setItemMeta(nbt.getItem().getItemMeta());
		generateItemStats(item);
	}


	public static void removeConsumeEffect(final ItemStack item, final int i) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		NBTItem nbt = new NBTItem(item);
		NBTCompoundList effects = getEffects(nbt);

		if (effects == null || effects.isEmpty()) {
			return;
		}

		effects.remove(i);
		item.setItemMeta(nbt.getItem().getItemMeta());
		generateItemStats(item);
	}

	public static boolean isConsumable(final ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return false;
		}
		NBTItem nbt = new NBTItem(item);
		NBTCompoundList effects = getEffects(nbt);

		if (effects == null || effects.isEmpty()) {
			return false;
		}

		return true;
	}

	public static @Nullable NBTCompoundList getEffects(final NBTItem nbt) {
		NBTCompound monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return null;
		}

		NBTCompound stock = monumenta.getCompound(STOCK_KEY);
		if (stock == null) {
			return null;
		}

		return stock.getCompoundList(EffectType.KEY);
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

	public static boolean hasEnchantment(@Nullable ItemStack item, EnchantmentType type) {
		return getEnchantmentLevel(item, type) > 0;
	}

	public static @Nullable NBTCompound getPlayerModified(final NBTCompound nbt) {
		NBTCompound monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return null;
		}
		return monumenta.getCompound(PLAYER_MODIFIED_KEY);
	}

	public static void removePlayerModified(final NBTCompound nbt) {
		NBTCompound monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return;
		}
		monumenta.removeKey(PLAYER_MODIFIED_KEY);
		if (monumenta.getKeys().isEmpty()) {
			nbt.removeKey(MONUMENTA_KEY);
		}
	}

	public static NBTCompound addPlayerModified(final NBTCompound nbt) {
		return nbt.addCompound(MONUMENTA_KEY).addCompound(PLAYER_MODIFIED_KEY);
	}

	public static @Nullable ItemStack copyPlayerModified(final @Nullable ItemStack item, @Nullable ItemStack newItem) {
		if (ItemUtils.isNullOrAir(item) || ItemUtils.isNullOrAir(newItem)) {
			return null;
		}

		NBTItem oldNBT = new NBTItem(item);
		NBTItem newNBT = new NBTItem(newItem);
		NBTCompound playerModified = getPlayerModified(oldNBT);
		if (playerModified == null) {
			return newItem;
		}

		addPlayerModified(newNBT).mergeCompound(playerModified);
		newItem = newNBT.getItem();
		generateItemStats(newItem);

		return newItem;
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

	public static int getInfusionLevel(final @Nullable ItemStack item, final @Nullable InfusionType type) {
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

	public static @Nullable UUID getInfuser(final @Nullable ItemStack item, final @Nullable InfusionType type) {
		if (item == null || item.getType() == Material.AIR || type == null || type.getName() == null) {
			return null;
		}
		NBTItem nbt = new NBTItem(item);
		NBTCompound infusions = getInfusions(nbt);
		if (infusions == null) {
			return null;
		}
		NBTCompound infusion = infusions.getCompound(type.getName());
		if (infusion == null) {
			return null;
		}

		try {
			return UUID.fromString(infusion.getString(INFUSER_KEY));
		} catch (IllegalArgumentException e) { // bad item format
			return null;
		}
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
		if (item.getType() == Material.AIR || type == null) {
			return;
		}
		NBTItem nbt = new NBTItem(item);
		NBTCompound infusions = getInfusions(nbt);
		if (infusions == null) {
			return;
		}

		infusions.removeKey(type.getName());

		item.setItemMeta(nbt.getItem().getItemMeta());
		if (updateItem) {
			generateItemStats(item);
		}
	}

	public static boolean hasInfusion(@Nullable ItemStack item, InfusionType type) {
		return getInfusionLevel(item, type) > 0;
	}

	public static @Nullable NBTCompoundList getAttributes(final NBTItem nbt) {
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
			return ItemUtils.isShulkerBox(item.getType()) ? Region.SHULKER_BOX : Region.NONE;
		}

		ItemMeta meta = item.getItemMeta();
		if (meta.hasDisplayName()) {
			String name = MessagingUtils.plainText(Objects.requireNonNull(meta.displayName()));
			if (name.contains("Alchemist's Bag")) {
				return Region.VALLEY;
			} else if (name.contains("Experiencinator")) {
				return Region.VALLEY;
			} else if (name.contains("Crystallizer")) {
				return Region.ISLES;
			}
		}

		String regionString = monumenta.getString(Region.KEY);
		if (regionString != null && !regionString.isEmpty()) {
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
			return ItemUtils.isShulkerBox(item.getType()) ? Tier.SHULKER_BOX : Tier.NONE;
		}

		ItemMeta meta = item.getItemMeta();
		if (meta.hasDisplayName()) {
			String itemName = MessagingUtils.plainText(Objects.requireNonNull(meta.displayName()));
			if (itemName.contains("Alchemist's Bag")) {
				return Tier.RARE;
			}
		}

		String tierString = monumenta.getString(Tier.KEY);
		if (tierString != null && !tierString.isEmpty()) {
			return Tier.getTier(tierString);
		}

		if (ItemUtils.isShulkerBox(item.getType())) {
			return Tier.SHULKER_BOX;
		}

		return Tier.NONE;
	}

	public static Masterwork getMasterwork(final @Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return Masterwork.NONE;
		}
		NBTItem nbt = new NBTItem(item);
		NBTCompound monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return Masterwork.NONE;
		}

		if (getRegion(item) != Region.RING) {
			return Masterwork.NONE;
		}

		String tierString = monumenta.getString(Masterwork.KEY);
		if (tierString != null) {
			return Masterwork.getMasterwork(tierString);
		}

		return Masterwork.NONE;
	}

	public static Location getLocation(final @Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return Location.NONE;
		}
		NBTItem nbt = new NBTItem(item);
		NBTCompound monumenta = nbt.getCompound(MONUMENTA_KEY);
		if (monumenta == null) {
			return Location.NONE;
		}

		String locationString = monumenta.getString(Location.KEY);
		if (locationString != null && !locationString.isEmpty()) {
			return Location.getLocation(locationString);
		}

		return Location.NONE;
	}

	public static boolean isClean(final @Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return true;
		}
		NBTItem nbt = new NBTItem(item);

		if (!nbt.hasKey(MONUMENTA_KEY)) {
			return true;
		}
		NBTCompound monumenta = nbt.getCompound(MONUMENTA_KEY);

		return !monumenta.hasKey(DIRTY_KEY);
	}

	public static void markClean(final @Nullable ItemStack item) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		NBTItem nbt = new NBTItem(item);

		if (!nbt.hasKey(MONUMENTA_KEY)) {
			return;
		}
		NBTCompound monumenta = nbt.getCompound(MONUMENTA_KEY);

		if (!monumenta.hasKey(DIRTY_KEY)) {
			return;
		}
		monumenta.removeKey(DIRTY_KEY);
		if (monumenta.getKeys().isEmpty()) {
			nbt.removeKey(MONUMENTA_KEY);
		}

		item.setItemMeta(nbt.getItem().getItemMeta());
	}

	public static int getShulkerSlots(@Nullable ItemStack item) {
		return getCompound(item, MONUMENTA_KEY, STOCK_KEY).map(stock -> stock.getInteger(SHULKER_SLOTS_KEY)).orElse(27);
	}

	public static int getCustomInventoryItemTypesLimit(@Nullable ItemStack item) {
		return getCompound(item, MONUMENTA_KEY, STOCK_KEY).map(stock -> stock.getInteger(CUSTOM_INVENTORY_TYPES_LIMIT_KEY)).orElse(0);
	}

	public static int getCustomInventoryItemsPerTypeLimit(@Nullable ItemStack item) {
		return getCompound(item, MONUMENTA_KEY, STOCK_KEY).map(stock -> stock.getInteger(CUSTOM_INVENTORY_ITEMS_PER_TYPE_LIMIT_KEY)).orElse(0);
	}

	public static int getCustomInventoryTotalItemsLimit(@Nullable ItemStack item) {
		return getCompound(item, MONUMENTA_KEY, STOCK_KEY).map(stock -> stock.getInteger(CUSTOM_INVENTORY_TOTAL_ITEMS_LIMIT_KEY)).orElse(0);
	}

	/**
	 * Checks if an item is a quiver, i.e. is a tipped arrow with the tag Monumenta.Stock.IsQuiver set to true.
	 */
	public static boolean isQuiver(@Nullable ItemStack item) {
		if (item == null || item.getType() != Material.TIPPED_ARROW) {
			return false;
		}
		return getCompound(item, MONUMENTA_KEY, STOCK_KEY).map(stock -> stock.getBoolean(IS_QUIVER_KEY)).orElse(false);
	}

	public static boolean isArrowTransformingQuiver(@Nullable ItemStack item) {
		return isQuiver(item) && "Shaman's Quiver".equals(ItemUtils.getPlainNameIfExists(item));
	}

	public static void setArrowTransformMode(@Nullable ItemStack item, QuiverListener.ArrowTransformMode arrowTransformMode) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		addPlayerModified(new NBTItem(item, true)).setString(QUIVER_ARROW_TRANSFORM_MODE_KEY, arrowTransformMode.name().toLowerCase(Locale.ROOT));
	}

	public static QuiverListener.ArrowTransformMode getArrowTransformMode(@Nullable ItemStack item) {
		return getCompound(item, MONUMENTA_KEY, PLAYER_MODIFIED_KEY)
			       .map(playerModified -> getEnum(playerModified, QUIVER_ARROW_TRANSFORM_MODE_KEY, QuiverListener.ArrowTransformMode.class))
			       .orElse(QuiverListener.ArrowTransformMode.NONE);
	}

	public static boolean isUpgradedLimeTesseract(@Nullable ItemStack item) {
		return item != null
			       && item.getType() == Material.LIME_STAINED_GLASS
			       && "Tesseract of Knowledge (u)".equals(ItemUtils.getPlainNameIfExists(item));
	}

	public static int getCharges(@Nullable ItemStack item) {
		return getCompound(item, MONUMENTA_KEY, PLAYER_MODIFIED_KEY).map(playerModified -> playerModified.getInteger(CHARGES_KEY)).orElse(0);
	}

	public static void setCharges(@Nullable ItemStack item, int charges) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}
		addPlayerModified(new NBTItem(item, true)).setInteger(CHARGES_KEY, charges);
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

				NBTCompoundList effects = stock.getCompoundList(EffectType.KEY);
				if (effects != null && effects.isEmpty()) {
					stock.removeKey(EffectType.KEY);
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
					if (type.isHidden()) {
						continue;
					}
					NBTCompound enchantment = enchantments.getCompound(type.getName());
					if (enchantment != null) {
						if (type.isItemTypeEnchantment()) {
							tagsLater.add(type.getDisplay(enchantment.getInteger(LEVEL_KEY)));
						} else if (type.getName().equals("Mending") || type.getName().equals("Unbreaking") || type.getName().equals("Unbreakable")) {
							unbreakingTags.add(type.getDisplay(enchantment.getInteger(LEVEL_KEY)));
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
				if (type.isHidden()) {
					continue;
				}
				NBTCompound infusion = infusions.getCompound(type.getName());
				if (infusion != null) {
					if (type == InfusionType.STAT_TRACK) {
						statTrackLater.add(0, type.getDisplay(infusion.getInteger(LEVEL_KEY), MonumentaRedisSyncIntegration.cachedUuidToNameOrUuid(UUID.fromString(infusion.getString(INFUSER_KEY)))));
					} else if (type.mIsStatTrackOption) {
						statTrackLater.add(type.getDisplay(infusion.getInteger(LEVEL_KEY)));
					} else if (!type.getMessage().isEmpty()) {
						infusionTagsLater.add(type.getDisplay(infusion.getInteger(LEVEL_KEY), MonumentaRedisSyncIntegration.cachedUuidToNameOrUuid(UUID.fromString(infusion.getString(INFUSER_KEY)))));
					} else {
						lore.add(type.getDisplay(infusion.getInteger(LEVEL_KEY)));
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
				Masterwork masterwork = Masterwork.getMasterwork(monumenta.getString(Masterwork.KEY));
				Tier tier = Tier.getTier(monumenta.getString(Tier.KEY));
				if (region != null) {
					// For R3 items, set tier to match masterwork level
					if (region == Region.RING) {
						if (masterwork != null && masterwork != Masterwork.ERROR && masterwork != Masterwork.NONE) {
							switch (Objects.requireNonNull(masterwork)) {
								case ZERO:
								case I:
								case II:
								case III:
									tier = Tier.RARE;
									break;
								case IV:
								case V:
									tier = Tier.ARTIFACT;
									break;
								case VI:
									tier = Tier.EPIC;
									break;
								case VIIA:
								case VIIB:
								case VIIC:
									tier = Tier.LEGENDARY;
									break;
								default:
									break;
							}
							monumenta.setString(Tier.KEY, tier.getName());
						}
					}
					if (tier != null && tier != Tier.NONE) {
						lore.add(region.getDisplay().append(tier.getDisplay()));
					}
				}

				if (isCharm(item)) {
					int charmPower = getCharmPower(item);
					if (charmPower > 0) {
						String starString = "";
						for (int i = 0; i < charmPower; i++) {
							starString += "★";
						}
						lore.add(Component.text("Charm Power : ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false).append(Component.text(starString, TextColor.fromHexString("#FFFA75")).decoration(TextDecoration.ITALIC, false))
							         .append(Component.text(" - ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)).append(getCharmClassComponent(monumenta.getStringList(CHARM_KEY))));
					}
				}

				if (masterwork != null && masterwork != Masterwork.NONE) {
					lore.add(Component.text("Masterwork : ", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false).append(masterwork.getDisplay()));
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

		if (isArrowTransformingQuiver(item)) {
			QuiverListener.ArrowTransformMode transformMode = getArrowTransformMode(item);
			if (transformMode == QuiverListener.ArrowTransformMode.NONE) {
				lore.add(Component.text("Arrow transformation ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
					         .append(Component.text("disabled", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
			} else {
				lore.add(Component.text("Transforms arrows to ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
					         .append(Component.text(transformMode.getArrowName(), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)));
			}
		}

		CustomContainerItemManager.generateDescription(item, monumenta, lore::add);

		if (isUpgradedLimeTesseract(item)) {
			lore.add(Component.text("Stored anvils: ", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
				         .append(Component.text(getCharges(item), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false)));
		}

		int shatterLevel = ItemStatUtils.getInfusionLevel(item, InfusionType.SHATTERED);
		if (shatterLevel > 0) {
			TextColor color = TextColor.color(155 + (int) (100.0 * (shatterLevel - 1) / (Shattered.MAX_LEVEL - 1)), 0, 0);
			lore.add(Component.text("* SHATTERED - " + toRomanNumerals(shatterLevel) + " *", color, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text("Return to your grave to remove one level", color).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text("of Shattered, or use an anvil on this item.", color).decoration(TextDecoration.ITALIC, false));
		}

		NBTCompoundList effects = getEffects(nbt);
		if (effects != null && !effects.isEmpty()) {

			lore.add(Component.empty());
			lore.add(Component.text("When Consumed:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

			for (NBTListCompound effect : effects) {
				String type = effect.getString(EFFECT_TYPE_KEY);
				int duration = effect.getInteger(EFFECT_DURATION_KEY);
				double strength = effect.getDouble(EFFECT_STRENGTH_KEY);

				EffectType effectType = EffectType.fromType(type);
				if (effectType != null) {
					Component comp = EffectType.getComponent(effectType, strength, duration);
					if (!lore.contains(comp)) {
						lore.add(comp);
					}
				}
			}
		}

		NBTCompoundList attributes = getAttributes(nbt);
		if (attributes != null
			    && getEnchantmentLevel(item, EnchantmentType.HIDE_ATTRIBUTES) == 0) {
			EnumMap<Slot, EnumMap<AttributeType, List<NBTListCompound>>> attributesBySlots = new EnumMap<>(Slot.class);
			for (NBTListCompound attribute : attributes) {
				Slot slot = Slot.getSlot(attribute.getString(Slot.KEY));
				AttributeType attributeType = AttributeType.getAttributeType(attribute.getString(ATTRIBUTE_NAME_KEY));
				attributesBySlots.computeIfAbsent(slot, key -> new EnumMap<>(AttributeType.class))
					.computeIfAbsent(attributeType, key -> new ArrayList<>())
					.add(attribute);
			}

			for (Slot slot : Slot.values()) {
				EnumMap<AttributeType, List<NBTListCompound>> attributesBySlot = attributesBySlots.get(slot);
				if (attributesBySlot == null || attributesBySlot.isEmpty()) {
					continue;
				}

				lore.add(Component.empty());
				lore.add(slot.getDisplay());

				// If mainhand, display certain attributes differently (attack and projectile related ones), and also show them before other attributes
				if (slot == Slot.MAINHAND) {
					boolean needsAttackSpeed = false;
					for (AttributeType attributeType : AttributeType.MAINHAND_ATTRIBUTE_TYPES) {
						List<NBTListCompound> attributesByType = attributesBySlot.get(attributeType);
						if (attributesByType != null) {
							for (NBTListCompound attribute : attributesByType) {
								Operation operation = Operation.getOperation(attribute.getString(Operation.KEY));
								if (operation == null
									    || (operation != Operation.ADD && attributeType != AttributeType.PROJECTILE_SPEED)) {
									continue;
								}
								lore.add(AttributeType.getDisplay(attributeType, attribute.getDouble(AMOUNT_KEY), slot, operation));
								if (attributeType == AttributeType.ATTACK_DAMAGE_ADD) {
									needsAttackSpeed = true;
								} else if (attributeType == AttributeType.ATTACK_SPEED) {
									needsAttackSpeed = false;
								}
							}
						}
						// show default attack speed if an item has attack damage, but no attack speed attribute
						if (needsAttackSpeed && attributeType == AttributeType.ATTACK_SPEED) {
							lore.add(AttributeType.getDisplay(AttributeType.ATTACK_SPEED, 0, slot, Operation.ADD));
						}
					}
				}

				for (AttributeType type : AttributeType.values()) {
					List<NBTListCompound> attributesByType = attributesBySlot.get(type);
					if (attributesByType == null) {
						continue;
					}
					for (Operation operation : Operation.values()) {
						if (slot == Slot.MAINHAND && AttributeType.MAINHAND_ATTRIBUTE_TYPES.contains(type) && (operation == Operation.ADD || type == AttributeType.PROJECTILE_SPEED)) {
							continue; // handled above
						}
						for (NBTListCompound attribute : attributesByType) {
							if (Operation.getOperation(attribute.getString(Operation.KEY)) == operation) {
								lore.add(AttributeType.getDisplay(type, attribute.getDouble(AMOUNT_KEY), slot, operation));
								break;
							}
						}
					}
				}
			}
		}

		NBTList<String> charmLore = monumenta.getStringList(CHARM_KEY);
		if (charmLore != null && isCharm(item)) {
			lore.add(Component.empty());
			lore.add(Component.text("When in Charm Slot:", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			for (String serializedLine : charmLore) {
				Component lineAdd = MessagingUtils.fromGson(serializedLine);
				lore.add(lineAdd);
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
		String name = item.getType().name();
		if (name.contains("POTION") || name.contains("PATTERN") || name.contains("SHIELD")) {
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

		Enchantment placeholder = ItemUtils.isSomeBow(item) ? Enchantment.WATER_WORKER : Enchantment.ARROW_DAMAGE;
		if (getEnchantmentLevel(item, EnchantmentType.NO_GLINT) > 0) {
			meta.removeEnchant(placeholder);
		} else {
			meta.addEnchant(placeholder, 1, true);
		}

		if (meta instanceof PotionMeta potionMeta) {
			potionMeta.clearCustomEffects();
			potionMeta.setBasePotionData(new PotionData(PotionType.AWKWARD));
			item.setItemMeta(potionMeta);
		} else {
			item.setItemMeta(meta);
		}

		ItemUtils.setPlainLore(item);
	}

	public static boolean isMaterial(@Nullable ItemStack item) {
		return item != null && getEnchantmentLevel(item, EnchantmentType.MATERIAL) > 0;
	}

	public static boolean isCharm(@Nullable ItemStack item) {
		Tier tier = getTier(item);
		if (tier == Tier.CHARM || tier == Tier.RARE_CHARM || tier == Tier.EPIC_CHARM) {
			return true;
		}
		return false;
	}

	// Returns true if the item has mainhand attack damage OR doesn't have mainhand projectile damage (i.e. any ranged weapon that is not also a melee weapon)
	public static boolean isNotExclusivelyRanged(@Nullable ItemStack item) {
		return item != null && (getAttributeAmount(item, AttributeType.ATTACK_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND) > 0 || getAttributeAmount(item, AttributeType.PROJECTILE_DAMAGE_ADD, Operation.ADD, Slot.MAINHAND) == 0);
	}
}
