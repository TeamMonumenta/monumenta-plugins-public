package com.playmonumenta.plugins.classes;

import org.jetbrains.annotations.NotNull;

import com.playmonumenta.plugins.abilities.cleric.DivineJustice;
import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.abilities.mage.FrostNova;
import com.playmonumenta.plugins.abilities.mage.MagmaShield;
import com.playmonumenta.plugins.abilities.mage.Spellshock;
import com.playmonumenta.plugins.abilities.mage.ThunderStep;
import com.playmonumenta.plugins.abilities.mage.arcanist.AstralOmen;
import com.playmonumenta.plugins.abilities.mage.arcanist.SpatialShatter;
import com.playmonumenta.plugins.abilities.mage.elementalist.Blizzard;
import com.playmonumenta.plugins.abilities.mage.elementalist.Starfall;
import com.playmonumenta.plugins.abilities.warrior.berserker.MeteorSlam;



// TODO perhaps simply pass around class literals instead of maintaining this?
/*
 * Order does not matter here, it is simply alphebetical.
 * Where order matters for what runs first is in AbilityManager.
 * This is just to link existing spells to custom damage events.
 */
public enum ClassAbility {
	// [Mage]
	ARCANE_STRIKE("Arcane Strike"),
	ELEMENTAL_ARROWS(ElementalArrows.NAME),
	FROST_NOVA(FrostNova.NAME),
	MAGMA_SHIELD(MagmaShield.NAME),
	MANA_LANCE("Mana Lance"),
	PRISMATIC_SHIELD("Prismatic Shield"),
	SPELLSHOCK(Spellshock.NAME),
	THUNDER_STEP(ThunderStep.NAME),

	// Arcanist
	ASTRAL_OMEN(AstralOmen.NAME),
	SPATIAL_SHATTER(SpatialShatter.NAME),

	// Elementalist
	BLIZZARD(Blizzard.NAME),
	ELEMENTAL_SPIRIT_FIRE("Fire Elemental Spirit"),
	ELEMENTAL_SPIRIT_ICE("Ice Elemental Spirit"),
	STARFALL(Starfall.NAME),

	// [Rogue]
	ADVANCING_SHADOWS("Advancing Shadows"),
	BY_MY_BLADE("By My Blade"),
	DAGGER_THROW("Dagger Throw"),
	DODGING("Dodging"),
	ESCAPE_DEATH("Escape Death"),
	SMOKESCREEN("Smokescreen"),

	// Assassin
	BODKIN_BLITZ("Bodkin Blitz"),
	CLOAK_AND_DAGGER("Cloak And Dagger"),

	// Swordsage
	BLADE_DANCE("Blade Dance"),
	DEADLY_RONDE("Deadly Ronde"),
	WIND_WALK("Wind Walk"),

	// [Cleric]
	CELESTIAL_BLESSING("Celestial Blessing"),
	CLEANSING_RAIN("Cleansing Rain"),
	DIVINE_JUSTICE(DivineJustice.NAME),
	HAND_OF_LIGHT("Hand of Light"),
	SANCTIFIED_ARMOR("Sanctified Armor"),

	// Hierophant
	ENCHANTED_PRAYER("Enchanted Prayer"),
	HALLOWED_BEAM("Hallowed Beam"),
	THURIBLE_PROCESSION("Thurible Procession"),

	// Paladin
	CHOIR_BELLS("Choir Bells"),
	HOLY_JAVELIN("Holy Javelin"),
	LUMINOUS_INFUSION("Luminous Infusion"),

	// [Scout]
	EAGLE_EYE("Eagle Eye"),
	VOLLEY("Volley"),
	WIND_BOMB("Wind Bomb"),
	HUNTING_COMPANION("Hunting Companion"),

	// Hunter
	PREDATOR_STRIKE("Predator Strike"),
	SPLIT_ARROW("Split Arrow"),

	// Ranger
	TACTICAL_MANEUVER("Tactical Maneuver"),
	QUICKDRAW("Quickdraw"),
	WHIRLING_BLADE("Whirling Blade"),

	// [Warlock]
	AMPLIFYING("Amplifying Hex"),
	CHOLERIC_FLAMES("Choleric Flames"),
	GRASPING_CLAWS("Grasping Claws"),
	MELANCHOLIC_LAMENT("Melancholic Lament"),
	SANGUINE_HARVEST("Sanguine Harvest"),
	SOUL_REND("Soul Rend"),

