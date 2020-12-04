package com.playmonumenta.plugins.enchantments;

import javax.annotation.Nullable;

import org.bukkit.ChatColor;

import com.playmonumenta.plugins.enchantments.evasions.AbilityEvasion;
import com.playmonumenta.plugins.enchantments.evasions.Evasion;
import com.playmonumenta.plugins.enchantments.evasions.MeleeEvasion;
import com.playmonumenta.plugins.enchantments.infusions.Acumen;
import com.playmonumenta.plugins.enchantments.infusions.Focus;
import com.playmonumenta.plugins.enchantments.infusions.Perspicacity;
import com.playmonumenta.plugins.enchantments.infusions.Tenacity;
import com.playmonumenta.plugins.enchantments.infusions.Vigor;
import com.playmonumenta.plugins.enchantments.infusions.Vitality;

public enum Enchantment {
	// armor upgrades : they should not be present in the item index, thus, unused here
	TENACITY(new Tenacity()),
	VITALITY(new Vitality()),
	VIGOR(new Vigor()),
	FOCUS(new Focus()),
	ACUMEN(new Acumen()),
	PERSPICACITY(new Perspicacity()),

	// on hit damage
	SHARPNESS(ChatColor.GRAY + "Sharpness", org.bukkit.enchantments.Enchantment.DAMAGE_ALL),
	SMITE(ChatColor.GRAY + "Smite", org.bukkit.enchantments.Enchantment.DAMAGE_UNDEAD),
	BANE_ARTHROPODS(ChatColor.GRAY + "Bane of Arthropods", org.bukkit.enchantments.Enchantment.DAMAGE_ARTHROPODS),
	SLAYER(new Slayer()),
	DUELIST(new Duelist()),
	REGICIDE(new Regicide()),
	HEX_EATER(new HexEater()),
	CHAOTIC(new Chaotic()),
	POINT_BLANK(new PointBlank()),
	SNIPER(new Sniper()),
	IMPALING(ChatColor.GRAY + "Impaling", org.bukkit.enchantments.Enchantment.IMPALING),
	SWEEPING_EDGE(ChatColor.GRAY + "Sweeping Edge", org.bukkit.enchantments.Enchantment.SWEEPING_EDGE),
	ARCANE_THRUST(new ArcaneThrust()),

	// on hit debuffs
	FLAME(ChatColor.GRAY + "Flame", org.bukkit.enchantments.Enchantment.ARROW_FIRE),
	FROST(new Frost()),
	FIRE_ASPECT(ChatColor.GRAY + "Fire Aspect", org.bukkit.enchantments.Enchantment.FIRE_ASPECT),
	ICE_ASPECT(new IceAspect()),
	THUNDER_ASPECT(new Thunder()),
	INFERNO(new Inferno()),
	CHANNELING(ChatColor.GRAY + "Channeling", org.bukkit.enchantments.Enchantment.CHANNELING),
	DECAY(new Decay()),
	EXORCISM(ChatColor.GRAY + "Exorcism"),
	SPARK(new Spark()),

