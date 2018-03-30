package pe.project.classes;

import pe.project.Plugin.Classes;

public enum Spells {

	//Mage
	MANA_LANCE("Mana Lance", false, Classes.MAGE),
	FROST_NOVA("Frost Nova", false, Classes.MAGE),
	PRISMATIC_SHIELD("Prismatic Shield", false, Classes.MAGE),
	MAGMA_SHIELD("Magma Shield", false, Classes.MAGE),
	ARCANE_STRIKE("Arcane Strike", false, Classes.MAGE),

	//Assassin
	BY_MY_BLADE("By My Blade", false, Classes.ROGUE),
	ADVANCING_SHADOWS("Advancing Shadows", false, Classes.ROGUE),
	DODGING("Dodging", false, Classes.ROGUE),
	ESCAPE_DEATH("Escape Death", false, Classes.ROGUE),
	SMOKESCREEN("Smokescreen", false, Classes.ROGUE),

	//Cleric
	CELESTIAL_BLESSING("Celestial Blessing", false, Classes.CLERIC),
	CELESTIAL_FAKE_1("Celestial Blessing", true, Classes.CLERIC),
	CELESTIAL_FAKE_2("Celestial Blessing", true, Classes.CLERIC),
	CLEANSING("Cleansing Rain", false, Classes.CLERIC),
	CLEANSING_FAKE("Cleansing Rain", true, Classes.CLERIC),
	HEALING("Hand of Light", false, Classes.CLERIC),

	//Scout
	VOLLEY("Volley", false, Classes.SCOUT),
	STANDARD_BEARER("Standard Bearer", false, Classes.SCOUT),
	STANDARD_BEARER_FAKE("Standard Bearer", true, Classes.SCOUT),
	EAGLE_EYE("Eagle Eye", false, Classes.SCOUT),

	//Warlock
	AMPLIFYING("Amplifying Hex", false, Classes.WARLOCK),
	BLASPHEMY("Blasphemous Aura", false, Classes.WARLOCK),
	SOUL_REND("Soul Rend", false, Classes.WARLOCK),
	CONSUMING_FLAMES("Consuming Flames", false, Classes.WARLOCK),
	GRASPING_CLAW("Grasping Claw", false, Classes.WARLOCK),

	//Warrior
	RIPOSTE("Riposte", false, Classes.WARRIOR),
	DEFENSIVE_LINE("Defensive Line", false, Classes.WARRIOR),

	//Alchemist
	POWER_INJECTION("Power Injection", false, Classes.ALCHEMIST),
	GRUESOME_ALCHEMY("Gruesome Alchemy", false, Classes.ALCHEMIST),
	PUTRID_FUMES("Putrid Fumes", false, Classes.ALCHEMIST);

	private final String name;
	private final boolean fake;
	private final Classes classes;

	Spells(String name, boolean fake, Classes classes) {
		this.name = name;
		this.fake = fake;
		this.classes = classes;
	}


	public String getName() {
		return name;
	}

	public Classes getBelongingClass() {
		return classes;
	}

	public boolean isFake() {
		return fake;
	}

}
