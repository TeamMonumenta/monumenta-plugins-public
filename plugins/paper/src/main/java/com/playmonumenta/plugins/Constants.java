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

	// Metadata keys
	public static final String SPAWNER_COUNT_METAKEY = "MonumentaSpawnCount";
	public static final String PLAYER_DAMAGE_NONCE_METAKEY = "MonumentaPlayerDamageNonce";
	public static final String PLAYER_BOW_SHOT_METAKEY = "MonumentaPlayerBowShot";
	public static final String TREE_METAKEY = "MonumentaStructureGrowEvent";
	public static final String ENTITY_DAMAGE_NONCE_METAKEY = "MonumentaEntityDamageNonce";
	public static final String ENTITY_COMBUST_NONCE_METAKEY = "MonumentaEntityCombustNonce";
	public static final String ENTITY_SLOWED_NONCE_METAKEY = "MonumentaEntitySlowedNonce";
	public static final String ANVIL_CONFIRMATION_METAKEY = "MonumentaAnvilConfirmation";
	public static final String PLAYER_SNEAKING_TASK_METAKEY = "MonumentaPlayerSneaking";

	public static final String SCOREBOARD_DEATH_MESSAGE = "DeathMessage";

	// The max distance for spells that detected nearby damaged allies/enemies.
	public static final double ABILITY_ENTITY_DAMAGE_BY_ENTITY_RADIUS = 15;

	// NMS ItemBow:
	// entityarrow.a(entityhuman, entityhuman.pitch, entityhuman.yaw, 0.0F, f * 3.0F, 1.0F);
	public static final int PLAYER_BOW_INITIAL_SPEED = 3;
	// NMS ItemCrossbow:
	// return itemstack.getItem() == Items.CROSSBOW && a(itemstack, Items.FIREWORK_ROCKET) ? 1.6F : 3.15F;
	public static final double PLAYER_CROSSBOW_ARROW_INITIAL_SPEED = 3.15;
	public static final double PLAYER_CROSSBOW_ROCKET_INITIAL_SPEED = 1.6;
	// NMS EntitySkeletonAbstract:
	// entityarrow.shoot(d0, d1 + d3 * 0.20000000298023224D, d2, 1.6F, (float)(14 - this.world.getDifficulty().a() * 4));
	public static final double SKELETON_SHOOT_INITIAL_SPEED = 1.6;
	// NMS EntityPillager:
	// this.a(this, entityliving, iprojectile, f, 1.6F);
	public static final double PILLAGER_SHOOT_INITIAL_SPEED = 1.6;
	// NMS EntityPiglin:
	// this.a(this, var0, var2, var3, 1.6F);
	public static final double PIGLIN_SHOOT_INITIAL_SPEED = 1.6;

	// + Discord channel
	// + In-game rank
	// + Plots iron block
	// + Castle head
	public static final int PATREON_TIER_1 = 1;
	// + White particles
	// + Stat tracking
	// + Shrine heads
	// + Hope skins
	public static final int PATREON_TIER_2 = 5;
	// + 1 daily buff
	// + Purple particles
	// Plots iron → gold block
	public static final int PATREON_TIER_3 = 10;
	// 1 → 2 daily buffs
	// + Green particles
	public static final int PATREON_TIER_4 = 20;
	// 2 → 3 daily buffs
	// + Red particles
	// Plots gold → diamond block
	public static final int PATREON_TIER_5 = 30;

	public static class Objectives {
		// PartialParticle
		public static final String PARTICLES_OWN_PASSIVE = "ParticlesOwnPassive";
		public static final String PARTICLES_OWN_ACTIVE = "ParticlesOwnActive";
		public static final String PARTICLES_OTHER_PASSIVE = "ParticlesOtherPassive";
		public static final String PARTICLES_OTHER_ACTIVE = "ParticlesOtherActive";
		public static final String PARTICLES_ENEMY = "ParticlesEnemy";
		public static final String PARTICLES_BOSS = "ParticlesBoss";


		// PlayerData
		public static final String NO_SELF_PARTICLES = "noSelfParticles";
		public static final String PATREON_DOLLARS = "Patreon";
	}
}