	// Reaper
	DARK_PACT("Dark Pact"),
	JUDGEMENT_CHAIN("Judgement Chain"),
	VOODOO_BONDS("Voodoo Bonds"),

	// Tenebrist
	HAUNTING_SHADES("Haunting Shades"),
	UMBRAL_WAIL("Umbral Wail"),
	WITHERING_GAZE("Withering Gaze"),

	// [Warrior]
	BRUTE_FORCE("Brute Force"),
	COUNTER_STRIKE("Counter Strike"),
	DEFENSIVE_LINE("Defensive Line"),
	RIPOSTE("Riposte"),
	SHIELD_BASH("Shield Bash"),

	// Berserker
	METEOR_SLAM(MeteorSlam.NAME),
	RAMPAGE("Rampage"),
	RECKLESS_SWING("Reckless Swing"),

	// Guardian
	BODYGUARD("Bodyguard"),
	CHALLENGE("Challenge"),
	SHIELD_WALL("Shield Wall"),

	// [Alchemist]
	ALCHEMIST_POTION("Alchemist Potion"),
	BASILISK_POISON("Basilisk Poison"),
	BEZOAR("Bezoar"),
	BRUTAL_ALCHEMY("Brutal Alchemy"),
	ENFEEBLING_ELIXIR("Enfeebling Elixir"),
	IRON_TINCTURE("Iron Tincture"),
	POWER_INJECTION("Power Injection"),
	UNSTABLE_ARROWS("Unstable Arrows"),

	// Apothecary
	ALCHEMICAL_AMALGAM("Alchemical Amalgam"),
	INVIGORATING_ODOR("Invigorating_Odor"),
	WARDING_REMEDY("Warding Remedy"),

	// Harbinger
	NIGHTMARISH_ALCHEMY("Nightmarish Alchemy"),
	PURPLE_HAZE("Purple Haze"),
	SCORCHED_EARTH("Scorched Earth"),

	// [DEPTHS ABILITIES]

	// FLAMECALLER
	APOCALYPSE("Apocalypse"),
	FIREBALL("Fireball"),
	RING_OF_FLAMES("Ring of Flames"),
	FLAMESTRIKE("Flamestrike"),
	VOLCANIC_METEOR("Volcanic Meteor"),
	PYROBLAST("Pyroblast"),

	// STEELSAGE
	METALMANCY("Metalmancy"),
	RAPIDFIRE("Rapid Fire"),
	SIDEARM("Sidearm"),
	SCRAPSHOT("Scrapshot"),
	FIREWORKBLAST("Firework Blast"),

	// WINDWALKER
	GUARDING_BOLT("Guarding Bolt"),
	LAST_BREATH("Last Breath"),
	SKYHOOK("Skyhook"),
	SLIPSTREAM("Slipstream"),
	UPDRAFT("Updraft"),
	WHIRLWIND("Whirlwind"),
	HOWLINGWINDS("Howling Winds"),

	// SHADOW
	BLADE_FLURRY("Blade Flurry"),
	CLOAK_OF_SHADOWS("Cloak of Shadows"),
	CHAOS_DAGGER("Chaos Dagger"),
	DUMMY_DECOY("Dummy Decoy"),

	// SUNLIGHT
	BOTTLED_SUNLIGHT("Bottled Sunlight"),
	LIGHTNING_BOTTLE("Lightning Bottle"),
	RADIANT_BLESSING("Radiant Blessing"),
	TOTEM_OF_SALVATION("Totem of Salvation"),
	WARD_OF_LIGHT("Ward of Light"),

	// FROSTBORN
	ICE_LANCE("Ice Lance"),
	ICE_BARRIER("Ice Barrier"),
	AVALANCHE("Avalanche"),
	PIERCING_COLD("Piercing Cold"),
	CRYOBOX("Cryobox"),

	// EARTHBORN
	BULWARK("Bulwark"),
	CRUSHING_EARTH("Crushing Earth"),
	EARTHEN_WRATH("Earthen Wrath"),
	EARTHQUAKE("Earthquake"),
	STONE_SKIN("Stone Skin"),
	TAUNT("Taunt");

	private final @NotNull String mName;

	ClassAbility(@NotNull String name) {
		this.mName = name;
	}

	public @NotNull String getName() {
		return mName;
	}
}
