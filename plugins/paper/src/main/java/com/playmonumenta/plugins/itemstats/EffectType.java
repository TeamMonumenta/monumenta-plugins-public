package com.playmonumenta.plugins.itemstats;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.*;
import com.playmonumenta.plugins.effects.hexfall.CreepingDeath;
import com.playmonumenta.plugins.effects.hexfall.DeathImmunity;
import com.playmonumenta.plugins.effects.hexfall.DeathVulnerability;
import com.playmonumenta.plugins.effects.hexfall.InfusedLife;
import com.playmonumenta.plugins.effects.hexfall.LifeImmunity;
import com.playmonumenta.plugins.effects.hexfall.LifeVulnerability;
import com.playmonumenta.plugins.effects.hexfall.Reincarnation;
import com.playmonumenta.plugins.effects.hexfall.VoodooBindings;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.enchantments.Starvation;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemupdater.ItemUpdateHelper;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
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
	VANILLA_DARKNESS("Darkness", "Darkness", false, true, PotionEffectType.DARKNESS),
	VANILLA_LEVITATION("Levitation", "Levitation", false, false, PotionEffectType.LEVITATION),
	VANILLA_DOLPHINS_GRACE("DolphinsGrace", "Dolphin's Grace", true, true, PotionEffectType.DOLPHINS_GRACE),

	SPEED("Speed", "Speed", true, false, false, pluginApplicator(PercentSpeed::new)),
	SLOW("Slow", "Speed", false, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentSpeed(duration, -strength, source)
	)),

	ATTACK_SPEED("AttackSpeed", "Attack Speed", true, false, false, pluginApplicator(PercentAttackSpeed::new)),
	NEGATIVE_ATTACK_SPEED("NegativeAttackSpeed", "Attack Speed", false, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentAttackSpeed(duration, -strength, source)
	)),

	KNOCKBACK_RESIST("KnockbackResist", "Knockback Resistance", true, false, false, pluginApplicator(PercentKnockbackResist::new)),
	NEGATIVE_KNOCKBACK_RESIST("NegativeKnockbackResist", "Knockback Resistance", false, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentKnockbackResist(duration, -strength, source)
	)),

	MAX_HEALTH_INCREASE("MaxHealthIncrease", "Max Health", true, false, false, pluginApplicator(PercentHealthBoost::new)),
	MAX_HEALTH_DECREASE("MaxHealthDecrease", "Max Health", false, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentHealthBoost(duration, -strength, source)
	)),

	ABSORPTION("Absorption", "Absorption Health", true, false, false,
		(effectType, entity, duration, strength, source, applySickness) -> {
			double amount = strength * EntityUtils.getMaxHealth(entity);
			AbsorptionUtils.addAbsorption(entity, amount, amount, duration);
			applyAbsorptionSickness(entity, applySickness, Plugin.getInstance());
		}),
	SATURATION("Saturation", "Saturation", true, true, PotionEffectType.SATURATION),
	STARVATION("Starvation", "Starvation", false, true, false,
		(effectType, entity, duration, strength, source, applySickness) -> {
			if (entity instanceof Player player) {
				Starvation.apply(player, (int) strength);
			}
		}),

	//Resistance type of effects
	RESISTANCE("Resistance", "Resistance", true, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentDamageReceived(duration, -strength)
	)),
	MELEE_RESISTANCE("MeleeResistance", "Melee Resistance", true, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentDamageReceived(duration, -strength, DamageEvent.DamageType.getAllMeleeTypes())
	)),
	PROJECTILE_RESISTANCE("ProjectileResistance", "Projectile Resistance", true, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentDamageReceived(duration, -strength, DamageEvent.DamageType.getAllProjectileTypes())
	)),
	MAGIC_RESISTANCE("MagicResistance", "Magic Resistance", true, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentDamageReceived(duration, -strength, EnumSet.of(DamageEvent.DamageType.MAGIC))
	)),
	BLAST_RESISTANCE("BlastResistance", "Blast Resistance", true, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentDamageReceived(duration, -strength, EnumSet.of(DamageEvent.DamageType.BLAST))
	)),
	FIRE_RESISTANCE("FireResistance", "Fire Resistance", true, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentDamageReceived(duration, -strength, EnumSet.of(DamageEvent.DamageType.FIRE))
	)),
	FALL_RESISTANCE("FallResistance", "Fall Resistance", true, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentDamageReceived(duration, -strength, EnumSet.of(DamageEvent.DamageType.FALL))
	)),

	//Damage Negation
	DAMAGE_NEGATE("DamageNegate", "Hits Blocked", true, true, false, pluginApplicator(
		(duration, strength, source) -> new NegateDamage(duration, (int) strength)
	)),
	MELEE_DAMAGE_NEGATE("MeleeDamageNegate", "Melee Hits Blocked", true, true, false, pluginApplicator(
		(duration, strength, source) -> new NegateDamage(duration, (int) strength, DamageEvent.DamageType.getAllMeleeTypes())
	)),
	PROJECTILE_DAMAGE_NEGATE("ProjectileDamageNegate", "Projectile Hits Blocked", true, true, false, pluginApplicator(
		(duration, strength, source) -> new NegateDamage(duration, (int) strength, DamageEvent.DamageType.getAllProjectileTypes())
	)),
	MAGIC_DAMAGE_NEGATE("MagicDamageNegate", "Magic Hits Blocked", true, true, false, pluginApplicator(
		(duration, strength, source) -> new NegateDamage(duration, (int) strength, EnumSet.of(DamageEvent.DamageType.MAGIC))
	)),
	BLAST_DAMAGE_NEGATE("BlastDamageNegate", "Blast Hits Blocked", true, true, false, pluginApplicator(
		(duration, strength, source) -> new NegateDamage(duration, (int) strength, EnumSet.of(DamageEvent.DamageType.BLAST))
	)),
	FIRE_DAMAGE_NEGATE("FireDamageNegate", "Fire Hits Blocked", true, true, false, pluginApplicator(
		(duration, strength, source) -> new NegateDamage(duration, (int) strength, EnumSet.of(DamageEvent.DamageType.FIRE))
	)),
	FALL_DAMAGE_NEGATE("FallDamageNegate", "Falling Hits Blocked", true, true, false, pluginApplicator(
		(duration, strength, source) -> new NegateDamage(duration, (int) strength, EnumSet.of(DamageEvent.DamageType.FALL))
	)),

	//Vulnerability type of effects
	VULNERABILITY("Vulnerability", "Resistance", false, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentDamageReceived(duration, strength)
	)),
	MELEE_VULNERABILITY("MeleeVulnerability", "Melee Resistance", false, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentDamageReceived(duration, strength, DamageEvent.DamageType.getAllMeleeTypes())
	)),
	PROJECTILE_VULNERABILITY("ProjectileVulnerability", "Projectile Resistance", false, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentDamageReceived(duration, strength, DamageEvent.DamageType.getAllProjectileTypes())
	)),
	MAGIC_VULNERABILITY("MagicVulnerability", "Magic Resistance", false, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentDamageReceived(duration, strength, EnumSet.of(DamageEvent.DamageType.MAGIC))
	)),
	BLAST_VULNERABILITY("BlastVulnerability", "Blast Resistance", false, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentDamageReceived(duration, strength, EnumSet.of(DamageEvent.DamageType.BLAST))
	)),
	FIRE_VULNERABILITY("FireVulnerability", "Fire Resistance", false, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentDamageReceived(duration, strength, EnumSet.of(DamageEvent.DamageType.FIRE))
	)),
	FALL_VULNERABILITY("FallVulnerability", "Fall Resistance", false, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentDamageReceived(duration, strength, EnumSet.of(DamageEvent.DamageType.FALL))
	)),

	//Damage type of effects
	DAMAGE("damage", "Strength", true, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentDamageDealt(duration, strength).damageTypes(DamageEvent.DamageType.getScalableDamageType())
	)),
	MELEE_DAMAGE("MeleeDamage", "Melee Damage", true, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentDamageDealt(duration, strength).damageTypes(DamageEvent.DamageType.getAllMeleeTypes())
	)),
	PROJECTILE_DAMAGE("ProjectileDamage", "Projectile Damage", true, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentDamageDealt(duration, strength).damageTypes(DamageEvent.DamageType.getAllProjectileTypes())
	)),
	MAGIC_DAMAGE("MagicDamage", "Magic Damage", true, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentDamageDealt(duration, strength).damageTypes(EnumSet.of(DamageEvent.DamageType.MAGIC))
	)),

	//Weakness type of effects
	WEAKNESS("Weakness", "Strength", false, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentDamageDealt(duration, -strength)
	)),
	MELEE_WEAKNESS("MeleeWeakness", "Melee Damage", false, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentDamageDealt(duration, -strength).damageTypes(DamageEvent.DamageType.getAllMeleeTypes())
	)),
	PROJECTILE_WEAKNESS("ProjectileWeakness", "Projectile Damage", false, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentDamageDealt(duration, -strength).damageTypes(DamageEvent.DamageType.getAllProjectileTypes())
	)),
	MAGIC_WEAKNESS("MagicWeakness", "Magic Damage", false, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentDamageDealt(duration, -strength).damageTypes(EnumSet.of(DamageEvent.DamageType.MAGIC))
	)),

	INSTANT_HEALTH("InstantHealthPercent", "Instant Health", true, false, false,
		(effectType, entity, duration, strength, source, applySickness) -> {
			Plugin plugin = Plugin.getInstance();
			double maxHealth = EntityUtils.getMaxHealth(entity);
			if (entity instanceof Player player) {
				PlayerUtils.healPlayer(plugin, player, maxHealth * strength);
				applyHealingSickness(entity, applySickness, plugin);
			} else {
				EntityUtils.healMob(entity, maxHealth * strength);
			}
		}),
	INSTANT_DAMAGE("InstantDamagePercent", "Instant Damage", false, false, false,
		(effectType, entity, duration, strength, source, applySickness) -> {
			if (strength >= 1) {
				entity.setHealth(0);
				return;
			}
			if (!ZoneUtils.hasZoneProperty(entity, ZoneProperty.RESIST_5)) {
				DamageUtils.damage(null, entity, DamageEvent.DamageType.AILMENT, EntityUtils.getMaxHealth(entity) * strength);
			}
		}),

	HEAL("Heal", "Healing Rate", true, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentHeal(duration, strength)
	)),
	ANTI_HEAL("AntiHeal", "Healing Rate", false, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentHeal(duration, -strength)
	)),

	ARROW_SAVING("ArrowSaving", "Arrow Save Chance", true, false, false, pluginApplicator(
		(duration, strength, source) -> new ArrowSaving(duration, strength)
	)),
	ARROW_LOSS("ArrowSaving", "Arrow Save Chance", false, false, false, pluginApplicator(
		(duration, strength, source) -> new ArrowSaving(duration, -strength)
	)),

	SOUL_THREAD_BONUS("SoulThreadBonus", "Soul Thread Chance", true, false, false, pluginApplicator(
		(duration, strength, source) -> new BonusSoulThreads(duration, strength)
	)),
	SOUL_THREAD_REDUCTION("SoulThreadReduction", "Soul Thread Chance", false, false, false, pluginApplicator(
		(duration, strength, source) -> new BonusSoulThreads(duration, -strength)
	)),

	DURABILITY_SAVE("DurabilitySave", "Durability", true, false, false, pluginApplicator(
		(duration, strength, source) -> new DurabilitySaving(duration, strength)
	)),
	DURABILITY_LOSS("DurabilityLoss", "Durability", false, false, false, pluginApplicator(
		(duration, strength, source) -> new DurabilitySaving(duration, -strength)
	)),

	EXP_BONUS("ExpBonus", "Experience", true, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentExperience(duration, strength)
	)),
	EXP_LOSS("ExpLoss", "Experience", false, false, false, pluginApplicator(
		(duration, strength, source) -> new PercentExperience(duration, -strength)
	)),

	COOLDOWN_DECREASE("AbilityCooldownDecrease", "Ability Cooldowns", true, false, false, pluginApplicator(
		(duration, strength, source) -> new AbilityCooldownDecrease(duration, strength)
	)),
	COOLDOWN_INCREASE("AbilityCooldownIncrease", "Ability Cooldowns", false, false, false, pluginApplicator(
		(duration, strength, source) -> new AbilityCooldownIncrease(duration, strength)
	)),

	FISH_QUALITY_INCREASE("FishQualityIncrease", "Fish Quality", true, false, false, pluginApplicator(
		(duration, strength, source) -> new FishQualityIncrease(duration, strength)
	)),
	FISH_QUALITY_DECREASE("FishQualityDecrease", "FishQuality", false, false, false, pluginApplicator(
		(duration, strength, source) -> new FishQualityIncrease(duration, -strength)
	)),

	STASIS("Stasis", "Stasis", true, false, true, pluginApplicator(
		(duration, strength, source) -> new Stasis(duration))),
	SILENCE("Silence", "Silence", false, false, true,
		(effectType, entity, duration, strength, source, applySickness) -> {
			if (entity instanceof Player player) {
				AbilityUtils.silencePlayer(player, duration);
			} else {
				EntityUtils.applySilence(Plugin.getInstance(), duration, entity);
			}
		}),
	PARADOX("Paradox", "Paradox", false, true, true, pluginApplicator(
		(duration, strength, source) -> new TemporalFlux(duration)
	)),
	INFUSED_LIFE("InfusedLife", "Infusion of Life", false, true, true, pluginApplicator(
		(duration, strength, source) -> new InfusedLife(duration)
	)),
	REINCARNATION("Reincarnation", "Reincarnation", false, false, true, pluginApplicator(
		(duration, strength, source) -> new Reincarnation(duration, strength)
	)),
	VOODOO_BINDINGS("VooodooBindings", "Voodoo Bindings", false, false, true, pluginApplicator(
		(duration, strength, source) -> new VoodooBindings(duration)
	)),
	LIFE_VULNERABILITY("LifeVulnerability", "Life Vulnerability", false, false, true, pluginApplicator(
		(duration, strength, source) -> new LifeVulnerability(duration)
	)),
	DEATH_VULNERABILITY("DeathVulnerability", "Death Vulnerability", false, false, true, pluginApplicator(
		(duration, strength, source) -> new DeathVulnerability(duration)
	)),
	CREEPING_DEATH("CreepingDeath", "Creeping Death", false, false, true, pluginApplicator(
		(duration, strength, source) -> new CreepingDeath(duration, Plugin.getInstance())
	)),
	LIFE_IMMUNITY("LifeImmortality", "Life Immortality", false, false, false, pluginApplicator(
		(duration, strength, source) -> new LifeImmunity(duration)
	)),
	DEATH_IMMUNITY("DeathImmortality", "Death Immortality", false, false, false, pluginApplicator(
		(duration, strength, source) -> new DeathImmunity(duration)
	)),
	PARASITES("Parasites", "Parasites", false, false, true, pluginApplicator(
		(duration, strength, source) -> new Parasites(duration)
	)),

	BOON_OF_THE_PIT("BoonOfThePit", "Boon of the Pit", true, false, true, pluginApplicator(
		(duration, strength, source) -> new BoonOfThePit(duration)
	)),
	BOON_OF_SILVER_SCALES("BoonOfSilverScales", "Boon of Silver Scales", true, false, true, pluginApplicator(
		(duration, strength, source) -> new AbilityCooldownDecrease(duration, 0.05)
	)),
	BOON_OF_KNIGHTLY_PRAYER("BoonOfKnightlyPrayer", "Boon of Knightly Prayer", true, false, true, pluginApplicator(
		(duration, strength, source) -> new BoonOfKnightlyPrayer(duration)
	)),
	CRYSTALLINE_BLESSING("CrystallineBlessing", "Crystalline Blessing", true, false, true, pluginApplicator(
		(duration, strength, source) -> new CrystallineBlessing(duration)
	)),
	CURSE_OF_THE_DARK_SOUL("DarkSoul", "Curse of the Dark Soul", false, false, true, pluginApplicator(
		(duration, strength, source) -> new PercentDamageReceived(duration, 1)
	)),
	DEEP_GODS_ENDOWMENT("DeepGodsEndowment", "Deep God's Endowment", true, false, true, pluginApplicator(
		(duration, strength, source) -> new DeepGodsEndowment(duration)
	)),
	HARRAKFARS_BLESSING("HarrakfarsBlessing", "Harrakfar's Blessing", true, false, true, pluginApplicator(
		(duration, strength, source) -> new PercentHeal(duration, 0.1)
	)),
	SILVER_PRAYER("SilverPrayer", "Silver Prayer", true, false, true, pluginApplicator(
		(duration, strength, source) -> new SilverPrayer(duration)
	)),
	STAR_COMMUNION("StarCommunion", "Star Communion", true, false, true, pluginApplicator(
		(duration, strength, source) -> new StarCommunion(duration)
	)),
	TUATHAN_BLESSING("TuathanBlessing", "Tuathan Blessing", true, false, true, pluginApplicator(
		(duration, strength, source) -> new TuathanBlessing(duration)
	)),
	GIFT_OF_THE_STARS("GiftOfTheStars", "Gift of the Stars", true, false, true, pluginApplicator(
		(duration, strength, source) -> new GiftOfTheStars(duration)
	)),
	BOON_OF_THE_FRACTURED_TREE("BoonOfTheFracturedTree", "Boon of the Fractured Tree", true, false, true, pluginApplicator(
		(duration, strength, source) -> new BoonOfTheFracturedTree(duration)
	)),
	SKY_SEEKERS_GRACE("SkySeekersGrace", "Sky Seeker's Grace", true, false, true, pluginApplicator(
		(duration, strength, source) -> new SkySeekersGrace(duration)
	)),

	POISON_IMMUNITY("PoisonImmunity", "Poison Immunity", true, true, false,
		(effectType, entity, duration, strength, source, applySickness) -> {
			if (entity instanceof Player player) {
				Plugin.getInstance().mPotionManager.removeLowerPotions(player, PotionManager.PotionID.APPLIED_POTION, PotionEffectType.POISON, (int) strength);
				PotionEffect potionEffect = player.getPotionEffect(PotionEffectType.POISON);
				if (potionEffect != null && potionEffect.getAmplifier() < strength) {
					player.removePotionEffect(PotionEffectType.POISON);
				}
				addEffect(player, source, new PoisonImmunity(duration, strength));
			}
		}),

	CLUCKING("Clucking", "Clucking", false, true, true,
		(effectType, entity, duration, strength, source, applySickness) -> {
			Plugin plugin = Plugin.getInstance();
			if (entity instanceof Player player) {
				if (plugin.mItemStatManager.getPlayerItemStats(player).getItemStats().get(EnchantmentType.CLUCKING) > 0) {
					return;
				}
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
		}),

	STEALTH("Stealth", "Stealth", true, true, false,
		(effectType, entity, duration, strength, source, applySickness) -> {
			if (entity instanceof Player player) {
				AbilityUtils.applyStealth(Plugin.getInstance(), player, duration);
			}
		}),
	;

	public EffectApplicator getEffectApplicator() {
		return mEffectApplicator;
	}

	@FunctionalInterface
	public interface EffectApplicator {
		void apply(EffectType effectType, LivingEntity entity, int duration, double strength, String source, boolean applySickness);
	}

	@FunctionalInterface
	interface PluginEffectConstructor {
		Effect construct(int duration, double strength, String source);
	}

	public static final String KEY = "Effects";

	private final String mType;
	private final String mName;
	private final boolean mIsPositive;
	private final boolean mIsFlat;
	private final boolean mIsConstant;
	private final @Nullable PotionEffectType mVanillaPotionEffectType;
	private final EffectApplicator mEffectApplicator;

	EffectType(String type, String name, boolean isPositive, boolean isFlat, boolean isConstant, EffectApplicator effectApplicator) {
		this(type, name, isPositive, isFlat, isConstant, null, effectApplicator);
	}

	EffectType(String type, String name, boolean isPositive, boolean isConstant, PotionEffectType vanillaPotionEffectType) {
		this(type, name, isPositive, true, isConstant, vanillaPotionEffectType,
			(effectType, entity, duration, strength, source, applySickness) -> {
				PotionEffect potionEffect = new PotionEffect(vanillaPotionEffectType, duration, (int) strength - 1, true);
				if (entity instanceof Player player) {
					PotionUtils.applyPotion(Plugin.getInstance(), player, potionEffect);
				} else {
					entity.addPotionEffect(potionEffect);
				}
			});
	}

	/**
	 * Effect Type enum, for consumables and EffectsList.
	 *
	 * @param type                    : is the unique key save inside the nbt of the item
	 * @param name                    : is the name that the player will see on the item -> format:  +/-dd% name (x:yy)
	 * @param isPositive              : if the display should be blue (positive) or red (negative)
	 * @param isFlat                  : not a percentage (true) or percentage (false)
	 * @param isConstant              : does it have a number associated?
	 * @param vanillaPotionEffectType : the vanilla potion effect applied by this effect type
	 * @param effectApplicator        : the function for granting the effect to the target
	 */
	EffectType(String type, String name, boolean isPositive, boolean isFlat, boolean isConstant, @Nullable PotionEffectType vanillaPotionEffectType, EffectApplicator effectApplicator) {
		mType = type;
		mName = name;
		mIsPositive = isPositive;
		mIsFlat = isFlat;
		mIsConstant = isConstant;
		mVanillaPotionEffectType = vanillaPotionEffectType;
		mEffectApplicator = effectApplicator;
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

	private static void addEffect(Entity target, String source, Effect effect) {
		EffectManager.getInstance().addEffect(target, source, effect);
	}

	private static EffectApplicator pluginApplicator(PluginEffectConstructor effect) {
		return (effectType, entity, duration, strength, source, applySickness) ->
			addEffect(entity, source, effect.construct(duration, strength, source));
	}

	private static String notNullString(EffectType effectType, @Nullable String source) {
		return source != null ? source : effectType.mType;
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

		if (effectType == STARVATION) {
			text = effectType.mName + " " + StringUtils.toRoman((int) strength);
			includeTime = false;
		} else if (effectType.isConstant()) {
			text = effectType.mName;
			if (effectType == CLUCKING) {
				includeTime = false;
			}
		} else if (effectType.getPotionEffectType() != null || effectType == POISON_IMMUNITY) {
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

		effectType.getEffectApplicator().apply(effectType, entity, duration, strength, notNullString(effectType, source), applySickness);
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
