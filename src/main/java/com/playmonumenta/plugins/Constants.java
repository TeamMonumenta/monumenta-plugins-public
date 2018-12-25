package com.playmonumenta.plugins;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Constants {
	public static final int TICKS_PER_SECOND = 20;
	public static final int HALF_TICKS_PER_SECOND = (int)(TICKS_PER_SECOND / 2.0);
	public static final int QUARTER_TICKS_PER_SECOND = (int)(HALF_TICKS_PER_SECOND / 2.0);
	public static final int TICKS_PER_MINUTE = TICKS_PER_SECOND * 60;

	public static final int TWO_MINUTES = TICKS_PER_MINUTE * 2;
	public static final int THIRTY_SECONDS = TICKS_PER_SECOND * 30;

	public static final int FIVE_MINUTES = TICKS_PER_MINUTE * 5;
	public static final int TEN_MINUTES = TICKS_PER_MINUTE * 10;
	public static final int THIRTY_MINUTES = TICKS_PER_MINUTE * 30;
	public static final int ONE_HOUR = TICKS_PER_MINUTE * 60;
	public static final int THREE_HOURS = TICKS_PER_MINUTE * 180;

	public static final PotionEffect CAPITAL_SPEED_EFFECT = new PotionEffect(PotionEffectType.SPEED, 2 * TICKS_PER_SECOND, 1, true, false);
	public static final PotionEffect CITY_SATURATION_EFFECT = new PotionEffect(PotionEffectType.SATURATION, 2 * TICKS_PER_SECOND, 1, true, false);
	public static final PotionEffect CITY_RESISTANCE_EFFECT = new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 2 * TICKS_PER_SECOND, 4, true, false);
	public static final PotionEffect CITY_JUMP_MASK_EFFECT = new PotionEffect(PotionEffectType.JUMP, 2 * TICKS_PER_SECOND, 1000, true, false);
	public static final PotionEffect CITY_SPEED_MASK_EFFECT = new PotionEffect(PotionEffectType.SPEED, 2 * TICKS_PER_SECOND, 1000, true, false);

	//  USED FOR DEBUGGING PURPOSES WHEN COMPILING A JAR FOR OTHERS. (sadly it'll still contain the code in the jar, however it won't run)
	public static final boolean CLASSES_ENABLED = true;
	public static final boolean SPECIALIZATIONS_ENABLED = true;
	public static final boolean TRACKING_MANAGER_ENABLED = true;
	public static final boolean POTION_MANAGER_ENABLED = true;
	public static final boolean COMMANDS_SERVER_ENABLED = true;

	// Tag players always have when they are logged out and before they have data applied to them
	public static final String PLAYER_MID_TRANSFER_TAG = "MidTransfer";

	// Metadata keys
	public static final String SPAWNER_COUNT_METAKEY = "MonumentaSpawnCount";
	public static final String PLAYER_ITEMS_LOCKED_METAKEY = "MonumentaPlayerItemsLocked";
	public static final String PLAYER_DAMAGE_NONCE_METAKEY = "MonumentaPlayerDamageNonce";
	public static final String PLAYER_BOW_SHOT_METAKEY = "MonumentaPlayerBowShot";
	public static final String TREE_METAKEY = "MonumentaStructureGrowEvent";
	public static final String ENTITY_DAMAGE_NONCE_METAKEY = "MonumentaEntityDamageNonce";
	public static final String ENTITY_COMBUST_NONCE_METAKEY = "MonumentaEntityCombustNonce";
	public static final String ANVIL_CONFIRMATION_METAKEY = "MonumentaAnvilConfirmation";
	public static final String PLAYER_SNEAKING_TASK_METAKEY = "MonumentaPlayerSneaking";
	public static final String PLAYER_CHEST_SORT_CLICK_COUNT_METAKEY = "MonumentaPlayerChestSort";

	public static final String SCOREBOARD_DEATH_MESSAGE = "DeathMessage";

	// The max distance for spells that detected nearby damaged allies/enemies.
	public static final double ABILITY_ENTITY_DAMAGE_BY_ENTITY_RADIUS = 15;
}