	// passives
	PUNCH(ChatColor.GRAY + "Punch", org.bukkit.enchantments.Enchantment.ARROW_KNOCKBACK),
	DARKSIGHT(new Darksight()),
	INFINITY(ChatColor.GRAY + "Infinity", org.bukkit.enchantments.Enchantment.ARROW_INFINITE),
	INTUITION(new Intuition()),
	LIFE_DRAIN(new LifeDrain()),
	SUSTENANCE(new Sustenance()),
	GILLS(new Gills()),
	RESPIRATION(ChatColor.GRAY + "Respiration", org.bukkit.enchantments.Enchantment.OXYGEN),
	AQUA_AFFINITY(ChatColor.GRAY + "Aqua Affinity", org.bukkit.enchantments.Enchantment.WATER_WORKER),
	REGENERATION(ChatColor.GRAY + "Regeneration", new Regeneration()),
	MAINHAND_REGENERATION(ChatColor.GRAY + "Mainhand Regeneration", new Regeneration()),
	FISHING_LUCK(ChatColor.GRAY + "Luck of the Sea", org.bukkit.enchantments.Enchantment.LUCK),
	FISHING_LURE(ChatColor.GRAY + "Lure", org.bukkit.enchantments.Enchantment.LURE),
	FORTUNE(ChatColor.GRAY + "Fortune", org.bukkit.enchantments.Enchantment.LOOT_BONUS_BLOCKS),
	LOOTING(ChatColor.GRAY + "Looting", org.bukkit.enchantments.Enchantment.LOOT_BONUS_MOBS),
	SILK_TOUCH(ChatColor.GRAY + "Silk Touch", org.bukkit.enchantments.Enchantment.SILK_TOUCH),
	DEPTH_STRIDER(ChatColor.GRAY + "Depth Strider", org.bukkit.enchantments.Enchantment.DEPTH_STRIDER),
	EFFICIENCY(ChatColor.GRAY + "Efficiency", org.bukkit.enchantments.Enchantment.DIG_SPEED),
	FROST_WALKER(ChatColor.GRAY + "Frost Walker", org.bukkit.enchantments.Enchantment.FROST_WALKER),
	KNOCKBACK(ChatColor.GRAY + "Knockback", org.bukkit.enchantments.Enchantment.KNOCKBACK),
	LOYALTY(ChatColor.GRAY + "Loyalty", org.bukkit.enchantments.Enchantment.LOYALTY),
	THORNS(ChatColor.GRAY + "Thorns", org.bukkit.enchantments.Enchantment.THORNS),
	ADRENALINE(new Adrenaline()),
	SAPPER(new Sapper()),
	RECOIL(new Recoil()),
	ERUPTION(new Eruption()),

	// armor protections
	PROTECTION(ChatColor.GRAY + "Protection", org.bukkit.enchantments.Enchantment.PROTECTION_ENVIRONMENTAL),
	BLAST_PROTECTION(ChatColor.GRAY + "Blast Protection", org.bukkit.enchantments.Enchantment.PROTECTION_EXPLOSIONS),
	FEATHER_FALLING(ChatColor.GRAY + "Feather Falling", org.bukkit.enchantments.Enchantment.PROTECTION_FALL),
	FIRE_PROTECTION(ChatColor.GRAY + "Fire Protection", org.bukkit.enchantments.Enchantment.PROTECTION_FIRE),
	PROJECTILE_PROTECTION(ChatColor.GRAY + "Projectile Protection", org.bukkit.enchantments.Enchantment.PROTECTION_PROJECTILE),
	EVASION(new Evasion()),
	MELEE_EVASION(new MeleeEvasion()),
	ABILITY_EVASION(new AbilityEvasion()),
	RESURRECTION(new Resurrection()),
	VOID_TETHER(new VoidTether()),
	ASHES_OF_ETERNITY(new AshesOfEternity()),

	// visuals
	CLUCKING(new Clucking()),
	OINKING(new Oinking()),
	BAAING(new Baaing()),
	BARKING(new Barking()),
	ETHERAL(new Ethereal()),
	STYLISH(new Stylish()),
	COLORFUL(new Colorful()),
	FESTIVE(new Festive()),
	DIVINE_AURA(new DivineAura()),
	GILDED(new Gilded()),
	RADIANT(new Radiant()),
	CLOAKING(new Cloaking()),

	// active effects
	THROWING_KNIFE(new ThrowingKnife()),
	INSTANT_DRINK(new InstantDrink()),
	MULTITOOL(new Multitool()),
	RIPTIDE(ChatColor.GRAY + "Riptide", org.bukkit.enchantments.Enchantment.RIPTIDE),
	JUNGLE_NOURISHMENT(new JunglesNourishment()),

