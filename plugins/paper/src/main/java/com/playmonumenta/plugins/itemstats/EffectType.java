package com.playmonumenta.plugins.itemstats;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.*;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.enchantments.Starvation;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemupdater.ItemUpdateHelper;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.NavigableSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
	VANILLA_CONDUIT("ConduitPower", "Conduit Power", true, false, PotionEffectType.CONDUIT_POWER),
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
	MELEE_DAMAGE("MeleeDamage", "Melee Damage", true, false, false),
	PROJECTILE_DAMAGE("ProjectileDamage", "Projectile Damage", true, false, false),
	MAGIC_DAMAGE("MagicDamage", "Magic Damage", true, false, false),

	//Weakness type of effects
	WEAKNESS("Weakness", "Strength", false, false, false),
	MELEE_WEAKNESS("MeleeWeakness", "Melee Damage", false, false, false),
	PROJECTILE_WEAKNESS("ProjectileWeakness", "Projectile Damage", false, false, false),
	MAGIC_WEAKNESS("MagicWeakness", "Magic Damage", false, false, false),

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

	FISH_QUALITY_INCREASE("FishQualityIncrease", "Fish Quality", true, false, false),
	FISH_QUALITY_DECREASE("FishQualityDecrease", "FishQuality", false, false, false),

	BLEED("Bleed", "Bleed", false, false, false),

	STASIS("Stasis", "Stasis", true, false, true),
	PARADOX("Paradox", "Paradox", false, false, true),

	BOON_OF_THE_PIT("BoonOfThePit", "Boon of the Pit", true, false, true),
	BOON_OF_SILVER_SCALES("BoonOfSilverScales", "Boon of Silver Scales", true, false, true),
	BOON_OF_KNIGHTLY_PRAYER("BoonOfKnightlyPrayer", "Boon of Knightly Prayer", true, false, true),
	CRYSTALLINE_BLESSING("CrystallineBlessing", "Crystalline Blessing", true, false, true),
	CURSE_OF_THE_DARK_SOUL("DarkSoul", "Curse of the Dark Soul", false, false, true),
	DEEP_GODS_ENDOWMENT("DeepGodsEndowment", "Deep God's Endowment", true, false, true),
	HARRAKFARS_BLESSING("HarrakfarsBlessing", "Harrakfar's Blessing", true, false, true),
	SILVER_PRAYER("SilverPrayer", "Silver Prayer", true, false, true),
	STAR_COMMUNION("StarCommunion", "Star Communion", true, false, true),
	TUATHAN_BLESSING("TuathanBlessing", "Tuathan Blessing", true, false, true),
	GIFT_OF_THE_STARS("GiftOfTheStars", "Gift of the Stars", true, false, true),
	BOON_OF_THE_FRACTURED_TREE("BoonOfTheFracturedTree", "Boon of the Fractured Tree", true, false, true),
	SKY_SEEKERS_GRACE("SkySeekersGrace", "Sky Seeker's Grace", true, false, true),

	CLUCKING("Clucking", "Clucking", false, true, true),

	STEALTH("Stealth", "Stealth", true, true, false),
	;


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
		if (source == null) {
			return false;
		}
		for (EffectType type : values()) {
			if (source.equals(type.getType())) {
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

	public static @Nullable EffectType fromTypeIgnoreCase(String type) {
		for (EffectType effectType : values()) {
			if (effectType.mType.equalsIgnoreCase(type)) {
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
		} else if (effectType.isConstant()) {
			text = effectType.mName;
			if (effectType == CLUCKING) {
				includeTime = false;
			}
		} else if (effectType.getPotionEffectType() != null) {
			text = effectType.mName + " " + StringUtils.toRoman((int) strength);
		} else if (effectType.getType().contains("Instant")) {
			text = (int) (strength * 100) + "% " + effectType.mName;
			includeTime = false;
		} else if (effectType.isFlat()) {
			text = add + ((int) strength) + " " + effectType.mName;
		} else {
			text = add + (int) (strength * 100) + "% " + effectType.mName;
		}

		Component component = Component.text(text, TextColor.fromHexString(color)).decoration(TextDecoration.ITALIC, false);
		if (includeTime) {
			String timeString;
			int minutes = duration / 1200;
			if (minutes > 999 || duration < 0) {
				timeString = "(∞)";
			} else {
				int seconds = (duration / 20) % 60;
				timeString = "(" + minutes + ":" + (seconds > 9 ? seconds : "0" + seconds) + ")";
			}
			component = component.append(Component.text(" " + timeString, TextColor.fromHexString("#555555")).decoration(TextDecoration.ITALIC, false));
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

		// Special cases, does not correspond to vanilla or monumenta effect
		if (effectType == ABSORPTION) {
			double amount = strength * EntityUtils.getMaxHealth(entity);
			AbsorptionUtils.addAbsorption(entity, amount, amount, duration);
			applyAbsorptionSickness(entity, applySickness, plugin);
			return;
		} else if (effectType == STARVATION) {
			if (entity instanceof Player player) {
				Starvation.apply(player, (int) strength);
			}
			return;
		} else if (effectType == INSTANT_HEALTH) {
			double maxHealth = EntityUtils.getMaxHealth(entity);
			if (entity instanceof Player player) {
				PlayerUtils.healPlayer(plugin, player, maxHealth * strength);
				applyHealingSickness(entity, applySickness, plugin);
			} else {
				entity.setHealth(Math.min(maxHealth, entity.getHealth() + maxHealth * strength));
			}
			return;
		} else if (effectType == INSTANT_DAMAGE) {
			if (strength >= 1) {
				entity.setHealth(0);
				return;
			}
			DamageUtils.damage(null, entity, DamageEvent.DamageType.AILMENT, EntityUtils.getMaxHealth(entity) * strength);
			return;
		} else if (effectType == CLUCKING) {
			if (entity instanceof Player player) {
				List<ItemStack> cluckingCandidates = new ArrayList<>(Arrays.asList(player.getInventory().getArmorContents()));
				cluckingCandidates.add(player.getInventory().getItemInOffHand());
				cluckingCandidates.removeIf(item -> item == null || item.getType() == Material.AIR || ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.CLUCKING) > 0);
				if (!cluckingCandidates.isEmpty()) {
					ItemStack item = cluckingCandidates.get(FastUtils.RANDOM.nextInt(cluckingCandidates.size()));
					ItemStatUtils.addEnchantment(item, EnchantmentType.CLUCKING, 1);
					ItemUpdateHelper.generateItemStats(item);
					plugin.mItemStatManager.updateStats(player);
					new PartialParticle(Particle.EXPLOSION_LARGE, entity.getLocation(), 1, 0, 0, 0, 0).minimumCount(1).spawnAsPlayerActive(player);
				}
			}
		} else if (effectType == STEALTH) {
			if (entity instanceof Player player) {
				AbilityUtils.applyStealth(plugin, player, duration);
			}
		}

		String sourceString = source != null ? source : effectType.mType;

		// This could be refactored to include a Supplier<Effect> as an option in the EffectType constructor and make this method much simpler
		Effect effect = switch (effectType) {
			case SPEED -> new PercentSpeed(duration, strength, sourceString);
			case SLOW -> new PercentSpeed(duration, -strength, sourceString);

			case ATTACK_SPEED -> new PercentAttackSpeed(duration, strength, sourceString);
			case NEGATIVE_ATTACK_SPEED -> new PercentAttackSpeed(duration, -strength, sourceString);

			case KNOCKBACK_RESIST -> new PercentKnockbackResist(duration, strength, sourceString);
			case NEGATIVE_KNOCKBACK_RESIST -> new PercentKnockbackResist(duration, -strength, sourceString);

			case MAX_HEALTH_INCREASE -> new PercentHealthBoost(duration, strength, sourceString);
			case MAX_HEALTH_DECREASE -> new PercentHealthBoost(duration, -strength, sourceString);

			case RESISTANCE -> new PercentDamageReceived(duration, -strength);
			case MELEE_RESISTANCE -> new PercentDamageReceived(duration, -strength, DamageEvent.DamageType.getAllMeleeTypes());
			case PROJECTILE_RESISTANCE -> new PercentDamageReceived(duration, -strength, DamageEvent.DamageType.getAllProjectileTypes());
			case MAGIC_RESISTANCE -> new PercentDamageReceived(duration, -strength, EnumSet.of(DamageEvent.DamageType.MAGIC));
			case BLAST_RESISTANCE -> new PercentDamageReceived(duration, -strength, EnumSet.of(DamageEvent.DamageType.BLAST));
			case FIRE_RESISTANCE -> new PercentDamageReceived(duration, -strength, EnumSet.of(DamageEvent.DamageType.FIRE));
			case FALL_RESISTANCE -> new PercentDamageReceived(duration, -strength, EnumSet.of(DamageEvent.DamageType.FALL));

			case DAMAGE_NEGATE -> new NegateDamage(duration, (int) strength);
			case MELEE_DAMAGE_NEGATE -> new NegateDamage(duration, (int) strength, DamageEvent.DamageType.getAllMeleeTypes());
			case PROJECTILE_DAMAGE_NEGATE -> new NegateDamage(duration, (int) strength, DamageEvent.DamageType.getAllProjectileTypes());
			case MAGIC_DAMAGE_NEGATE -> new NegateDamage(duration, (int) strength, EnumSet.of(DamageEvent.DamageType.MAGIC));
			case BLAST_DAMAGE_NEGATE -> new NegateDamage(duration, (int) strength, EnumSet.of(DamageEvent.DamageType.BLAST));
			case FIRE_DAMAGE_NEGATE -> new NegateDamage(duration, (int) strength, EnumSet.of(DamageEvent.DamageType.FIRE));
			case FALL_DAMAGE_NEGATE -> new NegateDamage(duration, (int) strength, EnumSet.of(DamageEvent.DamageType.FALL));

			case VULNERABILITY -> new PercentDamageReceived(duration, strength);
			case MELEE_VULNERABILITY -> new PercentDamageReceived(duration, strength, DamageEvent.DamageType.getAllMeleeTypes());
			case PROJECTILE_VULNERABILITY -> new PercentDamageReceived(duration, strength, DamageEvent.DamageType.getAllProjectileTypes());
			case MAGIC_VULNERABILITY -> new PercentDamageReceived(duration, strength, EnumSet.of(DamageEvent.DamageType.MAGIC));
			case BLAST_VULNERABILITY -> new PercentDamageReceived(duration, strength, EnumSet.of(DamageEvent.DamageType.BLAST));
			case FIRE_VULNERABILITY -> new PercentDamageReceived(duration, strength, EnumSet.of(DamageEvent.DamageType.FIRE));
			case FALL_VULNERABILITY -> new PercentDamageReceived(duration, strength, EnumSet.of(DamageEvent.DamageType.FALL));

			case DAMAGE -> new PercentDamageDealt(duration, strength, DamageEvent.DamageType.getScalableDamageType());
			case MELEE_DAMAGE -> new PercentDamageDealt(duration, strength, DamageEvent.DamageType.getAllMeleeTypes());
			case PROJECTILE_DAMAGE -> new PercentDamageDealt(duration, strength, DamageEvent.DamageType.getAllProjectileTypes());
			case MAGIC_DAMAGE -> new PercentDamageDealt(duration, strength, EnumSet.of(DamageEvent.DamageType.MAGIC));

			case WEAKNESS -> new PercentDamageDealt(duration, -strength);
			case MELEE_WEAKNESS -> new PercentDamageDealt(duration, -strength, DamageEvent.DamageType.getAllMeleeTypes());
			case PROJECTILE_WEAKNESS -> new PercentDamageDealt(duration, -strength, DamageEvent.DamageType.getAllProjectileTypes());
			case MAGIC_WEAKNESS -> new PercentDamageDealt(duration, -strength, EnumSet.of(DamageEvent.DamageType.MAGIC));

			case HEAL -> new PercentHeal(duration, strength);
			case ANTI_HEAL -> new PercentHeal(duration, -strength);

			case ARROW_SAVING -> new ArrowSaving(duration, strength);
			case ARROW_LOSS -> new ArrowSaving(duration, -strength);

			case SOUL_THREAD_BONUS -> new BonusSoulThreads(duration, strength);
			case SOUL_THREAD_REDUCTION -> new BonusSoulThreads(duration, -strength);

			case DURABILITY_SAVE -> new DurabilitySaving(duration, strength);
			case DURABILITY_LOSS -> new DurabilitySaving(duration, -strength);

			case COOLDOWN_DECREASE -> new AbilityCooldownDecrease(duration, strength);
			case COOLDOWN_INCREASE -> new AbilityCooldownIncrease(duration, strength);

			case EXP_BONUS -> new PercentExperience(duration, strength);
			case EXP_LOSS -> new PercentExperience(duration, -strength);

			case FISH_QUALITY_INCREASE -> new FishQualityIncrease(duration, strength);
			case FISH_QUALITY_DECREASE -> new FishQualityIncrease(duration, -strength);

			case BLEED -> new Bleed(duration, strength, plugin);

			case STASIS -> new Stasis(duration);
			case PARADOX -> new TemporalFlux(duration);

			case BOON_OF_THE_PIT -> new BoonOfThePit(duration);
			case BOON_OF_SILVER_SCALES -> new AbilityCooldownDecrease(duration, 0.05);
			case BOON_OF_KNIGHTLY_PRAYER -> new BoonOfKnightlyPrayer(duration);
			case CRYSTALLINE_BLESSING -> new CrystallineBlessing(duration);
			case CURSE_OF_THE_DARK_SOUL -> new PercentDamageReceived(duration, 1);
			case DEEP_GODS_ENDOWMENT -> new DeepGodsEndowment(duration);
			case HARRAKFARS_BLESSING -> new PercentHeal(duration, 0.1);
			case SILVER_PRAYER -> new SilverPrayer(duration);
			case STAR_COMMUNION -> new StarCommunion(duration);
			case TUATHAN_BLESSING -> new TuathanBlessing(duration);
			case GIFT_OF_THE_STARS -> new GiftOfTheStars(duration);
			case BOON_OF_THE_FRACTURED_TREE -> new BoonOfTheFracturedTree(duration);
			case SKY_SEEKERS_GRACE -> new SkySeekersGrace(duration);

			default -> null;
		};

		if (effect == null) {
			MMLog.warning("No EffectType implemented in applyEffect(..) for: " + effectType.mType);
			return;
		}

		plugin.mEffectManager.addEffect(entity, sourceString, effect);
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
