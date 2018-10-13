package com.playmonumenta.plugins.classes;

import com.playmonumenta.plugins.Plugin.Classes;

public enum Spells {

	//Mage

	//--Core Abilities
	MANA_LANCE("Mana Lance", Classes.MAGE),
	FROST_NOVA("Frost Nova", Classes.MAGE),
	PRISMATIC_SHIELD("Prismatic Shield", Classes.MAGE),
	MAGMA_SHIELD("Magma Shield", Classes.MAGE),
	ARCANE_STRIKE("Arcane Strike", Classes.MAGE),
	ELEMENTAL_ARROWS("Elemental Arrows", Classes.MAGE),

	//--Pyromancer Spec
	INFERNO("Inferno", Classes.MAGE),
	ASHEN_HEART("Ashen Heart", Classes.MAGE),

	//--Cyromancer Spec.

	BLIZZARD("Blizzard", Classes.MAGE),
	COLD_WAVE("Cold Wave", Classes.MAGE),
	FROZEN_HEART("Frozen Heart", Classes.MAGE),

	//--Elementalist Spec.

	METEOR_STRIKE("Meteor Strike", Classes.MAGE),
	ELEMENTAL_SPIRIT("Elemental Spirit", Classes.MAGE),
	GLACIAL_RIFT("Glacial Rift", Classes.MAGE),

	//--Arcanist Spec.
	FSWORD("Flash Sword", Classes.MAGE),

	//Rogue

	//--Core Abilities
	BY_MY_BLADE("By My Blade", Classes.ROGUE),
	ADVANCING_SHADOWS("Advancing Shadows", Classes.ROGUE),
	DODGING("Dodging", Classes.ROGUE),
	ESCAPE_DEATH("Escape Death", Classes.ROGUE),
	SMOKESCREEN("Smokescreen", Classes.ROGUE),
	DAGGER_THROW("Dagger Throw", Classes.ROGUE),

	//--Swordsage Spec.
	SNAKE_HEAD("Snake Head", Classes.ROGUE),
	BLADE_SURGE("Blade Surge", Classes.ROGUE),
	BLADE_DANCE("Blade Dance", Classes.ROGUE),

	//--Assassin Spec.
	PERFECT_KILL("Perfect Kill", Classes.ROGUE),

	//Cleric

	//--Core Abilities
	CELESTIAL_BLESSING("Celestial Blessing", Classes.CLERIC),
	CLEANSING("Cleansing Rain", Classes.CLERIC),
	HEALING("Hand of Light", Classes.CLERIC),

	//--Pally
	HOLY_JAVELIN("Holy Javelin", Classes.CLERIC),
	CHOIR_BELLS("Choir Bells", Classes.CLERIC),
	LUMINOUS_INFUSION("Luminous Infusion", Classes.CLERIC),

	//--Hierophant
	HALLOWED_BEAM("Hallowed Beam", Classes.CLERIC),
	INCENSED_THURIBLE("Incensed Thurible", Classes.CLERIC),

	//Scout

	//-- Core Abilities
	VOLLEY("Volley", Classes.SCOUT),
	STANDARD_BEARER("Standard Bearer", Classes.SCOUT),
	EAGLE_EYE("Eagle Eye", Classes.SCOUT),

	//--Camper/Sniper
	ENCHANTED_ARROW("Enchanted Arrow", Classes.SCOUT),

	//--Ranger
	QUICKSHOT("Quickshot", Classes.SCOUT),
	DISENGAGE("Disengage", Classes.SCOUT),
	DEADEYE("Deadeye", Classes.SCOUT),
	PRECISION_STRIKE("Precision Strike", Classes.SCOUT),

	//Warlock

	//--Core Abilities
	AMPLIFYING("Amplifying Hex", Classes.WARLOCK),
	BLASPHEMY("Blasphemous Aura", Classes.WARLOCK),
	SOUL_REND("Soul Rend", Classes.WARLOCK),
	CONSUMING_FLAMES("Consuming Flames", Classes.WARLOCK),
	GRASPING_CLAW("Grasping Claw", Classes.WARLOCK),

	//--Reaper
	DARK_ERUPTION("Dark Eruption", Classes.WARLOCK),
	PETRIFYING_GLARE("Petrifying Glare", Classes.WARLOCK),
	SOULREAPING("Soulreaping", Classes.WARLOCK),

	//--Tenebrist
	HYPNOTIC_RAGE("Hypnotic Rage", Classes.WARLOCK),
	SABLE_SPHERE("Sable Sphere", Classes.WARLOCK),
	BLACK_CLOUDS("Black Clouds", Classes.WARLOCK),

	//Warrior

	//--Core Abilites
	RIPOSTE("Riposte", Classes.WARRIOR),
	DEFENSIVE_LINE("Defensive Line", Classes.WARRIOR),

	//--Berserker
	RECKLESS_SWING("Reckless Swing", Classes.WARRIOR),
	METEOR_SLAM("Meteor Slam", Classes.WARRIOR),

	//Alchemist
	POWER_INJECTION("Power Injection", Classes.ALCHEMIST),
	GRUESOME_ALCHEMY("Gruesome Alchemy", Classes.ALCHEMIST),
	PUTRID_FUMES("Putrid Fumes", Classes.ALCHEMIST),
	CAUSTIC_BLADE("Caustic Blade", Classes.ALCHEMIST),
	IRON_TINCTURE("Iron Tincture", Classes.ALCHEMIST),
	ENFEEBLING_ELIXIR("Enfeebling Elixir", Classes.ALCHEMIST),
	BOMB_ARROW("Unstable Arrows", Classes.ALCHEMIST);

	private final String name;
	private final Classes classes;

	Spells(String name, Classes classes) {
		this.name = name;
		this.classes = classes;
	}

	public String getName() {
		return name;
	}

	public Classes getBelongingClass() {
		return classes;
	}
}