	// durability
	UNBREAKING(ChatColor.GRAY + "Unbreaking", org.bukkit.enchantments.Enchantment.DURABILITY),
	MENDING(ChatColor.GRAY + "Mending", org.bukkit.enchantments.Enchantment.MENDING),
	HOPELESS(new Hopeless()),
	HOPE(new Hope()),

	// curses
	CURSE_CORRUPTION(new CurseOfCorruption()),
	CURSE_CRIPPLING(new CurseOfCrippling()),
	CURSE_IRREPARABLE(ChatColor.RED + "Curse of Irreparability"),
	CURSE_EPHEMERALITY(new CurseOfEphemerality()),
	CURSE_SHRAPNEL(new CurseOfShrapnel()),
	CURSE_BINDING(ChatColor.RED + "Curse of Binding", org.bukkit.enchantments.Enchantment.BINDING_CURSE),
	CURSE_VANISHING(ChatColor.RED + "Curse of Vanishing", org.bukkit.enchantments.Enchantment.VANISHING_CURSE),
	CURSE_ANEMIA(new CurseOfAnemia()),
	TWO_HANDED(new TwoHanded()),
	;

	String mReadableString;
	BaseEnchantment mEnchantClass;
	org.bukkit.enchantments.Enchantment mBukkitEnchant;

	Enchantment(String s) {
		this.mEnchantClass = null;
		this.mBukkitEnchant = null;
		this.mReadableString = s;
	}

	Enchantment(String s, BaseEnchantment enchantClass) {
		this.mEnchantClass = enchantClass;
		this.mReadableString = s;
		this.mBukkitEnchant = null;
	}

	Enchantment(BaseEnchantment enchantClass) {
		this.mEnchantClass = enchantClass;
		this.mReadableString = enchantClass.getProperty();
		this.mBukkitEnchant = null;
	}

	Enchantment(String s, org.bukkit.enchantments.Enchantment bukkitEnchant) {
		this.mEnchantClass = null;
		this.mReadableString = s;
		this.mBukkitEnchant = bukkitEnchant;
	}

	public boolean isCustomEnchant() {
		return this.mEnchantClass != null;
	}

	@Nullable
	public BaseEnchantment getEnchantClass() {
		return this.mEnchantClass;
	}

	public String getReadableString() {
		return this.mReadableString;
	}

	public boolean isBukkitEnchant() {
		return this.mBukkitEnchant != null;
	}

	@Nullable
	public org.bukkit.enchantments.Enchantment getBukkitEnchantment() {
		return this.mBukkitEnchant;
	}

	public static String[] valuesLowerCase() {
		Enchantment[] vals = Enchantment.values();
		String[] out = new String[vals.length];
		for (int i = 0; i < vals.length; i++) {
			out[i] = vals[i].toString().toLowerCase();
		}
		return out;
	}

	public boolean ignoresLevels() {
		switch (this) {
			case HOPE:
			case GILLS:
			case BAAING:
			case GILDED:
			case ETHERAL:
			case FESTIVE:
			case MENDING:
			case OINKING:
			case RADIANT:
			case STYLISH:
			case CLOAKING:
			case CLUCKING:
			case COLORFUL:
			case INFINITY:
			case DARKSIGHT:
			case INTUITION:
			case SILK_TOUCH:
			case TWO_HANDED:
			case DIVINE_AURA:
			case RESURRECTION:
			case VOID_TETHER:
			case AQUA_AFFINITY:
			case CURSE_BINDING:
			case INSTANT_DRINK:
			case CURSE_SHRAPNEL:
			case THROWING_KNIFE:
			case CURSE_CRIPPLING:
			case CURSE_VANISHING:
			case CURSE_CORRUPTION:
			case CURSE_IRREPARABLE:
			case CURSE_EPHEMERALITY:
			case JUNGLE_NOURISHMENT:
				return true;
			default:
				return false;
		}
	}
}
