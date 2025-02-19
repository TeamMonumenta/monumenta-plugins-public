package com.playmonumenta.plugins.depths.charmfactory;

import com.playmonumenta.plugins.depths.DepthsTree;
import javax.annotation.Nullable;

public enum CharmNounConcepts {

	POWER("Power", null),
	FLAME("Flame", DepthsTree.FLAMECALLER),
	LIGHT("Light", DepthsTree.DAWNBRINGER),
	THE_HUNT("the Hunt", DepthsTree.STEELSAGE),
	FEAR("Fear", DepthsTree.SHADOWDANCER),
	TERROR("Terror", DepthsTree.SHADOWDANCER),
	WONDER("Wonder", null),
	ALARM("Alarm", DepthsTree.SHADOWDANCER),
	DREAD("Dread", DepthsTree.SHADOWDANCER),
	DEATH("Death", DepthsTree.SHADOWDANCER),
	PANIC("Panic", DepthsTree.SHADOWDANCER),
	DEMONS("Demons", DepthsTree.SHADOWDANCER),
	INFLUENCE("Influence", null),
	MIGHT("Might", null),
	POTENCY("Potency", null),
	STRENGTH("Strength", null),
	DOMINION("Dominion", null),
	SUPREMACY("Supremacy", null),
	CONTROL("Control", null),
	DIRECTION("Direction", null),
	GUIDANCE("Guidance", null),
	COMMAND("Command", null),
	AWE("Awe", null),
	MARVEL("Marvel", null),
	FASCINATION("Fascination", null),
	MIRACLES("Miracles", DepthsTree.DAWNBRINGER),
	REMEDIES("Remedies", DepthsTree.DAWNBRINGER),
	MENDING("Mending", DepthsTree.DAWNBRINGER),
	HEALING("Healing", DepthsTree.DAWNBRINGER),
	RESTORATION("Restoration", DepthsTree.DAWNBRINGER),
	PANACEA("Panacea", DepthsTree.DAWNBRINGER),
	RECOVERY("Recovery", DepthsTree.DAWNBRINGER),
	THE_CURE("the Cure", DepthsTree.DAWNBRINGER),
	TRACKING("Tracking", DepthsTree.STEELSAGE),
	PURSUIT("Pursuit", DepthsTree.STEELSAGE),
	ARCHERY("Archery", DepthsTree.STEELSAGE),
	BOWMANSHIP("Bowmanship", DepthsTree.STEELSAGE),
	THE_DEADEYE("the Deadeye", DepthsTree.STEELSAGE),
	FLETCHING("Fletching", DepthsTree.STEELSAGE),
	METAL("Metal", DepthsTree.STEELSAGE),
	SILVER("Silver", DepthsTree.STEELSAGE),
	ALLOY("Alloy", DepthsTree.STEELSAGE),
	INFERNO("Inferno", DepthsTree.FLAMECALLER),
	RADIATION("Radiation", DepthsTree.FLAMECALLER),
	BONFIRES("Bonfires", DepthsTree.FLAMECALLER),
	MISERY("Misery", DepthsTree.SHADOWDANCER),
	SUFFERING("Suffering", DepthsTree.SHADOWDANCER),
	HELLFIRE("Hellfire", DepthsTree.FLAMECALLER),
	HELLSPAWN("Hellspawn", DepthsTree.FLAMECALLER),
	TORTURE("Torture", DepthsTree.SHADOWDANCER),
	COMBUSTION("Combustion", DepthsTree.FLAMECALLER),
	BURNING("Burning", DepthsTree.FLAMECALLER),
	INCINERATION("Incineration", DepthsTree.FLAMECALLER),
	GROUNDING("Grounding", DepthsTree.EARTHBOUND),
	SOIL("Soil", DepthsTree.EARTHBOUND),
	TERRAIN("Terrain", DepthsTree.EARTHBOUND),
	DUST("Dust", DepthsTree.EARTHBOUND),
	MUCK("Muck", DepthsTree.EARTHBOUND),
	FILTH("Filth", DepthsTree.EARTHBOUND),
	RUBBLE("Rubble", DepthsTree.EARTHBOUND),
	DEBRIS("Debris", DepthsTree.EARTHBOUND),
	NATURE("Nature", DepthsTree.EARTHBOUND),
	CREATION("Creation", DepthsTree.EARTHBOUND),
	ENVIRONMENT("Environment", DepthsTree.EARTHBOUND),
	TEMPERAMENT("Temperament", DepthsTree.EARTHBOUND),
	ESSENCE("Essence", DepthsTree.EARTHBOUND),
	FORTITUDE("Fortitude", DepthsTree.EARTHBOUND),
	RESILIENCE("Resilience", DepthsTree.EARTHBOUND),
	PERSEVERANCE("Perseverance", DepthsTree.EARTHBOUND),
	FROST("Frost", DepthsTree.FROSTBORN),
	SLEET("Sleet", DepthsTree.FROSTBORN),
	SNOW("Snow", DepthsTree.FROSTBORN),
	CHILL("Chill", DepthsTree.FROSTBORN),
	TUNDRA("Tundra", DepthsTree.FROSTBORN),
	TAIGA("Taiga", DepthsTree.FROSTBORN),
	KNOWLEDGE("Knowledge", DepthsTree.FROSTBORN),
	WISDOM("Wisdom", DepthsTree.FROSTBORN),
	UNDERSTANDING("Understanding", DepthsTree.FROSTBORN),
	STAMINA("Stamina", null),
	COMPOSURE("Composure", null),
	TOLERANCE("Tolerance", null),
	SERENITY("Serenity", null),
	RESIGNATION("Resignation", null),
	STOICISM("Stoicism", null),
	SERENDIPITY("Serendipity", null),
	DISCOVERY("Discovery", null),
	BREEZE("Breeze", DepthsTree.WINDWALKER),
	GUSTS("Gusts", DepthsTree.WINDWALKER),
	STORM("Storm", DepthsTree.WINDWALKER),
	WHIRLWIND("Whirlwind", DepthsTree.WINDWALKER),
	FORCE("Force", DepthsTree.WINDWALKER),
	GALES("Gales", DepthsTree.WINDWALKER),
	HURRICANES("Hurricanes", DepthsTree.WINDWALKER),
	CYCLONES("Cyclones", DepthsTree.WINDWALKER),
	TEMPEST("Tempest", DepthsTree.WINDWALKER),
	BLIZZARD("Blizzard", DepthsTree.FROSTBORN),
	CONQUEST("Conquest", null),
	VICTORY("Victory", null),
	TRIUMPH("Triumph", null),
	GLORY("Glory", null),
	VINDICATION("Vindication", null),
	MERIT("Merit", null),
	COMPASSION("Compassion", null),
	LOYALTY("Loyalty", null),
	PERSISTENCE("Persistence", null),
	HUMOR("Humor", null),
	COURAGE("Courage", null),
	BRAVERY("Bravery", null),
	ASSISTANCE("Assistance", DepthsTree.DAWNBRINGER),
	FORTUNE("Fortune", null),
	PROSPERITY("Prosperity", null),
	BOUNTY("Bounty", null),
	WEALTH("Wealth", null),
	ARROGANCE("Arrogance", null),
	CONFUSION("Confusion", null),
	PERSUASION("Persuasion", null),
	LUST("Lust", null),
	GLUTTONY("Gluttony", null),
	GREED("Greed", null),
	SLOTH("Sloth", null),
	WRATH("Wrath", null),
	ENVY("Envy", null),
	PRIDE("Pride", null),
	THE_STARS("the Stars", null),
	THE_DEPTHS("the Depths", null),
	SUMMER("Summer", DepthsTree.FLAMECALLER),
	WINTER("Winter", DepthsTree.FROSTBORN),
	AUTUMN("Autumn", DepthsTree.WINDWALKER),
	SPRING("Spring", DepthsTree.EARTHBOUND),
	MEDICINE("Medicine", DepthsTree.DAWNBRINGER),
	DIRT("Dirt", DepthsTree.EARTHBOUND),
	PYROMANIA("Pyromania", DepthsTree.FLAMECALLER),
	STILLNESS("Stillness", DepthsTree.FROSTBORN),
	REGICIDE("Regicide", DepthsTree.SHADOWDANCER),
	NIGHT("Night", DepthsTree.SHADOWDANCER),
	ARTILLERY("Artillery", DepthsTree.STEELSAGE),
	THE_WIND("the Wind", DepthsTree.WINDWALKER);



	public final String mName;
	public final @Nullable DepthsTree mTree;

	CharmNounConcepts(String name, @Nullable DepthsTree tree) {
		mName = name;
		mTree = tree;
	}
}
