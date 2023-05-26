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
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.StringUtils;
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
	// vanillaPotionEffectType: the vanilla potion effect applied by this effect type
	VANILLA_SPEED("VanillaSpeed", "Speed", true, false, PotionEffectType.SPEED),
	VANILLA_SLOW("VanillaSlow", "Slowness", false, false, PotionEffectType.SLOW),
	VANILLA_HASTE("Haste", "Haste", true, false, PotionEffectType.FAST_DIGGING),
	VANILLA_FATIGUE("MiningFatigue", "Mining Fatigue", false, false, PotionEffectType.SLOW_DIGGING),
	VANILLA_JUMP("JumpBoost", "Jump Boost", true, false, PotionEffectType.JUMP),
	VANILLA_FIRE_RESISTANCE("VanillaFireRes", "Fire Immunity", true, true, PotionEffectType.FIRE_RESISTANCE),
	VANILLA_WATER_BREATH("WaterBreath", "Water Breathing", true, true, PotionEffectType.WATER_BREATHING),
	VANILLA_BLINDNESS("Blindness", "Blindness", false, true, PotionEffectType.BLINDNESS),
	VANILLA_NIGHT_VISION("NightVision", "Night Vision", true, true, PotionEffectType.NIGHT_VISION),
	VANILLA_POISON("Poison", "Poison", false, false, PotionEffectType.POISON),
	VANILLA_WITHER("Wither", "Wither", false, false, PotionEffectType.WITHER),
	VANILLA_REGEN("Regeneration", "Regeneration", true, false, PotionEffectType.REGENERATION),
	VANILLA_SATURATION("Saturation", "Saturation", true, false, PotionEffectType.SATURATION),
	VANILLA_GLOW("Glowing", "Glowing", true, true, PotionEffectType.GLOWING),
	VANILLA_SLOWFALL("SlowFalling", "Slow Falling", true, false, PotionEffectType.SLOW_FALLING),
	VANILLA_CONDUIT("ConduitPower", "Conduit Power", true, true, PotionEffectType.CONDUIT_POWER),
	VANILLA_HUNGER("Hunger", "Hunger", false, false, PotionEffectType.HUNGER),
	VANILLA_NAUSEA("Nausea", "Nausea", false, true, PotionEffectType.CONFUSION),
	VANILLA_BADLUCK("BadLuck", "Bad Luck", false, false, PotionEffectType.UNLUCK),

	SPEED("Speed", "Speed", true, false, false),
	SLOW("Slow", "Speed", false, false, false),

	ATTACK_SPEED("AttackSpeed", "Attack Speed", true, false, false),
	NEGATIVE_ATTACK_SPEED("NegativeAttackSpeed", "Attack Speed", false, false, false),

	KNOCKBACK_RESIST("KnockbackResist", "Knockback Resistance", true, false, false),
	NEGATIVE_KNOCKBACK_RESIST("NegativeKnockbackResist", "Knockback Resistance", false, false, false),

	MAX_HEALTH_INCREASE("MaxHealthIncrease", "Max Health", true, false, false),
	MAX_HEALTH_DECREASE("MaxHealthDecrease", "Max Health", false, false, false),

	ABSORPTION("Absorption", "Absorption Health", true, false, false),
	SATURATION("Saturation", "Saturation", true, true, false),
	STARVATION("Starvation", "Starvation", false, true, false),

	//Resistance type of effects
	RESISTANCE("Resistance", "Resistance", true, false, false),
	MELEE_RESISTANCE("MeleeResistance", "Melee Resistance", true, false, false),
	PROJECTILE_RESISTANCE("ProjectileResistance", "Projectile Resistance", true, false, false),
	MAGIC_RESISTANCE("MagicResistance", "Magic Resistance", true, false, false),
	BLAST_RESISTANCE("BlastResistance", "Blast Resistance", true, false, false),
	FIRE_RESISTANCE("FireResistance", "Fire Resistance", true, false, false),
	FALL_RESISTANCE("FallResistance", "Fall Resistance", true, false, false),

	//Damage Negation
	DAMAGE_NEGATE("DamageNegate", "Hits Blocked", true, true, false),
	MELEE_DAMAGE_NEGATE("MeleeDamageNegate", "Melee Hits Blocked", true, true, false),
	PROJECTILE_DAMAGE_NEGATE("ProjectileDamageNegate", "Projectile Hits Blocked", true, true, false),
	MAGIC_DAMAGE_NEGATE("MagicDamageNegate", "Magic Hits Blocked", true, true, false),
	BLAST_DAMAGE_NEGATE("BlastDamageNegate", "Blast Hits Blocked", true, true, false),
	FIRE_DAMAGE_NEGATE("FireDamageNegate", "Fire Hits Blocked", true, true, false),
	FALL_DAMAGE_NEGATE("FallDamageNegate", "Falling Hits Blocked", true, true, false),

	//Vulnerability type of effects
	VULNERABILITY("Vulnerability", "Resistance", false, false, false),
	MELEE_VULNERABILITY("MeleeVulnerability", "Melee Resistance", false, false, false),
	PROJECTILE_VULNERABILITY("ProjectileVulnerability", "Projectile Resistance", false, false, false),
	MAGIC_VULNERABILITY("MagicVulnerability", "Magic Resistance", false, false, false),
	BLAST_VULNERABILITY("BlastVulnerability", "Blast Resistance", false, false, false),
	FIRE_VULNERABILITY("FireVulnerability", "Fire Resistance", false, false, false),
	FALL_VULNERABILITY("FallVulnerability", "Fall Resistance", false, false, false),

	//Damage type of effects
	DAMAGE("damage", "Strength", true, false, false),
	MAGIC_DAMAGE("MagicDamage", "Magic Damage", true, false, false),
	MELEE_DAMAGE("MeleeDamage", "Melee Damage", true, false, false),
	PROJECTILE_DAMAGE("ProjectileDamage", "Projectile Damage", true, false, false),

	//Weakness type of effects
	WEAKNESS("Weakness", "Weakness", false, false, false),
	MAGIC_WEAKNESS("MagicWeakness", "Magic Damage", false, false, false),
	MELEE_WEAKNESS("MeleeWeakness", "Melee Damage", false, false, false),
	PROJECTILE_WEAKNESS("ProjectileWeakness", "Projectile Damage", false, false, false),

	INSTANT_HEALTH("InstantHealthPercent", "Instant Health", true, false, false),
	INSTANT_DAMAGE("InstantDamagePercent", "Instant Damage", false, false, false),

	HEAL("Heal", "Healing Rate", true, false, false),
	ANTI_HEAL("AntiHeal", "Healing Rate", false, false, false),

	ARROW_SAVING("ArrowSaving", "Arrow Save Chance", true, false, false),
	ARROW_LOSS("ArrowSaving", "Arrow Save Chance", false, false, false),

	SOUL_THREAD_BONUS("SoulThreadBonus", "Soul Thread Chance", true, false, false),
	SOUL_THREAD_REDUCTION("SoulThreadReduction", "Soul Thread Chance", false, false, false),

	DURABILITY_SAVE("DurabilitySave", "Durability", true, false, false),
	DURABILITY_LOSS("DurabilityLoss", "Durability", false, false, false),

	EXP_BONUS("ExpBonus", "Experience", true, false, false),
	EXP_LOSS("ExpLoss", "Experience", false, false, false),

	COOLDOWN_DECREASE("AbilityCooldownDecrease", "Ability Cooldowns", true, false, false),
	COOLDOWN_INCREASE("AbilityCooldownIncrease", "Ability Cooldowns", false, false, false),

	BLEED("Bleed", "Bleed", false, false, false),

	STASIS("Stasis", "Stasis", true, false, true),

	BOON_OF_THE_PIT("BoonOfThePit", "Boon of the Pit", true, false, true),
	BOON_OF_SILVER_SCALES("BoonOfSilverScales", "Boon of Silver Scales", true, false, true),
	BOON_OF_KNIGHTLY_PRAYER("BoonOfKnightlyPrayer", "Boon of Knightly Prayer", true, false, true),
	CRYSTALLINE_BLESSING("CrystallineBlessing", "Crystalline Blessing", true, false, true),
	CURSE_OF_THE_DARK_SOUL("DarkSoul", "Curse of the Dark Soul", false, false, true),
	DEEP_GODS_ENDOWMENT("DeepGodsEndowment", "Deep God's Endowment", true, false, true),
	HARRAKFARS_BLESSING("HarrakfarsBlessing", "Harrakfar's Blessing", true, false, true),
	SILVER_PRAYER("SilverPrayer", "Silver Prayer", true, false, true),
	STAR_COMMUNION("StarCommunion", "Star Communion", true, false, true),
	TUATHAN_BLESSING("TuathanBlessing", "Tuathan Blessing", true, false, true);

	public static final String KEY = "Effects";

	private final String mType;
	private final String mName;
	private final boolean mIsPositive;
	private final boolean mIsFlat;
	private final boolean mIsConstant;
	private final @Nullable PotionEffectType mVanillaPotionEffectType;

	EffectType(String type, String name, boolean isPositive, boolean isFlat, boolean isConstant) {
		this(type, name, isPositive, isFlat, isConstant, null);
	}

	EffectType(String type, String name, boolean isPositive, boolean isConstant, PotionEffectType vanillaPotionEffectType) {
		this(type, name, isPositive, true, isConstant, vanillaPotionEffectType);
	}

	EffectType(String type, String name, boolean isPositive, boolean isFlat, boolean isConstant, @Nullable PotionEffectType vanillaPotionEffectType) {
		mType = type;
		mName = name;
		mIsPositive = isPositive;
		mIsFlat = isFlat;
		mIsConstant = isConstant;
		mVanillaPotionEffectType = vanillaPotionEffectType;
	}

	public String getType() {
		return mType;
	}

	public String getName() {
		return mName;
	}

	public boolean isPositive() {
		return mIsPositive;
	}

	public boolean isFlat() {
		return mIsFlat;
	}

	public boolean isConstant() {
		return mIsConstant;
	}

	public @Nullable PotionEffectType getPotionEffectType() {
		return mVanillaPotionEffectType;
	}

	public static boolean isEffectTypeAppliedEffect(@Nullable String source) {
		// Inputs a source, and looks up through all the EffectTypes to check if their source starts with mName.
		// Since Source is registered as:
		// mName + <Source> or
		// mName (if not source),
		// We can determine if a source comes from a EffectType.applyEffect if the source starts with mName.
		if (source == null) {
			return false;
		}

		for (EffectType type : values()) {
			if (source.startsWith(type.getName())) {
				return true;
			}
		}
		return false;
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

		String text;
		boolean includeTime = true;

		if (effectType == EffectType.STARVATION) {
			text = effectType.mName + " " + StringUtils.toRoman((int) strength);
			includeTime = false;
		} else if (effectType.getPotionEffectType() != null) {
			text = effectType.mName + " " + StringUtils.toRoman((int) strength);
		} else if (effectType.getType().contains("Instant")) {
			text = (int) (strength * 100) + "% " + effectType.mName;
			includeTime = false;
		} else if (effectType.isConstant()) {
			text = effectType.mName;
		} else if (effectType.isFlat()) {
			text = add + ((int) strength) + " " + effectType.mName;
		} else {
			text = add + (int) (strength * 100) + "% " + effectType.mName;
		}

		Component component = Component.text(text, TextColor.fromHexString(color)).decoration(TextDecoration.ITALIC, false);
		if (includeTime) {
			int minutes = duration / 1200;
			int seconds = (duration / 20) % 60;
			String timeString = "(" + minutes + ":" + (seconds > 9 ? seconds : "0" + seconds) + ")";
			if (minutes > 999) {
				timeString = "(∞)";
			}
			component = component.append(Component.text(" " + timeString, TextColor.fromHexString("#55555")).decoration(TextDecoration.ITALIC, false));
		}
		return component;
	}

	public static void applyEffect(@Nullable EffectType effectType, LivingEntity entity, int duration, double strength, @Nullable String source, boolean applySickness) {
		if (effectType == null) {
			return;
		}

		Plugin plugin = Plugin.getInstance();

		PotionEffectType potionEffectType = effectType.getPotionEffectType();
		if (potionEffectType != null) {
			PotionEffect potionEffect = new PotionEffect(potionEffectType, duration, (int) strength - 1, true);
			if (entity instanceof Player player) {
				PotionUtils.applyPotion(plugin, player, potionEffect);
			} else {
				entity.addPotionEffect(potionEffect);
			}
			return;
		}

		String sourceString = (source != null ? effectType.mName + source : effectType.mName);

		switch (effectType) {
			case SPEED -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentSpeed(duration, strength, sourceString));
			case SLOW -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentSpeed(duration, -strength, sourceString));

			case ATTACK_SPEED -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentAttackSpeed(duration, strength, sourceString));
			case NEGATIVE_ATTACK_SPEED -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentAttackSpeed(duration, -strength, sourceString));

			case KNOCKBACK_RESIST -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentKnockbackResist(duration, strength, sourceString));
			case NEGATIVE_KNOCKBACK_RESIST -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentKnockbackResist(duration, -strength, sourceString));

			case MAX_HEALTH_INCREASE -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentHealthBoost(duration, strength, sourceString));
			case MAX_HEALTH_DECREASE -> plugin.mEffectManager.addEffect(entity, sourceString, new PercentHealthBoost(duration, -strength, sourceString));

			case ABSORPTION -> {
				double amount = strength * EntityUtils.getMaxHealth(entity);
				AbsorptionUtils.addAbsorption(entity, amount, amount, duration);
				applyAbsorptionSickness(entity, applySickness, plugin);
			}
			case STARVATION -> {
				if (entity instanceof Player player) {
					Starvation.apply(player, (int) strength);
				}
			}

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
				if (entity instanceof Player player) {
					PlayerUtils.healPlayer(plugin, player, EntityUtils.getMaxHealth(entity) * strength);
					applyHealingSickness(entity, applySickness, plugin);
				}
			}
			case INSTANT_DAMAGE -> DamageUtils.damage(null, entity, DamageEvent.DamageType.AILMENT, EntityUtils.getMaxHealth(entity) * strength);

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

	private static void applyHealingSickness(Entity entity, boolean applySickness, Plugin plugin) {
		if (applySickness) {
			double sicknessPenalty = 0;
			NavigableSet<Effect> sicks = plugin.mEffectManager.getEffects(entity, "HealingSickness");
			if (sicks != null) {
				Effect sick = sicks.last();
				sicknessPenalty = sick.getMagnitude();
			}
			plugin.mEffectManager.addEffect(entity, "HealingSickness", new HealingSickness(20 * 15, Math.min(sicknessPenalty + 0.2, 0.8), "HealingSickness"));
		}
	}

	private static void applyAbsorptionSickness(Entity entity, boolean applySickness, Plugin plugin) {
		if (applySickness) {
			double sicknessPenalty = 0;
			NavigableSet<Effect> sicks = plugin.mEffectManager.getEffects(entity, "AbsorptionSickness");
			if (sicks != null) {
				Effect sick = sicks.last();
				sicknessPenalty = sick.getMagnitude();
			}
			plugin.mEffectManager.addEffect(entity, "AbsorptionSickness", new AbsorptionSickness(20 * 15, Math.min(sicknessPenalty + 0.2, 0.8), "AbsorptionSickness"));
		}
	}
}
