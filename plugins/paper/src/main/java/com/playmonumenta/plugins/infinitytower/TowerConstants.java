package com.playmonumenta.plugins.infinitytower;

import org.bukkit.NamespacedKey;

public class TowerConstants {

	public static boolean SHOULD_GAME_START = true;
	//This boolean is set to false if there has been some problem while loading the files
	//so the game will not start.

	//Floor designed
	public static int DESIGNED_FLOORS = 0;

	//Coins
	public static final int STARTING_GOLD = 6;

	public static int getGoldWin(int floor) {
		int q = floor / 10;
		if (floor % 10 == 0) {
			return 12 + ((q - 1) * 4);
		}
		return 3 + q;
	}

	public static final int COST_REROLL = 1;

	public static final String COIN_SCOREBOARD_NAME = "ITCoin";

	//Players Tags
	protected static final String SCOREBOARD_RESULT = "Blitz";
	public static final String TAG_BETWEEN_BATTLE = "ITBattleOver";
	public static final String TAG_IN_BATTLE = "ITBattleStarted";
	public static final String TAG_BOOK = "ITBook";
	public static final String PLAYER_TAG = "TowerPlayer";

	public static final int[] LEVEL_COST = {5, 8, 11, 14, 17, 20, 23, 26, 29};
	public static final int PLAYER_MAX_LEVEL = LEVEL_COST.length + 1;

	public static final int STARTING_TEAM_SIZE = 3;
	public static final int LEVEL_UP_TEAM_SIZE_INCREASE = 3;

	//Mobs
	public static final String MOB_TAG = "TowerMob";
	public static final String MOB_TAG_PLAYER_TEAM = "TowerMobPlayerTeam";
	public static final String MOB_TAG_FLOOR_TEAM = "TowerMobFloorTeam";

	public static final int MAX_MOB_LEVEL = 5;

	public static final String MOB_TAG_FIGHTER = "TowerFighter";
	public static final String MOB_TAG_DEFENDER = "TowerDefender";
	public static final String MOB_TAG_CASTER = "TowerCaster";
	public static final double DAMAGE_MLT_CLASS = 1.25;

	public static final String MOB_TAG_UNTARGETABLE = "UNTARGETABLE";



	//Blocks
	public static final String FALLING_BLOCK_TAG = "ITFallingBlock";


	//Floors
	public static int FLOOR_SIZE_X = 27;
	public static int FLOOR_SIZE_Z = 27;


	//Loots
	public static final NamespacedKey BOOK_LOOT_TABLE_KEY = NamespacedKey.fromString("epic:r1/blitz/blitz_master_guide");
	public static final NamespacedKey COIN_LOOT_TABLE_KEY = NamespacedKey.fromString("epic:r1/blitz/blitz_doubloon");

	public static final String TAG_UNLOAD_ENTITY = "ITUnloadMe";
	public static final String TAG_FIREWORK_ARMORSTAND = "ITFirework";


}
