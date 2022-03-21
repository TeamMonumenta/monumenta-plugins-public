package com.playmonumenta.plugins;

import java.util.EnumSet;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
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
	// + Patreon Parakeet
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

	public static final int DUNGEON_INSTANCE_MODULUS = 10000;

	static {
		Materials.WEARABLE.addAll(Materials.ARMOR);
	}

	public static class Objectives {
		// PartialParticle
		public static final String PP_OWN_PASSIVE = "PPOwnPassive";
		public static final String PP_OWN_BUFF = "PPOwnBuff";
		public static final String PP_OWN_ACTIVE = "PPOwnActive";
		public static final String PP_OTHER_PASSIVE = "PPOtherPassive";
		public static final String PP_OTHER_BUFF = "PPOtherBuff";
		public static final String PP_OTHER_ACTIVE = "PPOtherActive";
		public static final String PP_ENEMY_BUFF = "PPEnemyBuff";
		public static final String PP_ENEMY = "PPEnemy";
		public static final String PP_BOSS = "PPBoss";

		// PlayerData
		public static final String PATREON_DOLLARS = "Patreon";
	}

	public static class Tags {
		// PlayerData
		public static final String NO_SELF_PARTICLES = "noSelfParticles";
		public static final String NO_TRANSPOSING = "NoTransposing";
		public static final String STASIS = "Stasis";
	}

	// Note blocks
	public static class NotePitches {
		public static final float FS0 = calculatePitch(0);
		public static final float G1 = calculatePitch(1);
		public static final float GS2 = calculatePitch(2);
		public static final float A3 = calculatePitch(3);
		public static final float AS4 = calculatePitch(4);
		public static final float B5 = calculatePitch(5);
		public static final float C6 = calculatePitch(6);
		public static final float CS7 = calculatePitch(7);
		public static final float D8 = calculatePitch(8);
		public static final float DS9 = calculatePitch(9);
		public static final float E10 = calculatePitch(10);
		public static final float F11 = calculatePitch(11);
		public static final float FS12 = calculatePitch(12);
		public static final float G13 = calculatePitch(13);
		public static final float GS14 = calculatePitch(14);
		public static final float A15 = calculatePitch(15);
		public static final float AS16 = calculatePitch(16);
		public static final float B17 = calculatePitch(17);
		public static final float C18 = calculatePitch(18);
		public static final float CS19 = calculatePitch(19);
		public static final float D20 = calculatePitch(20);
		public static final float DS21 = calculatePitch(21);
		public static final float E22 = calculatePitch(22);
		public static final float F23 = calculatePitch(23);
		public static final float FS24 = calculatePitch(24);

		public static float calculatePitch(int clicks) {
			return 0.5f * (float)Math.pow(2, (clicks / 12d));
		}
	}

	public static class Colors {
		public static final TextColor GREENISH_BLUE = TextColor.color(85, 255, 170);
		public static final TextColor GREENISH_BLUE_DARK = TextColor.color(76, 230, 153);
	}

	public static class Materials {
		public static final EnumSet<Material> ARMOR = EnumSet.of(
			Material.LEATHER_HELMET,
			Material.LEATHER_CHESTPLATE,
			Material.LEATHER_LEGGINGS,
			Material.LEATHER_BOOTS,

			Material.GOLDEN_HELMET,
			Material.GOLDEN_CHESTPLATE,
			Material.GOLDEN_LEGGINGS,
			Material.GOLDEN_BOOTS,

			Material.CHAINMAIL_HELMET,
			Material.CHAINMAIL_CHESTPLATE,
			Material.CHAINMAIL_LEGGINGS,
			Material.CHAINMAIL_BOOTS,

			Material.IRON_HELMET,
			Material.IRON_CHESTPLATE,
			Material.IRON_LEGGINGS,
			Material.IRON_BOOTS,

			Material.DIAMOND_HELMET,
			Material.DIAMOND_CHESTPLATE,
			Material.DIAMOND_LEGGINGS,
			Material.DIAMOND_BOOTS,

			Material.NETHERITE_HELMET,
			Material.NETHERITE_CHESTPLATE,
			Material.NETHERITE_LEGGINGS,
			Material.NETHERITE_BOOTS,

			Material.TURTLE_HELMET,
			Material.ELYTRA
		);

		// ARMOR is added in static {}
		public static final EnumSet<Material> WEARABLE = EnumSet.of(
			Material.PLAYER_HEAD,
			Material.CREEPER_HEAD,
			Material.DRAGON_HEAD,
			Material.SKELETON_SKULL,
			Material.WITHER_SKELETON_SKULL,
			Material.ZOMBIE_HEAD,

			Material.CARVED_PUMPKIN
		);

		public static final EnumSet<Material> SWORDS = EnumSet.of(
			Material.WOODEN_SWORD,
			Material.STONE_SWORD,
			Material.GOLDEN_SWORD,
			Material.IRON_SWORD,
			Material.DIAMOND_SWORD,
			Material.NETHERITE_SWORD
		);
		public static final EnumSet<Material> BOWS = EnumSet.of(
			Material.BOW,
			Material.CROSSBOW
		);
		public static final EnumSet<Material> HOES = EnumSet.of(
			Material.WOODEN_HOE,
			Material.STONE_HOE,
			Material.GOLDEN_HOE,
			Material.IRON_HOE,
			Material.DIAMOND_HOE,
			Material.NETHERITE_HOE
		);
		public static final EnumSet<Material> PICKAXES = EnumSet.of(
			Material.WOODEN_PICKAXE,
			Material.STONE_PICKAXE,
			Material.GOLDEN_PICKAXE,
			Material.IRON_PICKAXE,
			Material.DIAMOND_PICKAXE,
			Material.NETHERITE_PICKAXE
		);
		public static final EnumSet<Material> AXES = EnumSet.of(
			Material.WOODEN_AXE,
			Material.STONE_AXE,
			Material.GOLDEN_AXE,
			Material.IRON_AXE,
			Material.DIAMOND_AXE,
			Material.NETHERITE_AXE
		);
		public static final EnumSet<Material> SHOVELS = EnumSet.of(
			Material.WOODEN_SHOVEL,
			Material.STONE_SHOVEL,
			Material.GOLDEN_SHOVEL,
			Material.IRON_SHOVEL,
			Material.NETHERITE_SHOVEL
		);

		public static final EnumSet<Material> POTIONS = EnumSet.of(
			Material.POTION,
			Material.SPLASH_POTION,
			Material.LINGERING_POTION
		);
	}
}
