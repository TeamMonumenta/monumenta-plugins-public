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
	CHANNELING("Channeling"),

	//--Elementalist Spec.
	BLIZZARD("Blizzard"),
	STARFALL("Starfall"),
	ELEMENTAL_SPIRIT_FIRE("Elemental Spirit (Fire)"),
	ELEMENTAL_SPIRIT_ICE("Elemental Spirit (Ice)"),

	//--Arcanist Spec.
	ARCANE_BARRAGE("Arcane Barrage"),
	FSWORD("Flash Sword"),

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

	//--Camper/Sniper
	ENCHANTED_ARROW("Enchanted Arrow"),
	SPLIT_ARROW("Split Arrow"),

	//--Ranger
	QUICKDRAW("Quickdraw"),
	TACTICAL_MANEUVER("Tactical Maneuver"),
	REFLEXES("Reflexes"),

	//Warlock

	//--Core Abilities
	AMPLIFYING("Amplifying Hex"),
	BLASPHEMY("Blasphemous Aura"),
	SOUL_REND("Soul Rend"),
	CONSUMING_FLAMES("Consuming Flames"),
	GRASPING_CLAWS("Grasping Claws"),
	EXORCISM("Exorcism"),

	//--Reaper
	GHOULISH_TAUNT("Ghoulish Taunt"),
	DEATHS_TOUCH("Death's Touch"),
	DARK_PACT("Dark Pact"),

	//--Tenebrist
	FRACTAL_ENERVATION("Fractal Enervation"),
	WITHERING_GAZE("Withering Gaze"),

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
	BRUTAL_ALCHEMY("Brutal Alchemy"),
	POWER_INJECTION("Power Injection"),
	IRON_TINCTURE("Iron Tincture"),
	ENFEEBLING_ELIXIR("Enfeebling Elixir"),
	UNSTABLE_ARROWS("Unstable Arrows"),
	ALCHEMICAL_ARTILLERY("Alchemical Artillery"),

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
