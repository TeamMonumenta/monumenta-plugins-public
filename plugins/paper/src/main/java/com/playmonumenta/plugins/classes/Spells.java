package com.playmonumenta.plugins.classes;

public enum Spells {

	//Mage

	//--Core Abilities
	MANA_LANCE("Mana Lance"),
	FROST_NOVA("Frost Nova"),
	PRISMATIC_SHIELD("Prismatic Shield"),
	MAGMA_SHIELD("Magma Shield"),
	ARCANE_STRIKE("Arcane Strike"),
	ELEMENTAL_ARROWS("Elemental Arrows"),
	SPELLSHOCK("Spellshock"),
	THUNDER_STEP("Thunder Step"),

	//--Elementalist Spec.
	BLIZZARD("Blizzard"),
	STARFALL("Starfall"),
	ELEMENTAL_SPIRIT_FIRE("Fire Elemental Spirit"),
	ELEMENTAL_SPIRIT_ICE("Ice Elemental Spirit"),

	//--Arcanist Spec.
	FSWORD("Flash Sword"),
	SPATIAL_SHATTER("Spatial Shatter"),
	ASTRAL_OMEN("Astral Omen"),

	//Rogue

	//--Core Abilities
	BY_MY_BLADE("By My Blade"),
	ADVANCING_SHADOWS("Advancing Shadows"),
	DODGING("Dodging"),
	ESCAPE_DEATH("Escape Death"),
	SMOKESCREEN("Smokescreen"),
	DAGGER_THROW("Dagger Throw"),

	//--Swordsage Spec.
	RONDE("Deadly Ronde"),
	BLADE_SURGE("Blade Surge"),
	BLADE_DANCE("Blade Dance"),
	WIND_WALK("Wind Walk"),

	//--Assassin Spec.
	BODKIN_BLITZ("Bodkin Blitz"),
	CLOAK_AND_DAGGER("Cloak And Dagger"),

	//Cleric

	//--Core Abilities
	CELESTIAL_BLESSING("Celestial Blessing"),
	CLEANSING("Cleansing Rain"),
	HEALING("Hand of Light"),
	SANCTIFIED("Sanctified Armor"),

	//--Pally
	HOLY_JAVELIN("Holy Javelin"),
	CHOIR_BELLS("Choir Bells"),
	LUMINOUS_INFUSION("Luminous Infusion"),

	//--Hierophant
	HALLOWED_BEAM("Hallowed Beam"),
	THURIBLE_PROCESSION("Thurible Procession"),
	ENCHANTED_PRAYER("Enchanted Prayer"),

	//Scout

	//-- Core Abilities
	VOLLEY("Volley"),
	EAGLE_EYE("Eagle Eye"),

	//--Hunter
	ENCHANTED_ARROW("Enchanted Arrow"),
	SPLIT_ARROW("Split Arrow"),

	//--Ranger
	QUICKDRAW("Quickdraw"),
	TACTICAL_MANEUVER("Tactical Maneuver"),
	WHIRLING_BLADE("Whirling Blade"),

	//Warlock

	//--Core Abilities
	AMPLIFYING("Amplifying Hex"),
	PHLEGMATIC_RESOLVE("Phlegmatic Resolve"),
	SOUL_REND("Soul Rend"),
	CHOLERIC_FLAMES("Choleric Flames"),
	SANGUINE_HARVEST("Sanguine Harvest"),
	MELANCHOLIC_LAMENT("Melancholic Lament"),
	GRASPING_CLAWS("Grasping Claws"),
	EXORCISM("Exorcism"),

	//--Reaper
	VOODOO_BONDS("Voodoo Bonds"),
	JUDGEMENT_CHAIN("Judgement Chain"),
	DARK_PACT("Dark Pact"),

	//--Tenebrist
	WITHERING_GAZE("Withering Gaze"),
	HAUNTING_SHADES("Haunting Shades"),
	UMBRAL_WAIL("Umbral Wail"),

	//Warrior

	//--Core Abilites
	RIPOSTE("Riposte"),
	DEFENSIVE_LINE("Defensive Line"),
	COUNTER_STRIKE("Counter Strike"),
	SHIELD_BASH("Shield Bash"),
	BRUTE_FORCE("Brute Force"),

	//--Berserker
	METEOR_SLAM("Meteor Slam"),
	RECKLESS_SWING("Reckless Swing"),
	RAMPAGE("Rampage"),

	//--Guardian
	SHIELD_WALL("Shield Wall"),
	CHALLENGE("Challenge"),
	BODYGUARD("Bodyguard"),

	//Alchemist
	ALCHEMIST_POTION("Alchemist Potion"),
	BASILISK_POISON("Basilisk Poison"),
	BRUTAL_ALCHEMY("Brutal Alchemy"),
	POWER_INJECTION("Power Injection"),
	IRON_TINCTURE("Iron Tincture"),
	ENFEEBLING_ELIXIR("Enfeebling Elixir"),
	UNSTABLE_ARROWS("Unstable Arrows"),
	BEZOAR("Bezoar"),

	//Harbinger
	NIGHTMARISH_ALCHEMY("Nightmarish Alchemy"),
	SCORCHED_EARTH("Scorched Earth"),
	PURPLE_HAZE("Purple Haze"),

	//Apothecary
	WARDING_REMEDY("Warding Remedy"),
	INVIGORATING_ODOR("Invigorating_Odor"),
	ALCHEMICAL_AMALGAM("Alchemical Amalgam");

	private final String mName;

	Spells(String name) {
		this.mName = name;
	}

	public String getName() {
		return mName;
	}
}
