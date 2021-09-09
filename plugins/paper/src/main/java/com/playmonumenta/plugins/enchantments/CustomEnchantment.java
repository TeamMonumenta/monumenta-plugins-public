package com.playmonumenta.plugins.enchantments;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.playmonumenta.plugins.enchantments.cosmetic.Baaing;
import com.playmonumenta.plugins.enchantments.cosmetic.Barking;
import com.playmonumenta.plugins.enchantments.cosmetic.Clucking;
import com.playmonumenta.plugins.enchantments.cosmetic.DivineAura;
import com.playmonumenta.plugins.enchantments.cosmetic.Festive;
import com.playmonumenta.plugins.enchantments.cosmetic.Gilded;
import com.playmonumenta.plugins.enchantments.cosmetic.Oinking;
import com.playmonumenta.plugins.enchantments.cosmetic.Stylish;
import com.playmonumenta.plugins.enchantments.curses.CurseOfAnemia;
import com.playmonumenta.plugins.enchantments.curses.CurseOfCorruption;
import com.playmonumenta.plugins.enchantments.curses.CurseOfCrippling;
import com.playmonumenta.plugins.enchantments.curses.CurseOfEphemerality;
import com.playmonumenta.plugins.enchantments.curses.CurseOfShrapnel;
import com.playmonumenta.plugins.enchantments.curses.Starvation;
import com.playmonumenta.plugins.enchantments.curses.TwoHanded;
import com.playmonumenta.plugins.enchantments.evasions.AbilityEvasion;
import com.playmonumenta.plugins.enchantments.evasions.Evasion;
import com.playmonumenta.plugins.enchantments.evasions.MeleeEvasion;
import com.playmonumenta.plugins.enchantments.infusions.Acumen;
import com.playmonumenta.plugins.enchantments.infusions.Focus;
import com.playmonumenta.plugins.enchantments.infusions.Perspicacity;
import com.playmonumenta.plugins.enchantments.infusions.Tenacity;
import com.playmonumenta.plugins.enchantments.infusions.Vigor;
import com.playmonumenta.plugins.enchantments.infusions.Vitality;
import com.playmonumenta.plugins.enchantments.infusions.delves.Ardor;
import com.playmonumenta.plugins.enchantments.infusions.delves.Aura;
import com.playmonumenta.plugins.enchantments.infusions.delves.Carapace;
import com.playmonumenta.plugins.enchantments.infusions.delves.Choler;
import com.playmonumenta.plugins.enchantments.infusions.delves.Empowered;
import com.playmonumenta.plugins.enchantments.infusions.delves.Epoch;
import com.playmonumenta.plugins.enchantments.infusions.delves.Execution;
import com.playmonumenta.plugins.enchantments.infusions.delves.Expedite;
import com.playmonumenta.plugins.enchantments.infusions.delves.Mitosis;
import com.playmonumenta.plugins.enchantments.infusions.delves.Natant;
import com.playmonumenta.plugins.enchantments.infusions.delves.Nutriment;
import com.playmonumenta.plugins.enchantments.infusions.delves.Pennate;
import com.playmonumenta.plugins.enchantments.infusions.delves.Reflection;
import com.playmonumenta.plugins.enchantments.infusions.delves.Understanding;
import com.playmonumenta.plugins.enchantments.infusions.delves.Usurper;




public enum CustomEnchantment {
	// Apply these first
	REGION_SCALING_DAMAGE_DEALT(new RegionScalingDamageDealt()),
	REGION_SCALING_DAMAGE_TAKEN(new RegionScalingDamageTaken()),

	// Infusions
	ACUMEN(new Acumen()),
	FOCUS(new Focus()),
	PERSPICACITY(new Perspicacity()),
	TENACITY(new Tenacity()),
	VIGOR(new Vigor()),
	VITALITY(new Vitality()),

