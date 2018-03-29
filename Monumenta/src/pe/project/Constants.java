package pe.project;

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
	public static final int THREE_HOURS = TICKS_PER_MINUTE * 180;

	public static final PotionEffect CAPITAL_SPEED_EFFECT = new PotionEffect(PotionEffectType.SPEED, TICKS_PER_SECOND, 1, true, false);
	public static final PotionEffect CITY_SATURATION_EFFECT = new PotionEffect(PotionEffectType.SATURATION, TICKS_PER_SECOND, 1, true, false);
	public static final PotionEffect CITY_RESISTANCE_EFFECT = new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, TICKS_PER_SECOND, 4, true, false);
	public static final PotionEffect CITY_JUMP_MASK_EFFECT = new PotionEffect(PotionEffectType.JUMP, TICKS_PER_SECOND, 1000, true, false);
	public static final PotionEffect CITY_SPEED_MASK_EFFECT = new PotionEffect(PotionEffectType.SPEED, TICKS_PER_SECOND, 1000, true, false);

	//	USED FOR DEBUGGING PURPOSES WHEN COMPILING A JAR FOR OTHERS. (sadly it'll still contain the code in the jar, however it won't run)
	public static final boolean CLASSES_ENABLED = true;
	public static final boolean POIS_ENABLED = true;
	public static final boolean TRACKING_MANAGER_ENABLED = true;
	public static final boolean POTION_MANAGER_ENABLED = true;
	public static final boolean COMMANDS_SERVER_ENABLED = true;

	// Metadata keys
	public static final String SPAWNER_COUNT_METAKEY = "MonumentaSpawnCount";
	public static final String PLAYER_ITEMS_LOCKED_METAKEY = "MonumentaPlayerItemsLocked";
	public static final String TREE_METAKEY = "MonumentaStructureGrowEvent";
	public static final String ENTITY_DAMAGE_NONCE_METAKEY = "MonumentaEntityDamageNonce";
	public static final String ENTITY_COMBUST_NONCE_METAKEY = "MonumentaEntityCombustNonce";
	public static final String ANVIL_CONFIRMATION_METAKEY = "MonumentaAnvilConfirmation";


	// For /back and /forward
	public static final String PLAYER_BACK_STACK_METAKEY = "MonumentaBackStack";
	public static final String PLAYER_FORWARD_STACK_METAKEY = "MonumentaForwardStack";
	public static final String PLAYER_SKIP_BACK_ADD_METAKEY = "MonumentaSkipAddToBackList";
}
