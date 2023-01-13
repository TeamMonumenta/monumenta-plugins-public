package com.playmonumenta.plugins.itemstats;

import com.playmonumenta.plugins.CustomLogger;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.AbilityCooldownDecrease;
import com.playmonumenta.plugins.effects.AbilityCooldownIncrease;
import com.playmonumenta.plugins.effects.AbsorptionSickness;
import com.playmonumenta.plugins.effects.ArrowSaving;
import com.playmonumenta.plugins.effects.Bleed;
import com.playmonumenta.plugins.effects.BonusSoulThreads;
import com.playmonumenta.plugins.effects.BoonOfKnightlyPrayer;
import com.playmonumenta.plugins.effects.BoonOfThePit;
import com.playmonumenta.plugins.effects.CrystalineBlessing;
import com.playmonumenta.plugins.effects.DeepGodsEndowment;
import com.playmonumenta.plugins.effects.DurabilitySaving;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.HealingSickness;
import com.playmonumenta.plugins.effects.NegateDamage;
import com.playmonumenta.plugins.effects.PercentAttackSpeed;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentExperience;
import com.playmonumenta.plugins.effects.PercentHeal;
import com.playmonumenta.plugins.effects.PercentHealthBoost;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.effects.SilverPrayer;
import com.playmonumenta.plugins.effects.StarCommunion;
import com.playmonumenta.plugins.effects.Stasis;
import com.playmonumenta.plugins.effects.TuathanBlessing;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.enchantments.Starvation;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.EnumSet;
import java.util.NavigableSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public enum EffectType {
	//when ever a new EffectType get added here remember to add it also in the switch inside applyEffect(...)

	// type: is the unique key save inside the nbt of the item
	// name: is the name that the player will see on the item -> format:  +/-dd% name (x:yy)
	// isPositive: if the display should be blue (positive) or red (negative)
	// isFlat: not a percentage (true) or percentage (false)
	// isConstant: does it have a number associated?
	// isVanilla: is it a vanilla effect?
	VANILLA_SPEED("VanillaSpeed", "Speed", true, true, false, true),
	VANILLA_SLOW("VanillaSlow", "Slowness", false, true, false, false),
	VANILLA_HASTE("Haste", "Haste", true, true, false, true),
	VANILLA_FATIGUE("MiningFatigue", "Mining Fatigue", false, true, false, true),
	VANILLA_JUMP("JumpBoost", "Jump Boost", true, true, false, true),
	VANILLA_FIRE_RESISTANCE("VanillaFireRes", "Fire Immunity", true, true, true, true),
	VANILLA_WATER_BREATH("WaterBreath", "Water Breathing", true, true, true, true),
	VANILLA_BLINDNESS("Blindness", "Blindness", false, true, true, true),
	VANILLA_NIGHT_VISION("NightVision", "Night Vision", true, true, true, true),
	VANILLA_POISON("Poison", "Poison", false, true, false, true),
	VANILLA_WITHER("Wither", "Wither", false, true, false, true),
	VANILLA_REGEN("Regeneration", "Regeneration", true, true, false, true),
	VANILLA_HEAL("InstantHealth", "Instant Health", true, false, false, true),
	VANILLA_DAMAGE("InstantDamage", "Instant Damage", false, false, false, true),
	VANILLA_SATURATION("Saturation", "Saturation", true, true, false, true),
	VANILLA_GLOW("Glowing", "Glowing", true, true, true, true),
	VANILLA_SLOWFALL("SlowFalling", "Slow Falling", true, true, false, true),
	VANILLA_CONDUIT("ConduitPower", "Conduit Power", true, true, true, true),
	VANILLA_HUNGER("Hunger", "Hunger", false, true, false, true),
	VANILLA_NAUSEA("Nausea", "Nausea", false, true, true, true),
	VANILLA_BADLUCK("BadLuck", "Bad Luck", false, true, false, true),

	SPEED("Speed", "Speed", true, false, false, false),
	SLOW("Slow", "Speed", false, false, false, false),

	ATTACK_SPEED("AttackSpeed", "Attack Speed", true, false, false, false),
	NEGATIVE_ATTACK_SPEED("NegativeAttackSpeed", "Attack Speed", false, false, false, false),

	KNOCKBACK_RESIST("KnockbackResist", "Knockback Resistance", true, false, false, false),
	NEGATIVE_KNOCKBACK_RESIST("NegativeKnockbackResist", "Knockback Resistance", false, false, false, false),

	MAX_HEALTH_INCREASE("MaxHealthIncrease", "Max Health", true, false, false, false),
	MAX_HEALTH_DECREASE("MaxHealthDecrease", "Max Health", false, false, false, false),

	ABSORPTION("Absorption", "Absorption Health", true, false, false, false),
	SATURATION("Saturation", "Saturation", true, true, false, false),
	STARVATION("Starvation", "Starvation", false, true, false, false),

	//Resistance type of effects
	RESISTANCE("Resistance", "Resistance", true, false, false, false),
	MELEE_RESISTANCE("MeleeResistance", "Melee Resistance", true, false, false, false),
	PROJECTILE_RESISTANCE("ProjectileResistance", "Projectile Resistance", true, false, false, false),
	MAGIC_RESISTANCE("MagicResistance", "Magic Resistance", true, false, false, false),
	BLAST_RESISTANCE("BlastResistance", "Blast Resistance", true, false, false, false),
	FIRE_RESISTANCE("FireResistance", "Fire Resistance", true, false, false, false),
	FALL_RESISTANCE("FallResistance", "Fall Resistance", true, false, false, false),

	//Damage Negation
	DAMAGE_NEGATE("DamageNegate", "Hits Blocked", true, true, false, false),
	MELEE_DAMAGE_NEGATE("MeleeDamageNegate", "Melee Hits Blocked", true, true, false, false),
	PROJECTILE_DAMAGE_NEGATE("ProjectileDamageNegate", "Projectile Hits Blocked", true, true, false, false),
	MAGIC_DAMAGE_NEGATE("MagicDamageNegate", "Magic Hits Blocked", true, true, false, false),
	BLAST_DAMAGE_NEGATE("BlastDamageNegate", "Blast Hits Blocked", true, true, false, false),
	FIRE_DAMAGE_NEGATE("FireDamageNegate", "Fire Hits Blocked", true, true, false, false),
	FALL_DAMAGE_NEGATE("FallDamageNegate", "Falling Hits Blocked", true, true, false, false),

	//Vulnerability type of effects
	VULNERABILITY("Vulnerability", "Resistance", false, false, false, false),
	MELEE_VULNERABILITY("MeleeVulnerability", "Melee Resistance", false, false, false, false),
	PROJECTILE_VULNERABILITY("ProjectileVulnerability", "Projectile Resistance", false, false, false, false),
	MAGIC_VULNERABILITY("MagicVulnerability", "Magic Resistance", false, false, false, false),
	BLAST_VULNERABILITY("BlastVulnerability", "Blast Resistance", false, false, false, false),
	FIRE_VULNERABILITY("FireVulnerability", "Fire Resistance", false, false, false, false),
	FALL_VULNERABILITY("FallVulnerability", "Fall Resistance", false, false, false, false),

	//Damage type of effects
	DAMAGE("damage", "Strength", true, false, false, false),
	MAGIC_DAMAGE("MagicDamage", "Magic Damage", true, false, false, false),
	MELEE_DAMAGE("MeleeDamage", "Melee Damage", true, false, false, false),
	PROJECTILE_DAMAGE("ProjectileDamage", "Projectile Damage", true, false, false, false),

	//Weakness type of effects
	WEAKNESS("Weakness", "Weakness", false, false, false, false),
	MAGIC_WEAKNESS("MagicWeakness", "Magic Damage", false, false, false, false),
	MELEE_WEAKNESS("MeleeWeakness", "Melee Damage", false, false, false, false),
	PROJECTILE_WEAKNESS("ProjectileWeakness", "Projectile Damage", false, false, false, false),

	INSTANT_HEALTH("InstantHealthPercent", "Instant Health", true, false, false, false),
	INSTANT_DAMAGE("InstantDamagePercent", "Instant Damage", false, false, false, false),

	HEAL("Heal", "Healing Rate", true, false, false, false),
	ANTI_HEAL("AntiHeal", "Healing Rate", false, false, false, false),

	ARROW_SAVING("ArrowSaving", "Arrow Save Chance", true, false, false, false),
	ARROW_LOSS("ArrowSaving", "Arrow Save Chance", false, false, false, false),

	SOUL_THREAD_BONUS("SoulThreadBonus", "Soul Thread Chance", true, false, false, false),
	SOUL_THREAD_REDUCTION("SoulThreadReduction", "Soul Thread Chance", false, false, false, false),

	DURABILITY_SAVE("DurabilitySave", "Durability", true, false, false, false),
	DURABILITY_LOSS("DurabilityLoss", "Durability", false, false, false, false),

	EXP_BONUS("ExpBonus", "Experience", true, false, false, false),
	EXP_LOSS("ExpLoss", "Experience", false, false, false, false),

	COOLDOWN_DECREASE("AbilityCooldownDecrease", "Ability Cooldowns", true, false, false, false),
	COOLDOWN_INCREASE("AbilityCooldownIncrease", "Ability Cooldowns", false, false, false, false),

	BLEED("Bleed", "Bleed", false, false, false, false),

	STASIS("Stasis", "Stasis", true, false, true, false),

	BOON_OF_THE_PIT("BoonOfThePit", "Boon of the Pit", true, false, true, false),
	BOON_OF_SILVER_SCALES("BoonOfSilverScales", "Boon of Silver Scales", true, false, true, false),
	BOON_OF_KNIGHTLY_PRAYER("BoonOfKnightlyPrayer", "Boon of Knightly Prayer", true, false, true, false),
	CRYSTALLINE_BLESSING("CrystallineBlessing", "Crystalline Blessing", true, false, true, false),
	CURSE_OF_THE_DARK_SOUL("DarkSoul", "Curse of the Dark Soul", false, false, true, false),
	DEEP_GODS_ENDOWMENT("DeepGodsEndowment", "Deep God's Endowment", true, false, true, false),
	HARRAKFARS_BLESSING("HarrakfarsBlessing", "Harrakfar's Blessing", true, false, true, false),
	SILVER_PRAYER("SilverPrayer", "Silver Prayer", true, false, true, false),
	STAR_COMMUNION("StarCommunion", "Star Communion", true, false, true, false),
	TUATHAN_BLESSING("TuathanBlessing", "Tuathan Blessing", true, false, true, false);

	public static final String KEY = "Effects";

	private final String mType;
	private final String mName;
	private final Boolean mIsPositive;
	private final Boolean mIsFlat;
	private final Boolean mIsConstant;
	private final Boolean mIsVanilla;

	EffectType(String type, String name, Boolean isPositive, Boolean isFlat, Boolean isConstant, Boolean isVanilla) {
		mType = type;
		mName = name;
		mIsPositive = isPositive;
		mIsFlat = isFlat;
		mIsConstant = isConstant;
		mIsVanilla = isVanilla;
	}

	public String getType() {
		return mType;
	}

	public String getName() {
		return mName;
	}

	public Boolean isPositive() {
		return mIsPositive;
	}

	public Boolean isFlat() {
		return mIsFlat;
	}

	public Boolean isConstant() {
		return mIsConstant;
	}

	public Boolean isVanilla() {
		return mIsVanilla;
	}

	public static @Nullable EffectType fromType(String type) {
		for (EffectType effectType : values()) {
			if (effectType.mType.equals(type)) {
				return effectType;
			}
		}
		return null;
	}

	public static Component getComponent(@Nullable EffectType effectType, double strength, int duration) {
		if (effectType == null) {
			return Component.empty();
		}

		int minutes = duration / 1200;
		int seconds = (duration / 20) % 60;
		String timeString = "(" + minutes + ":" + (seconds > 9 ? seconds : "0" + seconds) + ")";
		if (minutes > 999) {
			timeString = "(∞)";
		}
		String color;
		String add;

		if (effectType.isPositive()) {
			color = "#4AC2E5";
			add = "+";
		} else {
			color = "#D02E28";
			add = "-";
		}

		if (effectType.getType().contains("Cooldown")) {
			if (effectType.isPositive()) {
				add = "-";
			} else {
				add = "+";
			}
		}

		if (effectType == EffectType.STARVATION) {
			return Component.text(effectType.mName + " " + ItemStatUtils.toRomanNumerals((int) strength), TextColor.fromHexString(color)).decoration(TextDecoration.ITALIC, false);
		}

		if (effectType.isVanilla()) {
			if (effectType.getType().contains("Instant")) {
				return Component.text(effectType.mName + " " + ItemStatUtils.toRomanNumerals((int) strength), TextColor.fromHexString(color)).decoration(TextDecoration.ITALIC, false);
			} else if (effectType.isConstant()) {
				return Component.text(effectType.mName + " ", TextColor.fromHexString(color)).decoration(TextDecoration.ITALIC, false).append(
					Component.text(timeString, TextColor.fromHexString("#555555")).decoration(TextDecoration.ITALIC, false)
				);
			}
			return Component.text(effectType.mName + " " + ItemStatUtils.toRomanNumerals((int) strength) + " ", TextColor.fromHexString(color)).decoration(TextDecoration.ITALIC, false).append(
				Component.text(timeString, TextColor.fromHexString("#555555")).decoration(TextDecoration.ITALIC, false)
			);
		} else {
			if (effectType.getType().contains("Instant")) {
				return Component.text((int) (strength * 100) + "% " + effectType.mName + " ", TextColor.fromHexString(color)).decoration(TextDecoration.ITALIC, false);
			}
			if (effectType.isConstant()) {
				return Component.text(effectType.mName + " ", TextColor.fromHexString(color)).decoration(TextDecoration.ITALIC, false).append(
					Component.text(timeString, TextColor.fromHexString("#555555")).decoration(TextDecoration.ITALIC, false)
				);
			}
			if (effectType.isFlat()) {
				return Component.text(add + ((int) strength) + " " + effectType.mName + " ", TextColor.fromHexString(color)).decoration(TextDecoration.ITALIC, false).append(
					Component.text(timeString, TextColor.fromHexString("#555555")).decoration(TextDecoration.ITALIC, false)
				);
			}
			return Component.text(add + (int) (strength * 100) + "% " + effectType.mName + " ", TextColor.fromHexString(color)).decoration(TextDecoration.ITALIC, false).append(
				Component.text(timeString, TextColor.fromHexString("#555555")).decoration(TextDecoration.ITALIC, false)
			);
		}
	}

	public static void applyEffect(@Nullable EffectType effectType, Entity entity, int duration, double strength, @Nullable String source, boolean applySickness) {
		if (effectType == null) {
			return;
		}

		String sourceString = (source != null ? effectType.mName + source : effectType.mName);
		Player player = (Player) entity;
		Plugin plugin = Plugin.getInstance();

		switch (effectType) {
			case VANILLA_SPEED -> {
				PotionUtils.applyPotion(plugin, player, new PotionEffect(PotionEffectType.SPEED, duration, (int) (strength - 1), true));
				plugin.mPotionManager.addPotion(player, PotionManager.PotionID.APPLIED_POTION, new PotionEffect(PotionEffectType.SPEED, duration, (int) (strength - 1), true));
			}
			case VANILLA_SLOW -> {
				PotionUtils.applyPotion(plugin, player, new PotionEffect(PotionEffectType.SLOW, duration, (int) (strength - 1), true));
				plugin.mPotionManager.addPotion(player, PotionManager.PotionID.APPLIED_POTION, new PotionEffect(PotionEffectType.SLOW, duration, (int) (strength - 1), true));
			}
			case VANILLA_HASTE -> {
				PotionUtils.applyPotion(plugin, player, new PotionEffect(PotionEffectType.FAST_DIGGING, duration, (int) (strength - 1), true));
				plugin.mPotionManager.addPotion(player, PotionManager.PotionID.APPLIED_POTION, new PotionEffect(PotionEffectType.FAST_DIGGING, duration, (int) (strength - 1), true));
			}
			case VANILLA_FATIGUE -> {
				PotionUtils.applyPotion(plugin, player, new PotionEffect(PotionEffectType.SLOW_DIGGING, duration, (int) (strength - 1), true));
				plugin.mPotionManager.addPotion(player, PotionManager.PotionID.APPLIED_POTION, new PotionEffect(PotionEffectType.SLOW_DIGGING, duration, (int) (strength - 1), true));
			}
			case VANILLA_JUMP -> {
				PotionUtils.applyPotion(plugin, player, new PotionEffect(PotionEffectType.JUMP, duration, (int) (strength - 1), true));
				plugin.mPotionManager.addPotion(player, PotionManager.PotionID.APPLIED_POTION, new PotionEffect(PotionEffectType.JUMP, duration, (int) (strength - 1), true));
			}
			case VANILLA_FIRE_RESISTANCE -> {
				PotionUtils.applyPotion(plugin, player, new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, (int) (strength - 1), true));
				plugin.mPotionManager.addPotion(player, PotionManager.PotionID.APPLIED_POTION, new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration, (int) (strength - 1), true));
			}
			case VANILLA_WATER_BREATH -> {
				PotionUtils.applyPotion(plugin, player, new PotionEffect(PotionEffectType.WATER_BREATHING, duration, (int) (strength - 1), true));
				plugin.mPotionManager.addPotion(player, PotionManager.PotionID.APPLIED_POTION, new PotionEffect(PotionEffectType.WATER_BREATHING, duration, (int) (strength - 1), true));
			}
			case VANILLA_BLINDNESS -> {
				PotionUtils.applyPotion(plugin, player, new PotionEffect(PotionEffectType.BLINDNESS, duration, (int) (strength - 1), true));
				plugin.mPotionManager.addPotion(player, PotionManager.PotionID.APPLIED_POTION, new PotionEffect(PotionEffectType.BLINDNESS, duration, (int) (strength - 1), true));
			}
			case VANILLA_NIGHT_VISION -> {
				PotionUtils.applyPotion(plugin, player, new PotionEffect(PotionEffectType.NIGHT_VISION, duration, (int) (strength - 1), true));
				plugin.mPotionManager.addPotion(player, PotionManager.PotionID.APPLIED_POTION, new PotionEffect(PotionEffectType.NIGHT_VISION, duration, (int) (strength - 1), true));
			}
			case VANILLA_POISON -> {
				PotionUtils.applyPotion(plugin, player, new PotionEffect(PotionEffectType.POISON, duration, (int) (strength - 1), true));
				plugin.mPotionManager.addPotion(player, PotionManager.PotionID.APPLIED_POTION, new PotionEffect(PotionEffectType.POISON, duration, (int) (strength - 1), true));
			}
			case VANILLA_WITHER -> {
				PotionUtils.applyPotion(plugin, player, new PotionEffect(PotionEffectType.WITHER, duration, (int) (strength - 1), true));
				plugin.mPotionManager.addPotion(player, PotionManager.PotionID.APPLIED_POTION, new PotionEffect(PotionEffectType.WITHER, duration, (int) (strength - 1), true));
			}
			case VANILLA_REGEN -> {
				PotionUtils.applyPotion(plugin, player, new PotionEffect(PotionEffectType.REGENERATION, duration, (int) (strength - 1), true));
				plugin.mPotionManager.addPotion(player, PotionManager.PotionID.APPLIED_POTION, new PotionEffect(PotionEffectType.REGENERATION, duration, (int) (strength - 1), true));
			}
			case VANILLA_HEAL -> {
				PlayerUtils.healPlayer(plugin, player, EntityUtils.getMaxHealth(player) * 0.2 * strength);
				applyHealingSickness(entity, applySickness, player, plugin);
			}
			case VANILLA_DAMAGE -> DamageUtils.damage(null, player, DamageEvent.DamageType.AILMENT, EntityUtils.getMaxHealth(player) * 0.2 * strength);
			case VANILLA_SATURATION -> {
				PotionUtils.applyPotion(plugin, player, new PotionEffect(PotionEffectType.SATURATION, duration, (int) (strength - 1), true));
				plugin.mPotionManager.addPotion(player, PotionManager.PotionID.APPLIED_POTION, new PotionEffect(PotionEffectType.SATURATION, duration, (int) (strength - 1), true));
			}
			case VANILLA_GLOW -> {
				PotionUtils.applyPotion(plugin, player, new PotionEffect(PotionEffectType.GLOWING, duration, (int) (strength - 1), true));
				plugin.mPotionManager.addPotion(player, PotionManager.PotionID.APPLIED_POTION, new PotionEffect(PotionEffectType.GLOWING, duration, (int) (strength - 1), true));
			}
			case VANILLA_SLOWFALL -> {
				PotionUtils.applyPotion(plugin, player, new PotionEffect(PotionEffectType.SLOW_FALLING, duration, (int) (strength - 1), true));
				plugin.mPotionManager.addPotion(player, PotionManager.PotionID.APPLIED_POTION, new PotionEffect(PotionEffectType.SLOW_FALLING, duration, (int) (strength - 1), true));
			}
			case VANILLA_CONDUIT -> {
				PotionUtils.applyPotion(plugin, player, new PotionEffect(PotionEffectType.CONDUIT_POWER, duration, (int) (strength - 1), true));
				plugin.mPotionManager.addPotion(player, PotionManager.PotionID.APPLIED_POTION, new PotionEffect(PotionEffectType.CONDUIT_POWER, duration, (int) (strength - 1), true));
			}
			case VANILLA_HUNGER -> {
				PotionUtils.applyPotion(plugin, player, new PotionEffect(PotionEffectType.HUNGER, duration, (int) (strength - 1), true));
				plugin.mPotionManager.addPotion(player, PotionManager.PotionID.APPLIED_POTION, new PotionEffect(PotionEffectType.HUNGER, duration, (int) (strength - 1), true));
			}
			case VANILLA_NAUSEA -> {
				PotionUtils.applyPotion(plugin, player, new PotionEffect(PotionEffectType.CONFUSION, duration, (int) (strength - 1), true));
				plugin.mPotionManager.addPotion(player, PotionManager.PotionID.APPLIED_POTION, new PotionEffect(PotionEffectType.CONFUSION, duration, (int) (strength - 1), true));
			}
			case VANILLA_BADLUCK -> {
				PotionUtils.applyPotion(plugin, player, new PotionEffect(PotionEffectType.UNLUCK, duration, (int) (strength - 1), true));
				plugin.mPotionManager.addPotion(player, PotionManager.PotionID.APPLIED_POTION, new PotionEffect(PotionEffectType.UNLUCK, duration, (int) (strength - 1), true));
			}

			case SPEED -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentSpeed(duration, strength, sourceString));
			case SLOW -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentSpeed(duration, -strength, sourceString));

			case ATTACK_SPEED -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentAttackSpeed(duration, strength, sourceString));
			case NEGATIVE_ATTACK_SPEED -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentAttackSpeed(duration, -strength, sourceString));

			case KNOCKBACK_RESIST -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentKnockbackResist(duration, strength, sourceString));
			case NEGATIVE_KNOCKBACK_RESIST -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentKnockbackResist(duration, -strength, sourceString));

			case MAX_HEALTH_INCREASE -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentHealthBoost(duration, strength, sourceString));
			case MAX_HEALTH_DECREASE -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentHealthBoost(duration, -strength, sourceString));

			case ABSORPTION -> {
				if (entity instanceof LivingEntity livingEntity) {
					double amount = strength * EntityUtils.getMaxHealth(livingEntity);
					AbsorptionUtils.addAbsorption(livingEntity, amount, amount, duration);
					if (applySickness) {
						double sicknessPenalty = 0;
						NavigableSet<Effect> sicks = plugin.mEffectManager.getEffects(player, "AbsorptionSickness");
						if (sicks != null) {
							Effect sick = sicks.last();
							sicknessPenalty = sick.getMagnitude();
						}
						plugin.mEffectManager.addEffect(entity, "AbsorptionSickness", new AbsorptionSickness(20 * 15, Math.min(sicknessPenalty + 0.2, 0.8), "AbsorptionSickness"));
					}
				}
			}
			case STARVATION -> Starvation.apply(player, (int) strength);

			case RESISTANCE -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, -strength));
			case MELEE_RESISTANCE -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, -strength, EnumSet.of(DamageEvent.DamageType.MELEE)));
			case PROJECTILE_RESISTANCE -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, -strength, EnumSet.of(DamageEvent.DamageType.PROJECTILE)));
			case MAGIC_RESISTANCE -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, -strength, EnumSet.of(DamageEvent.DamageType.MAGIC)));
			case BLAST_RESISTANCE -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, -strength, EnumSet.of(DamageEvent.DamageType.BLAST)));
			case FIRE_RESISTANCE -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, -strength, EnumSet.of(DamageEvent.DamageType.FIRE)));
			case FALL_RESISTANCE -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, -strength, EnumSet.of(DamageEvent.DamageType.FALL)));

			case DAMAGE_NEGATE -> plugin.mEffectManager.addEffect(entity, sourceString, new NegateDamage(duration, (int) strength));
			case MELEE_DAMAGE_NEGATE -> plugin.mEffectManager.addEffect(entity, sourceString, new NegateDamage(duration, (int) strength, EnumSet.of(DamageEvent.DamageType.MELEE)));
			case PROJECTILE_DAMAGE_NEGATE -> plugin.mEffectManager.addEffect(entity, sourceString, new NegateDamage(duration, (int) strength, EnumSet.of(DamageEvent.DamageType.PROJECTILE)));
			case MAGIC_DAMAGE_NEGATE -> plugin.mEffectManager.addEffect(entity, sourceString, new NegateDamage(duration, (int) strength, EnumSet.of(DamageEvent.DamageType.MAGIC)));
			case BLAST_DAMAGE_NEGATE -> plugin.mEffectManager.addEffect(entity, sourceString, new NegateDamage(duration, (int) strength, EnumSet.of(DamageEvent.DamageType.BLAST)));
			case FIRE_DAMAGE_NEGATE -> plugin.mEffectManager.addEffect(entity, sourceString, new NegateDamage(duration, (int) strength, EnumSet.of(DamageEvent.DamageType.FIRE)));
			case FALL_DAMAGE_NEGATE -> plugin.mEffectManager.addEffect(entity, sourceString, new NegateDamage(duration, (int) strength, EnumSet.of(DamageEvent.DamageType.FALL)));

			case VULNERABILITY -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, strength));
			case MELEE_VULNERABILITY -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, strength, EnumSet.of(DamageEvent.DamageType.MELEE)));
			case PROJECTILE_VULNERABILITY -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, strength, EnumSet.of(DamageEvent.DamageType.PROJECTILE)));
			case MAGIC_VULNERABILITY -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, strength, EnumSet.of(DamageEvent.DamageType.MAGIC)));
			case BLAST_VULNERABILITY -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, strength, EnumSet.of(DamageEvent.DamageType.BLAST)));
			case FIRE_VULNERABILITY -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, strength, EnumSet.of(DamageEvent.DamageType.FIRE)));
			case FALL_VULNERABILITY -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, strength, EnumSet.of(DamageEvent.DamageType.FALL)));

			case DAMAGE -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentDamageDealt(duration, strength, DamageEvent.DamageType.getScalableDamageType()));
			case PROJECTILE_DAMAGE -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentDamageDealt(duration, strength, EnumSet.of(DamageEvent.DamageType.PROJECTILE)));
			case MAGIC_DAMAGE -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentDamageDealt(duration, strength, EnumSet.of(DamageEvent.DamageType.MAGIC)));
			case MELEE_DAMAGE -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentDamageDealt(duration, strength, EnumSet.of(DamageEvent.DamageType.MELEE)));

			case WEAKNESS -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentDamageDealt(duration, -strength));
			case PROJECTILE_WEAKNESS -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentDamageDealt(duration, -strength, EnumSet.of(DamageEvent.DamageType.PROJECTILE)));
			case MAGIC_WEAKNESS -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentDamageDealt(duration, -strength, EnumSet.of(DamageEvent.DamageType.MAGIC)));
			case MELEE_WEAKNESS -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentDamageDealt(duration, -strength, EnumSet.of(DamageEvent.DamageType.MELEE)));

			case HEAL -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentHeal(duration, strength));
			case ANTI_HEAL -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentHeal(duration, -strength));

			case INSTANT_HEALTH -> {
				PlayerUtils.healPlayer(plugin, player, EntityUtils.getMaxHealth(player) * strength);
				applyHealingSickness(entity, applySickness, player, plugin);
			}
			case INSTANT_DAMAGE -> DamageUtils.damage(null, player, DamageEvent.DamageType.AILMENT, EntityUtils.getMaxHealth(player) * strength);

			case ARROW_SAVING -> plugin.mEffectManager.addEffect(entity, sourceString, new ArrowSaving(duration, strength));
			case ARROW_LOSS -> plugin.mEffectManager.addEffect(entity, sourceString, new ArrowSaving(duration, -strength));

			case SOUL_THREAD_BONUS -> plugin.mEffectManager.addEffect(entity, sourceString, new BonusSoulThreads(duration, strength));
			case SOUL_THREAD_REDUCTION -> plugin.mEffectManager.addEffect(entity, sourceString, new BonusSoulThreads(duration, -strength));

			case DURABILITY_SAVE -> plugin.mEffectManager.addEffect(entity, sourceString, new DurabilitySaving(duration, strength));
			case DURABILITY_LOSS -> plugin.mEffectManager.addEffect(entity, sourceString, new DurabilitySaving(duration, -strength));

			case COOLDOWN_DECREASE -> plugin.mEffectManager.addEffect(entity, sourceString, new AbilityCooldownDecrease(duration, strength));
			case COOLDOWN_INCREASE -> plugin.mEffectManager.addEffect(entity, sourceString, new AbilityCooldownIncrease(duration, strength));

			case EXP_BONUS -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentExperience(duration, strength));
			case EXP_LOSS -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentExperience(duration, -strength));

			case BLEED -> plugin.mEffectManager.addEffect(entity, sourceString, new Bleed(duration, strength, plugin));

			case STASIS -> plugin.mEffectManager.addEffect(entity, sourceString, new Stasis(duration));

			case BOON_OF_THE_PIT -> plugin.mEffectManager.addEffect(entity, sourceString, new BoonOfThePit(duration));
			case BOON_OF_SILVER_SCALES -> plugin.mEffectManager.addEffect(entity, sourceString, new AbilityCooldownDecrease(duration, 0.05));
			case BOON_OF_KNIGHTLY_PRAYER -> plugin.mEffectManager.addEffect(entity, sourceString, new BoonOfKnightlyPrayer(duration));
			case CRYSTALLINE_BLESSING -> plugin.mEffectManager.addEffect(entity, sourceString, new CrystalineBlessing(duration));
			case CURSE_OF_THE_DARK_SOUL -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentDamageReceived(duration, 1));
			case DEEP_GODS_ENDOWMENT -> plugin.mEffectManager.addEffect(entity, sourceString, new DeepGodsEndowment(duration));
			case HARRAKFARS_BLESSING -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentHeal(duration, 0.1));
			case SILVER_PRAYER -> plugin.mEffectManager.addEffect(entity, sourceString, new SilverPrayer(duration));
			case STAR_COMMUNION -> plugin.mEffectManager.addEffect(entity, sourceString, new StarCommunion(duration));
			case TUATHAN_BLESSING -> plugin.mEffectManager.addEffect(entity, sourceString, new TuathanBlessing(duration));

			default -> CustomLogger.getInstance().warning("No EffectType implemented in applyEffect(..) for: " + effectType.mType);

		}
	}

	private static void applyHealingSickness(Entity entity, boolean applySickness, Player player, Plugin plugin) {
		if (applySickness) {
			double sicknessPenalty = 0;
			NavigableSet<Effect> sicks = plugin.mEffectManager.getEffects(player, "HealingSickness");
			if (sicks != null) {
				Effect sick = sicks.last();
				sicknessPenalty = sick.getMagnitude();
			}
			plugin.mEffectManager.addEffect(entity, "HealingSickness", new HealingSickness(20 * 15, Math.min(sicknessPenalty + 0.2, 0.8), "HealingSickness"));
		}
	}
}