	// Delve infusions
	PENNATE(new Pennate()),
	CARAPACE(new Carapace()),
	AURA(new Aura()),
	EXPEDITE(new Expedite()),
	CHOLER(new Choler()),
	USURPER(new Usurper()),
	EMPOWERED(new Empowered()),
	BALANCE(new Nutriment()),
	EXECUTION(new Execution()),
	REFLECTION(new Reflection()),
	MITOSIS(new Mitosis()),
	ARDOR(new Ardor()),
	EPOCH(new Epoch()),
	NATANT(new Natant()),
	NOTHINGNESS(new Understanding()),

	// On hit damage
	ARCANE_THRUST(new ArcaneThrust()),
	CHAOTIC(new Chaotic()),
	DUELIST(new Duelist()),
	HEX_EATER(new HexEater()),
	POINT_BLANK(new PointBlank()),
	SLAYER(new Slayer()),
	SNIPER(new Sniper()),

	// On hit debuffs
	BLEEDING(new Bleeding()),
	DECAY(new Decay()),
	FROST(new Frost()),
	ICE_ASPECT(new IceAspect()),
	SPARK(new Spark()),
	THUNDER_ASPECT(new ThunderAspect()),

	// Passives
	REGENERATION(new Regeneration()),
	MAINHAND_REGENERATION(new MainhandRegeneration()),

	ADRENALINE(new Adrenaline()),
	DARKSIGHT(new Darksight()),
	ERUPTION(new Eruption()),
	GILLS(new Gills()),
	INFERNO(new Inferno()),
	INTUITION(new Intuition()),
	LIFE_DRAIN(new LifeDrain()),
	QUAKE(new Quake()),
	RADIANT(new Radiant()),
	RECOIL(new Recoil()),
	REGICIDE(new Regicide()),
	RETRIEVAL(new Retrieval()),
	SAPPER(new Sapper()),
	SECOND_WIND(new SecondWind()),
	SUSTENANCE(new Sustenance()),
	WEIGHTLESS(new Weightless()),

	// Protections
	ABILITY_EVASION(new AbilityEvasion()),
	ASHES_OF_ETERNITY(new AshesOfEternity()),
	EVASION(new Evasion()),
	MELEE_EVASION(new MeleeEvasion()),
	RESURRECTION(new Resurrection()),
	VOID_TETHER(new VoidTether()),
	PROTECTION_OF_THE_DEPTHS(new ProtectionOfDepths()),

	// Durability
	COLOSSAL(new Colossal()),
	HOPE(new Hope()),
	HOPELESS(new Hopeless()),
	PHYLACTERY(new Phylactery()),

	// Curses
	CURSE_ANEMIA(new CurseOfAnemia()),
	CURSE_CORRUPTION(new CurseOfCorruption()),
	CURSE_CRIPPLING(new CurseOfCrippling()),
	CURSE_EPHEMERALITY(new CurseOfEphemerality()),
	CURSE_SHRAPNEL(new CurseOfShrapnel()),
	STARVATION(new Starvation()),
	TWO_HANDED(new TwoHanded()),

	// Active effects
	INSTANT_DRINK(new InstantDrink()),
	JUNGLES_NOURISHMENT(new JunglesNourishment()),
	MULTITOOL(new Multitool()),
	RAGE_OF_THE_KETER(new RageOfTheKeter()),
	THROWING_KNIFE(new ThrowingKnife()),

	// Cosmetic
	BAAING(new Baaing()),
	BARKING(new Barking()),
	CLUCKING(new Clucking()),
	DIVINE_AURA(new DivineAura()),
	FESTIVE(new Festive()),
	GILDED(new Gilded()),
	OINKING(new Oinking()),
	STYLISH(new Stylish()),

	// Apply these last
	STAT_TRACK(new StatTrack());

	private final @NotNull BaseEnchantment mEnchantment;

	CustomEnchantment(@NotNull BaseEnchantment enchantment) {
		mEnchantment = enchantment;
	}

	public @Nullable BaseEnchantment getEnchantment() {
		return mEnchantment;
	}
}