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

	//--Pyromancer Spec
	INFERNO("Inferno"),
	ASHEN_HEART("Ashen Heart"),

	//--Cyromancer Spec.

	BLIZZARD("Blizzard"),
	COLD_WAVE("Cold Wave"),
	FROZEN_HEART("Frozen Heart"),

	//--Elementalist Spec.

	METEOR_STRIKE("Meteor Strike"),
	ELEMENTAL_SPIRIT("Elemental Spirit"),
	FROST_RAY("Frost Ray"),

	//--Arcanist Spec.
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
	SNAKE_HEAD("Snake Head"),
	BLADE_SURGE("Blade Surge"),
	BLADE_DANCE("Blade Dance"),
	WIND_WALK("Wind Walk"),

	//--Assassin Spec.
	PERFECT_KILL("Perfect Kill"),
	CLOAK_AND_DAGGER("Cloak And Dagger"),

	//Cleric

	//--Core Abilities
	CELESTIAL_BLESSING("Celestial Blessing"),
	CLEANSING("Cleansing Rain"),
	HEALING("Hand of Light"),

	//--Pally
	HOLY_JAVELIN("Holy Javelin"),
	CHOIR_BELLS("Choir Bells"),
	LUMINOUS_INFUSION("Luminous Infusion"),

	//--Hierophant
	HALLOWED_BEAM("Hallowed Beam"),
	INCENSED_THURIBLE("Incensed Thurible"),
	ENCHANTED_PRAYER("Enchanted Prayer"),

	//Scout

	//-- Core Abilities
	VOLLEY("Volley"),
	EAGLE_EYE("Eagle Eye"),

	//--Camper/Sniper
	ENCHANTED_ARROW("Enchanted Arrow"),

	//--Ranger
	QUICKDRAW("Quickdraw"),
	DISENGAGE("Disengage"),
	PRECISION_STRIKE("Precision Strike"),

	//Warlock

	//--Core Abilities
	AMPLIFYING("Amplifying Hex"),
	BLASPHEMY("Blasphemous Aura"),
	SOUL_REND("Soul Rend"),
	CONSUMING_FLAMES("Consuming Flames"),
	GRASPING_CLAW("Grasping Claw"),

	//--Reaper
	HUNGERING_VORTEX("Hungering Vortex"),
	DEATHS_TOUCH("Death's Touch"),
	DARK_PACT("Dark Pact"),

	//--Tenebrist
	HYPNOTIC_RAGE("Hypnotic Rage"),
	SABLE_SPHERE("Sable Sphere"),
	BLACK_CLOUDS("Black Clouds"),

	//Warrior

	//--Core Abilites
	RIPOSTE("Riposte"),
	DEFENSIVE_LINE("Defensive Line"),

	//--Berserker
	METEOR_SLAM("Meteor Slam"),

	//Alchemist
	POWER_INJECTION("Power Injection"),
	GRUESOME_ALCHEMY("Gruesome Alchemy"),
	PUTRID_FUMES("Putrid Fumes"),
	CAUSTIC_BLADE("Caustic Blade"),
	IRON_TINCTURE("Iron Tincture"),
	ENFEEBLING_ELIXIR("Enfeebling Elixir"),
	UNSTABLE_ARROWS("Unstable Arrows"),

	//Harbinger
	ADRENAL_SERUM("Adrenal Serum");

	private final String name;

	Spells(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
