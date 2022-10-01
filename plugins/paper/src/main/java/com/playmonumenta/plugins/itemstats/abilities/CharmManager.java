package com.playmonumenta.plugins.itemstats.abilities;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.alchemist.AlchemicalArtillery;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.abilities.alchemist.Bezoar;
import com.playmonumenta.plugins.abilities.alchemist.BrutalAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.EmpoweringOdor;
import com.playmonumenta.plugins.abilities.alchemist.EnergizingElixir;
import com.playmonumenta.plugins.abilities.alchemist.GruesomeAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.IronTincture;
import com.playmonumenta.plugins.abilities.alchemist.UnstableAmalgam;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.Panacea;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.TransmutationRing;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.WardingRemedy;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.EsotericEnhancements;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.ScorchedEarth;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.Taboo;
import com.playmonumenta.plugins.abilities.cleric.CelestialBlessing;
import com.playmonumenta.plugins.abilities.cleric.CleansingRain;
import com.playmonumenta.plugins.abilities.cleric.DivineJustice;
import com.playmonumenta.plugins.abilities.cleric.HandOfLight;
import com.playmonumenta.plugins.abilities.cleric.HeavenlyBoon;
import com.playmonumenta.plugins.abilities.cleric.Rejuvenation;
import com.playmonumenta.plugins.abilities.cleric.SacredProvisions;
import com.playmonumenta.plugins.abilities.cleric.SanctifiedArmor;
import com.playmonumenta.plugins.abilities.cleric.hierophant.EnchantedPrayer;
import com.playmonumenta.plugins.abilities.cleric.hierophant.HallowedBeam;
import com.playmonumenta.plugins.abilities.cleric.hierophant.ThuribleProcession;
import com.playmonumenta.plugins.abilities.cleric.paladin.ChoirBells;
import com.playmonumenta.plugins.abilities.cleric.paladin.HolyJavelin;
import com.playmonumenta.plugins.abilities.cleric.paladin.LuminousInfusion;
import com.playmonumenta.plugins.abilities.mage.ArcaneStrike;
import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.abilities.mage.FrostNova;
import com.playmonumenta.plugins.abilities.mage.MagmaShield;
import com.playmonumenta.plugins.abilities.mage.ManaLance;
import com.playmonumenta.plugins.abilities.mage.PrismaticShield;
import com.playmonumenta.plugins.abilities.mage.Spellshock;
import com.playmonumenta.plugins.abilities.mage.ThunderStep;
import com.playmonumenta.plugins.abilities.mage.arcanist.AstralOmen;
import com.playmonumenta.plugins.abilities.mage.arcanist.CosmicMoonblade;
import com.playmonumenta.plugins.abilities.mage.arcanist.SagesInsight;
import com.playmonumenta.plugins.abilities.mage.elementalist.Blizzard;
import com.playmonumenta.plugins.abilities.mage.elementalist.ElementalSpiritFire;
import com.playmonumenta.plugins.abilities.mage.elementalist.Starfall;
import com.playmonumenta.plugins.abilities.rogue.AdvancingShadows;
import com.playmonumenta.plugins.abilities.rogue.ByMyBlade;
import com.playmonumenta.plugins.abilities.rogue.DaggerThrow;
import com.playmonumenta.plugins.abilities.rogue.EscapeDeath;
import com.playmonumenta.plugins.abilities.rogue.Skirmisher;
import com.playmonumenta.plugins.abilities.rogue.Smokescreen;
import com.playmonumenta.plugins.abilities.rogue.ViciousCombos;
import com.playmonumenta.plugins.abilities.rogue.assassin.BodkinBlitz;
import com.playmonumenta.plugins.abilities.rogue.assassin.CloakAndDagger;
import com.playmonumenta.plugins.abilities.rogue.assassin.CoupDeGrace;
import com.playmonumenta.plugins.abilities.rogue.swordsage.BladeDance;
import com.playmonumenta.plugins.abilities.rogue.swordsage.DeadlyRonde;
import com.playmonumenta.plugins.abilities.rogue.swordsage.WindWalk;
import com.playmonumenta.plugins.abilities.scout.EagleEye;
import com.playmonumenta.plugins.abilities.scout.HuntingCompanion;
import com.playmonumenta.plugins.abilities.scout.Sharpshooter;
import com.playmonumenta.plugins.abilities.scout.SwiftCuts;
import com.playmonumenta.plugins.abilities.scout.Swiftness;
import com.playmonumenta.plugins.abilities.scout.Volley;
import com.playmonumenta.plugins.abilities.scout.WindBomb;
import com.playmonumenta.plugins.abilities.scout.hunter.PinningShot;
import com.playmonumenta.plugins.abilities.scout.hunter.PredatorStrike;
import com.playmonumenta.plugins.abilities.scout.hunter.SplitArrow;
import com.playmonumenta.plugins.abilities.scout.ranger.Quickdraw;
import com.playmonumenta.plugins.abilities.scout.ranger.TacticalManeuver;
import com.playmonumenta.plugins.abilities.scout.ranger.WhirlingBlade;
import com.playmonumenta.plugins.abilities.warlock.AmplifyingHex;
import com.playmonumenta.plugins.abilities.warlock.CholericFlames;
import com.playmonumenta.plugins.abilities.warlock.CursedWound;
import com.playmonumenta.plugins.abilities.warlock.GraspingClaws;
import com.playmonumenta.plugins.abilities.warlock.MelancholicLament;
import com.playmonumenta.plugins.abilities.warlock.PhlegmaticResolve;
import com.playmonumenta.plugins.abilities.warlock.SanguineHarvest;
import com.playmonumenta.plugins.abilities.warlock.SoulRend;
import com.playmonumenta.plugins.abilities.warlock.reaper.DarkPact;
import com.playmonumenta.plugins.abilities.warlock.reaper.JudgementChain;
import com.playmonumenta.plugins.abilities.warlock.reaper.VoodooBonds;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.HauntingShades;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.RestlessSouls;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.WitheringGaze;
import com.playmonumenta.plugins.abilities.warrior.BruteForce;
import com.playmonumenta.plugins.abilities.warrior.CounterStrike;
import com.playmonumenta.plugins.abilities.warrior.DefensiveLine;
import com.playmonumenta.plugins.abilities.warrior.Frenzy;
import com.playmonumenta.plugins.abilities.warrior.Riposte;
import com.playmonumenta.plugins.abilities.warrior.ShieldBash;
import com.playmonumenta.plugins.abilities.warrior.Toughness;
import com.playmonumenta.plugins.abilities.warrior.WeaponMastery;
import com.playmonumenta.plugins.abilities.warrior.berserker.GloriousBattle;
import com.playmonumenta.plugins.abilities.warrior.berserker.MeteorSlam;
import com.playmonumenta.plugins.abilities.warrior.berserker.Rampage;
import com.playmonumenta.plugins.abilities.warrior.guardian.Bodyguard;
import com.playmonumenta.plugins.abilities.warrior.guardian.Challenge;
import com.playmonumenta.plugins.abilities.warrior.guardian.ShieldWall;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.enchantments.Abyssal;
import com.playmonumenta.plugins.itemstats.enchantments.Adrenaline;
import com.playmonumenta.plugins.itemstats.enchantments.ArcaneThrust;
import com.playmonumenta.plugins.itemstats.enchantments.Decay;
import com.playmonumenta.plugins.itemstats.enchantments.Duelist;
import com.playmonumenta.plugins.itemstats.enchantments.Eruption;
import com.playmonumenta.plugins.itemstats.enchantments.HexEater;
import com.playmonumenta.plugins.itemstats.enchantments.IceAspect;
import com.playmonumenta.plugins.itemstats.enchantments.Inferno;
import com.playmonumenta.plugins.itemstats.enchantments.IntoxicatingWarmth;
import com.playmonumenta.plugins.itemstats.enchantments.JunglesNourishment;
import com.playmonumenta.plugins.itemstats.enchantments.LifeDrain;
import com.playmonumenta.plugins.itemstats.enchantments.LiquidCourage;
import com.playmonumenta.plugins.itemstats.enchantments.PointBlank;
import com.playmonumenta.plugins.itemstats.enchantments.Quake;
import com.playmonumenta.plugins.itemstats.enchantments.RageOfTheKeter;
import com.playmonumenta.plugins.itemstats.enchantments.Regeneration;
import com.playmonumenta.plugins.itemstats.enchantments.Regicide;
import com.playmonumenta.plugins.itemstats.enchantments.Retrieval;
import com.playmonumenta.plugins.itemstats.enchantments.Sapper;
import com.playmonumenta.plugins.itemstats.enchantments.SecondWind;
import com.playmonumenta.plugins.itemstats.enchantments.Slayer;
import com.playmonumenta.plugins.itemstats.enchantments.Smite;
import com.playmonumenta.plugins.itemstats.enchantments.Sniper;
import com.playmonumenta.plugins.itemstats.enchantments.TemporalBender;
import com.playmonumenta.plugins.itemstats.enchantments.ThunderAspect;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class CharmManager {

	public static final String KEY_PLUGIN_DATA = "R3Charms";
	public static final String KEY_CHARMS = "charms";
	public static final String KEY_ITEM = "item";
	public static final int MAX_CHARM_COUNT = 7;
	private static final String CHARM_EFFECT_NAME = "CHARM EFFECT";

	public static CharmManager mInstance;

	public List<String> mCharmEffectList;

	public List<String> mFlippedColorEffectSubstrings;

	public Map<UUID, List<ItemStack>> mPlayerCharms;

	public Map<UUID, Map<String, Double>> mPlayerCharmEffectMap;

	public Map<UUID, Multimap<ClassAbility, Effect>> mPlayerAbilityEffectMap;

	private CharmManager() {
		mPlayerCharms = new HashMap<>();
		mPlayerCharmEffectMap = new HashMap<UUID, Map<String, Double>>();
		mPlayerAbilityEffectMap = new HashMap<>();
		loadCharmEffects();
		loadFlippedColorEffects();
	}

	public static CharmManager getInstance() {
		if (mInstance == null) {
			mInstance = new CharmManager();
		}
		return mInstance;
	}

	// Adding new Charm Effects:
	// Add the string in this list, with other effects from the same ability/enchantment
	// If it is a "debuff" (i.e. a greater number is worse), then ALSO list it in the next method
	public void loadCharmEffects() {
		mCharmEffectList = Arrays.asList(
			// Custom Enchantments
			Inferno.CHARM_DAMAGE,
			ThunderAspect.CHARM_STUN_CHANCE,
			IceAspect.CHARM_DURATION,
			IceAspect.CHARM_SLOW,
			Decay.CHARM_DAMAGE,
			Decay.CHARM_DURATION,
			Sapper.CHARM_HEAL,
			HexEater.CHARM_DAMAGE,
			LifeDrain.CHARM_HEAL,
			Retrieval.CHARM_CHANCE,
			Regicide.CHARM_DAMAGE,
			SecondWind.CHARM_RESISTANCE,
			SecondWind.CHARM_THRESHOLD,
			Sniper.CHARM_DAMAGE,
			PointBlank.CHARM_DAMAGE,
			Adrenaline.CHARM_SPEED,
			Quake.CHARM_DAMAGE,
			Quake.CHARM_RADIUS,
			JunglesNourishment.CHARM_COOLDOWN,
			JunglesNourishment.CHARM_HEALTH,
			RageOfTheKeter.CHARM_COOLDOWN,
			RageOfTheKeter.CHARM_DAMAGE,
			RageOfTheKeter.CHARM_SPEED,
			IntoxicatingWarmth.CHARM_COOLDOWN,
			IntoxicatingWarmth.CHARM_DURATION,
			IntoxicatingWarmth.CHARM_SATURATION,
			TemporalBender.CHARM_COOLDOWN,
			TemporalBender.CHARM_COOLDOWN_REDUCTION,
			LiquidCourage.CHARM_CHARGES,
			LiquidCourage.CHARM_DURATION,
			LiquidCourage.CHARM_COOLDOWN,
			LiquidCourage.CHARM_RESISTANCE,
			Regeneration.CHARM_HEAL,
			Smite.CHARM_DAMAGE,
			Slayer.CHARM_DAMAGE,
			Duelist.CHARM_DAMAGE,
			ArcaneThrust.CHARM_DAMAGE,
			ArcaneThrust.CHARM_KNOCKBACK,
			Abyssal.CHARM_DAMAGE,
			Eruption.CHARM_DAMAGE,
			Eruption.CHARM_RADIUS,
			// Classes
			// Mage
			ManaLance.CHARM_DAMAGE,
			ManaLance.CHARM_CHARGES,
			ManaLance.CHARM_COOLDOWN,
			ManaLance.CHARM_RANGE,
			ThunderStep.CHARM_DAMAGE,
			ThunderStep.CHARM_COOLDOWN,
			ThunderStep.CHARM_DISTANCE,
			ThunderStep.CHARM_RADIUS,
			PrismaticShield.CHARM_DURATION,
			PrismaticShield.CHARM_KNOCKBACK,
			PrismaticShield.CHARM_STUN,
			PrismaticShield.CHARM_ABSORPTION,
			PrismaticShield.CHARM_TRIGGER,
			PrismaticShield.CHARM_COOLDOWN,
			FrostNova.CHARM_DAMAGE,
			FrostNova.CHARM_DURATION,
			FrostNova.CHARM_RANGE,
			FrostNova.CHARM_SLOW,
			FrostNova.CHARM_COOLDOWN,
			FrostNova.CHARM_FROZEN,
			MagmaShield.CHARM_DAMAGE,
			MagmaShield.CHARM_DURATION,
			MagmaShield.CHARM_KNOCKBACK,
			MagmaShield.CHARM_RANGE,
			MagmaShield.CHARM_CONE,
			MagmaShield.CHARM_COOLDOWN,
			ArcaneStrike.CHARM_DAMAGE,
			ArcaneStrike.CHARM_RADIUS,
			ArcaneStrike.CHARM_BONUS,
			ArcaneStrike.CHARM_COOLDOWN,
			ElementalArrows.CHARM_DAMAGE,
			ElementalArrows.CHARM_AREA_DAMAGE,
			ElementalArrows.CHARM_DURATION,
			ElementalArrows.CHARM_RANGE,
			ElementalArrows.CHARM_THUNDER_COOLDOWN,
			Spellshock.CHARM_SPEED,
			Spellshock.CHARM_SLOW,
			Spellshock.CHARM_SPELL,
			Spellshock.CHARM_MELEE,
			Spellshock.CHARM_DETONATION_DAMAGE,
			Spellshock.CHARM_DETONATION_RADIUS,
			AstralOmen.CHARM_DAMAGE,
			AstralOmen.CHARM_RANGE,
			AstralOmen.CHARM_MODIFIER,
			AstralOmen.CHARM_STACK,
			CosmicMoonblade.CHARM_CAP,
			CosmicMoonblade.CHARM_DAMAGE,
			CosmicMoonblade.CHARM_RANGE,
			CosmicMoonblade.CHARM_COOLDOWN,
			CosmicMoonblade.CHARM_SPELL_COOLDOWN,
			CosmicMoonblade.CHARM_SLASH,
			SagesInsight.CHARM_SPEED,
			SagesInsight.CHARM_ABILITY,
			SagesInsight.CHARM_DECAY,
			SagesInsight.CHARM_STACKS,
			Blizzard.CHARM_COOLDOWN,
			Blizzard.CHARM_SLOW,
			Blizzard.CHARM_DURATION,
			Blizzard.CHARM_RANGE,
			Blizzard.CHARM_DAMAGE,
			Starfall.CHARM_COOLDOWN,
			Starfall.CHARM_DAMAGE,
			Starfall.CHARM_RANGE,
			Starfall.CHARM_FIRE,
			ElementalSpiritFire.CHARM_COOLDOWN,
			ElementalSpiritFire.CHARM_DAMAGE,
			ElementalSpiritFire.CHARM_SIZE,

			//Cleric
			CelestialBlessing.CHARM_COOLDOWN,
			CelestialBlessing.CHARM_DAMAGE,
			CelestialBlessing.CHARM_DURATION,
			CelestialBlessing.CHARM_SPEED,
			CelestialBlessing.CHARM_RADIUS,
			DivineJustice.CHARM_DAMAGE,
			DivineJustice.CHARM_ALLY,
			DivineJustice.CHARM_SELF,
			HeavenlyBoon.CHARM_CHANCE,
			HeavenlyBoon.CHARM_DURATION,
			HeavenlyBoon.CHARM_RADIUS,
			SacredProvisions.CHARM_CHANCE,
			SacredProvisions.CHARM_RANGE,
			CleansingRain.CHARM_COOLDOWN,
			CleansingRain.CHARM_RANGE,
			CleansingRain.CHARM_REDUCTION,
			CleansingRain.CHARM_DURATION,
			HandOfLight.CHARM_COOLDOWN,
			HandOfLight.CHARM_DAMAGE,
			HandOfLight.CHARM_HEALING,
			HandOfLight.CHARM_RANGE,
			SanctifiedArmor.CHARM_DAMAGE,
			SanctifiedArmor.CHARM_DURATION,
			SanctifiedArmor.CHARM_SLOW,
			HolyJavelin.CHARM_COOLDOWN,
			HolyJavelin.CHARM_DAMAGE,
			HolyJavelin.CHARM_RANGE,
			ChoirBells.CHARM_COOLDOWN,
			ChoirBells.CHARM_RANGE,
			ChoirBells.CHARM_SLOW,
			ChoirBells.CHARM_DAMAGE,
			ChoirBells.CHARM_VULN,
			ChoirBells.CHARM_WEAKEN,
			ChoirBells.CHARM_SLOW,
			LuminousInfusion.CHARM_COOLDOWN,
			LuminousInfusion.CHARM_RADIUS,
			LuminousInfusion.CHARM_DAMAGE,
			EnchantedPrayer.CHARM_COOLDOWN,
			EnchantedPrayer.CHARM_DAMAGE,
			EnchantedPrayer.CHARM_RANGE,
			EnchantedPrayer.CHARM_EFFECT_RANGE,
			EnchantedPrayer.CHARM_HEAL,
			ThuribleProcession.CHARM_COOLDOWN,
			ThuribleProcession.CHARM_EFFECT_DURATION,
			ThuribleProcession.CHARM_DAMAGE,
			ThuribleProcession.CHARM_SPEED,
			ThuribleProcession.CHARM_ATTACK,
			ThuribleProcession.CHARM_HEAL,
			HallowedBeam.CHARM_CHARGE,
			HallowedBeam.CHARM_COOLDOWN,
			HallowedBeam.CHARM_DAMAGE,
			HallowedBeam.CHARM_DISTANCE,
			HallowedBeam.CHARM_STUN,
			HallowedBeam.CHARM_HEAL,
			Rejuvenation.CHARM_THRESHOLD,

			//Rogue
			AdvancingShadows.CHARM_COOLDOWN,
			AdvancingShadows.CHARM_DAMAGE,
			AdvancingShadows.CHARM_KNOCKBACK,
			AdvancingShadows.CHARM_RANGE,
			ByMyBlade.CHARM_COOLDOWN,
			ByMyBlade.CHARM_DAMAGE,
			ByMyBlade.CHARM_HASTE_DURATION,
			ByMyBlade.CHARM_HASTE_AMPLIFIER,
			DaggerThrow.CHARM_COOLDOWN,
			DaggerThrow.CHARM_DAMAGE,
			DaggerThrow.CHARM_RANGE,
			DaggerThrow.CHARM_VULN,
			DaggerThrow.CHARM_DAGGERS,
			EscapeDeath.CHARM_ABSORPTION,
			EscapeDeath.CHARM_COOLDOWN,
			EscapeDeath.CHARM_SPEED,
			EscapeDeath.CHARM_JUMP,
			EscapeDeath.CHARM_STUN_DURATION,
			Skirmisher.CHARM_DAMAGE,
			Skirmisher.CHARM_RADIUS,
			Smokescreen.CHARM_COOLDOWN,
			Smokescreen.CHARM_RANGE,
			Smokescreen.CHARM_WEAKEN,
			Smokescreen.CHARM_SLOW,
			ViciousCombos.CHARM_CDR,
			BladeDance.CHARM_COOLDOWN,
			BladeDance.CHARM_DAMAGE,
			BladeDance.CHARM_RADIUS,
			BladeDance.CHARM_RESIST,
			BladeDance.CHARM_ROOT,
			DeadlyRonde.CHARM_DAMAGE,
			DeadlyRonde.CHARM_RADIUS,
			DeadlyRonde.CHARM_ANGLE,
			DeadlyRonde.CHARM_KNOCKBACK,
			DeadlyRonde.CHARM_STACKS,
			WindWalk.CHARM_CHARGE,
			WindWalk.CHARM_COOLDOWN,
			WindWalk.CHARM_COOLDOWN_REDUCTION,
			BodkinBlitz.CHARM_CHARGE,
			BodkinBlitz.CHARM_COOLDOWN,
			BodkinBlitz.CHARM_DAMAGE,
			BodkinBlitz.CHARM_DISTANCE,
			BodkinBlitz.CHARM_STEALTH,
			CloakAndDagger.CHARM_DAMAGE,
			CloakAndDagger.CHARM_STACKS,
			CloakAndDagger.CHARM_STEALTH,
			CoupDeGrace.CHARM_NORMAL,
			CoupDeGrace.CHARM_THRESHOLD,
			CoupDeGrace.CHARM_ELITE,

			//Warrior
			BruteForce.CHARM_DAMAGE,
			BruteForce.CHARM_RADIUS,
			BruteForce.CHARM_KNOCKBACK,
			BruteForce.CHARM_WAVES,
			BruteForce.CHARM_WAVE_DAMAGE_RATIO,
			CounterStrike.CHARM_DAMAGE,
			CounterStrike.CHARM_RADIUS,
			CounterStrike.CHARM_DURATION,
			CounterStrike.CHARM_STACKS,
			CounterStrike.CHARM_DAMAGE_REDUCTION,
			DefensiveLine.CHARM_COOLDOWN,
			DefensiveLine.CHARM_DURATION,
			DefensiveLine.CHARM_KNOCKBACK,
			DefensiveLine.CHARM_REDUCTION,
			DefensiveLine.CHARM_RANGE,
			DefensiveLine.CHARM_NEGATIONS,
			Frenzy.CHARM_ATTACK_SPEED,
			Frenzy.CHARM_SPEED,
			Frenzy.CHARM_DURATION,
			Frenzy.CHARM_BONUS_DAMAGE,
			Riposte.CHARM_COOLDOWN,
			Riposte.CHARM_KNOCKBACK,
			Riposte.CHARM_BONUS_DAMAGE,
			Riposte.CHARM_DAMAGE_DURATION,
			Riposte.CHARM_STUN_DURATION,
			Riposte.CHARM_ROOT_DURATION,
			Riposte.CHARM_RADIUS,
			Riposte.CHARM_DAMAGE,
			ShieldBash.CHARM_DAMAGE,
			ShieldBash.CHARM_COOLDOWN,
			ShieldBash.CHARM_RANGE,
			ShieldBash.CHARM_RADIUS,
			ShieldBash.CHARM_DURATION,
			Toughness.CHARM_HEALTH,
			Toughness.CHARM_REDUCTION,
			Toughness.CHARM_HEALING,
			WeaponMastery.CHARM_REDUCTION,
			WeaponMastery.CHARM_ATTACK_SPEED,
			WeaponMastery.CHARM_WEAKEN,
			WeaponMastery.CHARM_DURATION,
			GloriousBattle.CHARM_DAMAGE,
			GloriousBattle.CHARM_CHARGES,
			GloriousBattle.CHARM_VELOCITY,
			GloriousBattle.CHARM_RADIUS,
			GloriousBattle.CHARM_BLEED_AMPLIFIER,
			GloriousBattle.CHARM_BLEED_DURATION,
			GloriousBattle.CHARM_KNOCKBACK,
			GloriousBattle.CHARM_DAMAGE_MODIFIER,
			MeteorSlam.CHARM_COOLDOWN,
			MeteorSlam.CHARM_DAMAGE,
			MeteorSlam.CHARM_RADIUS,
			MeteorSlam.CHARM_JUMP_BOOST,
			MeteorSlam.CHARM_DURATION,
			MeteorSlam.CHARM_THRESHOLD,
			Rampage.CHARM_STACKS,
			Rampage.CHARM_DAMAGE,
			Rampage.CHARM_RADIUS,
			Rampage.CHARM_HEALING,
			Rampage.CHARM_THRESHOLD,
			Rampage.CHARM_REDUCTION_PER_STACK,
			Bodyguard.CHARM_COOLDOWN,
			Bodyguard.CHARM_RADIUS,
			Bodyguard.CHARM_RANGE,
			Bodyguard.CHARM_ABSORPTION,
			Bodyguard.CHARM_ABSORPTION_DURATION,
			Bodyguard.CHARM_STUN_DURATION,
			Bodyguard.CHARM_KNOCKBACK,
			Challenge.CHARM_COOLDOWN,
			Challenge.CHARM_DAMAGE,
			Challenge.CHARM_ABSORPTION_PER,
			Challenge.CHARM_ABSORPTION_MAX,
			Challenge.CHARM_DURATION,
			Challenge.CHARM_RANGE,
			ShieldWall.CHARM_DAMAGE,
			ShieldWall.CHARM_COOLDOWN,
			ShieldWall.CHARM_DURATION,
			ShieldWall.CHARM_ANGLE,
			ShieldWall.CHARM_KNOCKBACK,

			//Alchemist
			AlchemistPotions.CHARM_DAMAGE,
			AlchemistPotions.CHARM_RADIUS,
			AlchemistPotions.CHARM_CHARGES,
			AlchemicalArtillery.CHARM_MULTIPLIER,
			AlchemicalArtillery.CHARM_EXPLOSION_MULTIPLIER,
			AlchemicalArtillery.CHARM_RADIUS,
			AlchemicalArtillery.CHARM_KNOCKBACK,
			AlchemicalArtillery.CHARM_DELAY,
			Bezoar.CHARM_RADIUS,
			Bezoar.CHARM_REQUIREMENT,
			Bezoar.CHARM_DAMAGE,
			Bezoar.CHARM_DAMAGE_DURATION,
			Bezoar.CHARM_HEALING,
			Bezoar.CHARM_HEAL_DURATION,
			Bezoar.CHARM_LINGER_TIME,
			Bezoar.CHARM_DEBUFF_REDUCTION,
			Bezoar.CHARM_POTIONS,
			Bezoar.CHARM_PHILOSOPHER_STONE_RATE,
			Bezoar.CHARM_PHILOSOPHER_STONE_POTIONS,
			Bezoar.CHARM_ABSORPTION,
			Bezoar.CHARM_ABSORPTION_DURATION,
			BrutalAlchemy.CHARM_POTION_DAMAGE,
			BrutalAlchemy.CHARM_DOT_DAMAGE,
			BrutalAlchemy.CHARM_DURATION,
			BrutalAlchemy.CHARM_MULTIPLIER,
			BrutalAlchemy.CHARM_RADIUS,
			EmpoweringOdor.CHARM_SPEED,
			EmpoweringOdor.CHARM_DAMAGE,
			EmpoweringOdor.CHARM_SINGLE_HIT_DAMAGE,
			EmpoweringOdor.CHARM_DURATION,
			EnergizingElixir.CHARM_SPEED,
			EnergizingElixir.CHARM_JUMP_BOOST,
			EnergizingElixir.CHARM_DAMAGE,
			EnergizingElixir.CHARM_DURATION,
			EnergizingElixir.CHARM_STACKS,
			EnergizingElixir.CHARM_BONUS,
			EnergizingElixir.CHARM_PRICE,
			GruesomeAlchemy.CHARM_DAMAGE,
			GruesomeAlchemy.CHARM_DURATION,
			GruesomeAlchemy.CHARM_SLOWNESS,
			GruesomeAlchemy.CHARM_VULNERABILITY,
			GruesomeAlchemy.CHARM_WEAKEN,
			IronTincture.CHARM_COOLDOWN,
			IronTincture.CHARM_ABSORPTION,
			IronTincture.CHARM_DURATION,
			IronTincture.CHARM_RESISTANCE,
			IronTincture.CHARM_VELOCITY,
			UnstableAmalgam.CHARM_COOLDOWN,
			UnstableAmalgam.CHARM_DAMAGE,
			UnstableAmalgam.CHARM_DURATION,
			UnstableAmalgam.CHARM_KNOCKBACK,
			UnstableAmalgam.CHARM_RADIUS,
			UnstableAmalgam.CHARM_RANGE,
			UnstableAmalgam.CHARM_INSTABILITY_DURATION,
			UnstableAmalgam.CHARM_POTION_DAMAGE,
			Panacea.CHARM_DAMAGE,
			Panacea.CHARM_COOLDOWN,
			Panacea.CHARM_RADIUS,
			Panacea.CHARM_ABSORPTION,
			Panacea.CHARM_ABSORPTION_DURATION,
			Panacea.CHARM_ABSORPTION_MAX,
			Panacea.CHARM_SLOW_DURATION,
			Panacea.CHARM_MOVEMENT_DURATION,
			Panacea.CHARM_MOVEMENT_SPEED,
			TransmutationRing.CHARM_COOLDOWN,
			TransmutationRing.CHARM_DURATION,
			TransmutationRing.CHARM_RADIUS,
			TransmutationRing.CHARM_DAMAGE_AMPLIFIER,
			TransmutationRing.CHARM_PER_KILL_AMPLIFIER,
			TransmutationRing.CHARM_MAX_KILLS,
			WardingRemedy.CHARM_COOLDOWN,
			WardingRemedy.CHARM_PULSES,
			WardingRemedy.CHARM_FREQUENCY,
			WardingRemedy.CHARM_RADIUS,
			WardingRemedy.CHARM_ABSORPTION,
			WardingRemedy.CHARM_MAX_ABSORPTION,
			WardingRemedy.CHARM_ABSORPTION_DURATION,
			WardingRemedy.CHARM_HEALING,
			EsotericEnhancements.CHARM_COOLDOWN,
			EsotericEnhancements.CHARM_DAMAGE,
			EsotericEnhancements.CHARM_BLEED,
			EsotericEnhancements.CHARM_DURATION,
			EsotericEnhancements.CHARM_RADIUS,
			EsotericEnhancements.CHARM_REACTION_TIME,
			EsotericEnhancements.CHARM_FUSE,
			EsotericEnhancements.CHARM_SPEED,
			EsotericEnhancements.CHARM_CREEPER,
			ScorchedEarth.CHARM_DAMAGE,
			ScorchedEarth.CHARM_COOLDOWN,
			ScorchedEarth.CHARM_CHARGES,
			ScorchedEarth.CHARM_RADIUS,
			ScorchedEarth.CHARM_DURATION,
			Taboo.CHARM_COOLDOWN,
			Taboo.CHARM_DAMAGE,
			Taboo.CHARM_HEALING,
			Taboo.CHARM_KNOCKBACK_RESISTANCE,
			Taboo.CHARM_SELF_DAMAGE,

			//Warlock
			AmplifyingHex.CHARM_CONE,
			AmplifyingHex.CHARM_COOLDOWN,
			AmplifyingHex.CHARM_DAMAGE,
			AmplifyingHex.CHARM_RANGE,
			AmplifyingHex.CHARM_EFFECT,
			CholericFlames.CHARM_COOLDOWN,
			CholericFlames.CHARM_DAMAGE,
			CholericFlames.CHARM_FIRE,
			CholericFlames.CHARM_HUNGER,
			CholericFlames.CHARM_KNOCKBACK,
			CholericFlames.CHARM_RANGE,
			CholericFlames.CHARM_INFERNO_CAP,
			CholericFlames.CHARM_ENHANCEMENT_RADIUS,
			CursedWound.CHARM_DAMAGE,
			CursedWound.CHARM_DOT,
			CursedWound.CHARM_CAP,
			CursedWound.CHARM_RADIUS,
			GraspingClaws.CHARM_DAMAGE,
			GraspingClaws.CHARM_COOLDOWN,
			GraspingClaws.CHARM_SLOW,
			GraspingClaws.CHARM_DURATION,
			GraspingClaws.CHARM_RADIUS,
			GraspingClaws.CHARM_PROJ_SPEED,
			GraspingClaws.CHARM_PULL,
			GraspingClaws.CHARM_CAGE_RADIUS,
			MelancholicLament.CHARM_COOLDOWN,
			MelancholicLament.CHARM_RADIUS,
			MelancholicLament.CHARM_RECOVERY,
			MelancholicLament.CHARM_WEAKNESS,
			PhlegmaticResolve.CHARM_ALLY,
			PhlegmaticResolve.CHARM_RANGE,
			PhlegmaticResolve.CHARM_RESIST,
			PhlegmaticResolve.CHARM_KBR,
			SanguineHarvest.CHARM_COOLDOWN,
			SanguineHarvest.CHARM_HEAL,
			SanguineHarvest.CHARM_KNOCKBACK,
			SanguineHarvest.CHARM_RADIUS,
			SanguineHarvest.CHARM_BLEED,
			SoulRend.CHARM_HEAL,
			SoulRend.CHARM_ALLY,
			SoulRend.CHARM_CAP,
			SoulRend.CHARM_COOLDOWN,
			SoulRend.CHARM_RADIUS,
			HauntingShades.CHARM_COOLDOWN,
			HauntingShades.CHARM_DURATION,
			HauntingShades.CHARM_HEALING,
			HauntingShades.CHARM_RADIUS,
			RestlessSouls.CHARM_CAP,
			RestlessSouls.CHARM_DAMAGE,
			RestlessSouls.CHARM_DURATION,
			RestlessSouls.CHARM_RADIUS,
			WitheringGaze.CHARM_COOLDOWN,
			WitheringGaze.CHARM_DAMAGE,
			WitheringGaze.CHARM_STUN,
			WitheringGaze.CHARM_DOT,
			WitheringGaze.CHARM_RANGE,
			DarkPact.CHARM_ABSORPTION,
			DarkPact.CHARM_ATTACK_SPEED,
			DarkPact.CHARM_CAP,
			DarkPact.CHARM_DAMAGE,
			DarkPact.CHARM_DURATION,
			DarkPact.CHARM_REFRESH,
			DarkPact.CHARM_COOLDOWN,
			JudgementChain.CHARM_COOLDOWN,
			JudgementChain.CHARM_DAMAGE,
			JudgementChain.CHARM_RANGE,
			JudgementChain.CHARM_DURATION,
			VoodooBonds.CHARM_COOLDOWN,
			VoodooBonds.CHARM_DAMAGE,
			VoodooBonds.CHARM_RADIUS,
			VoodooBonds.CHARM_TRANSFER_DAMAGE,
			VoodooBonds.CHARM_TRANSFER_TIME,

			//Scout
			EagleEye.CHARM_COOLDOWN,
			EagleEye.CHARM_DURATION,
			EagleEye.CHARM_RADIUS,
			EagleEye.CHARM_REFRESH,
			EagleEye.CHARM_VULN,
			HuntingCompanion.CHARM_DAMAGE,
			HuntingCompanion.CHARM_DURATION,
			HuntingCompanion.CHARM_COOLDOWN,
			HuntingCompanion.CHARM_STUN_DURATION,
			HuntingCompanion.CHARM_BLEED_DURATION,
			HuntingCompanion.CHARM_BLEED_AMPLIFIER,
			HuntingCompanion.CHARM_FOXES,
			HuntingCompanion.CHARM_EAGLES,
			HuntingCompanion.CHARM_SPEED,
			HuntingCompanion.CHARM_HEALING,
			Sharpshooter.CHARM_STACK_DAMAGE,
			Sharpshooter.CHARM_STACKS,
			Sharpshooter.CHARM_RETRIEVAL,
			Sharpshooter.CHARM_DECAY,
			SwiftCuts.CHARM_DAMAGE,
			SwiftCuts.CHARM_SWEEP_DAMAGE,
			SwiftCuts.CHARM_RADIUS,
			Swiftness.CHARM_SPEED,
			Swiftness.CHARM_JUMP_BOOST,
			Swiftness.CHARM_DODGE,
			Volley.CHARM_COOLDOWN,
			Volley.CHARM_ARROWS,
			Volley.CHARM_DAMAGE,
			Volley.CHARM_PIERCING,
			Volley.CHARM_BLEED_AMPLIFIER,
			Volley.CHARM_BLEED_DURATION,
			WindBomb.CHARM_COOLDOWN,
			WindBomb.CHARM_DAMAGE,
			WindBomb.CHARM_DAMAGE_MODIFIER,
			WindBomb.CHARM_RADIUS,
			WindBomb.CHARM_DURATION,
			WindBomb.CHARM_WEAKNESS,
			WindBomb.CHARM_HEIGHT,
			WindBomb.CHARM_PULL,
			WindBomb.CHARM_VORTEX_DURATION,
			WindBomb.CHARM_VORTEX_RADIUS,
			PinningShot.CHARM_DAMAGE,
			PinningShot.CHARM_WEAKEN,
			PredatorStrike.CHARM_DAMAGE,
			PredatorStrike.CHARM_RADIUS,
			PredatorStrike.CHARM_COOLDOWN,
			SplitArrow.CHARM_DAMAGE,
			SplitArrow.CHARM_RANGE,
			SplitArrow.CHARM_BOUNCES,
			Quickdraw.CHARM_COOLDOWN,
			Quickdraw.CHARM_PIERCING,
			Quickdraw.CHARM_DAMAGE,
			TacticalManeuver.CHARM_CHARGES,
			TacticalManeuver.CHARM_COOLDOWN,
			TacticalManeuver.CHARM_DAMAGE,
			TacticalManeuver.CHARM_VELOCITY,
			TacticalManeuver.CHARM_RADIUS,
			TacticalManeuver.CHARM_DURATION,
			WhirlingBlade.CHARM_CHARGES,
			WhirlingBlade.CHARM_COOLDOWN,
			WhirlingBlade.CHARM_DAMAGE,
			WhirlingBlade.CHARM_RADIUS,
			WhirlingBlade.CHARM_KNOCKBACK
		);
	}

	public void loadFlippedColorEffects() {
		mFlippedColorEffectSubstrings = Arrays.asList(
			JunglesNourishment.CHARM_COOLDOWN,
			RageOfTheKeter.CHARM_COOLDOWN,
			IntoxicatingWarmth.CHARM_COOLDOWN,
			TemporalBender.CHARM_COOLDOWN,
			LiquidCourage.CHARM_COOLDOWN,
			ElementalArrows.CHARM_THUNDER_COOLDOWN,
			ManaLance.CHARM_COOLDOWN,
			ThunderStep.CHARM_COOLDOWN,
			PrismaticShield.CHARM_COOLDOWN,
			FrostNova.CHARM_COOLDOWN,
			MagmaShield.CHARM_COOLDOWN,
			ArcaneStrike.CHARM_COOLDOWN,
			CosmicMoonblade.CHARM_COOLDOWN,
			CosmicMoonblade.CHARM_SPELL_COOLDOWN,
			Blizzard.CHARM_COOLDOWN,
			Starfall.CHARM_COOLDOWN,
			ElementalSpiritFire.CHARM_COOLDOWN,
			CelestialBlessing.CHARM_COOLDOWN,
			CleansingRain.CHARM_COOLDOWN,
			HandOfLight.CHARM_COOLDOWN,
			HolyJavelin.CHARM_COOLDOWN,
			ChoirBells.CHARM_COOLDOWN,
			LuminousInfusion.CHARM_COOLDOWN,
			EnchantedPrayer.CHARM_COOLDOWN,
			ThuribleProcession.CHARM_COOLDOWN,
			HallowedBeam.CHARM_COOLDOWN,
			AdvancingShadows.CHARM_COOLDOWN,
			ByMyBlade.CHARM_COOLDOWN,
			DaggerThrow.CHARM_COOLDOWN,
			EscapeDeath.CHARM_COOLDOWN,
			Smokescreen.CHARM_COOLDOWN,
			BladeDance.CHARM_COOLDOWN,
			WindWalk.CHARM_COOLDOWN,
			BodkinBlitz.CHARM_COOLDOWN,
			DefensiveLine.CHARM_COOLDOWN,
			Riposte.CHARM_COOLDOWN,
			ShieldBash.CHARM_COOLDOWN,
			MeteorSlam.CHARM_COOLDOWN,
			Bodyguard.CHARM_COOLDOWN,
			Challenge.CHARM_COOLDOWN,
			ShieldWall.CHARM_COOLDOWN,
			IronTincture.CHARM_COOLDOWN,
			EnergizingElixir.CHARM_PRICE,
			UnstableAmalgam.CHARM_COOLDOWN,
			Panacea.CHARM_COOLDOWN,
			TransmutationRing.CHARM_COOLDOWN,
			WardingRemedy.CHARM_COOLDOWN,
			EsotericEnhancements.CHARM_COOLDOWN,
			EsotericEnhancements.CHARM_FUSE,
			ScorchedEarth.CHARM_COOLDOWN,
			Taboo.CHARM_COOLDOWN,
			Taboo.CHARM_SELF_DAMAGE,
			AmplifyingHex.CHARM_COOLDOWN,
			CholericFlames.CHARM_COOLDOWN,
			GraspingClaws.CHARM_COOLDOWN,
			MelancholicLament.CHARM_COOLDOWN,
			SanguineHarvest.CHARM_COOLDOWN,
			SoulRend.CHARM_COOLDOWN,
			HauntingShades.CHARM_COOLDOWN,
			WitheringGaze.CHARM_COOLDOWN,
			DarkPact.CHARM_COOLDOWN,
			JudgementChain.CHARM_COOLDOWN,
			VoodooBonds.CHARM_COOLDOWN,
			EagleEye.CHARM_COOLDOWN,
			HuntingCompanion.CHARM_COOLDOWN,
			Volley.CHARM_COOLDOWN,
			WindBomb.CHARM_COOLDOWN,
			PredatorStrike.CHARM_COOLDOWN,
			Quickdraw.CHARM_COOLDOWN,
			TacticalManeuver.CHARM_COOLDOWN,
			WhirlingBlade.CHARM_COOLDOWN
		);
	}

	public boolean addCharm(Player p, ItemStack charm) {

		if (p != null && mPlayerCharms.get(p.getUniqueId()) != null && validateCharm(p, charm)) {
			ItemStack charmCopy = new ItemStack(charm);
			mPlayerCharms.get(p.getUniqueId()).add(charmCopy);

			updateCharms(p, mPlayerCharms.get(p.getUniqueId()));

			return true;
		} else if (p != null && validateCharm(p, charm)) {
			List<ItemStack> playerCharms = new ArrayList<>();
			ItemStack charmCopy = new ItemStack(charm);
			playerCharms.add(charmCopy);
			mPlayerCharms.put(p.getUniqueId(), playerCharms);

			updateCharms(p, mPlayerCharms.get(p.getUniqueId()));

			return true;
		}

		return false;
	}

	// This method validates the charm against the player's current charm list before actually adding it
	public boolean validateCharm(Player p, ItemStack charm) {
		//Check to make sure the added charm is a valid charm (check lore) and not a duplicate of one they already have!
		//Also make sure the charm list exists if we're checking against it
		//Also make sure the charm list has space for the new charm if it exists
		//Also make sure it's not a stack (only one item)
		//Also parse the charm's power budget and make sure adding it would not overflow

		//Check item stack for valid name and amount
		if (charm == null || charm.getAmount() != 1 || !charm.hasItemMeta() || !charm.getItemMeta().hasDisplayName()) {
			return false;
		}
		// Make sure item has Charm Tier
		if (!ItemStatUtils.getTier(charm).equals(ItemStatUtils.Tier.CHARM) && !ItemStatUtils.getTier(charm).equals(ItemStatUtils.Tier.RARE_CHARM) && !ItemStatUtils.getTier(charm).equals(ItemStatUtils.Tier.EPIC_CHARM)) {
			return false;
		}
		// Charm Power Handling
		int charmPower = ItemStatUtils.getCharmPower(charm);
		if (charmPower > 0) {
			//Now check the player charms to make sure it is different from existing charms
			if (p != null && mPlayerCharms.get(p.getUniqueId()) != null) {
				List<ItemStack> charms = mPlayerCharms.get(p.getUniqueId());
				//Check max charm count in list
				if (charms.size() >= MAX_CHARM_COUNT) {
					return false;
				}
				int powerBudget = 0;
				//Check naming of each charm
				for (ItemStack c : charms) {
					if (c.getItemMeta().displayName().equals(charm.getItemMeta().displayName())) {
						return false;
					}
					powerBudget += ItemStatUtils.getCharmPower(c);
				}
				//Check to see if adding the extra charm would exceed budget
				Optional<Integer> optionalBudget = ScoreboardUtils.getScoreboardValue(p, "CharmPower");
				int totalBudget = 0;
				if (optionalBudget.isPresent()) {
					totalBudget = optionalBudget.get();
				}
				if (totalBudget == 0) {
					//Default case for testing- later will be set by mechs for all r3 players
					totalBudget = 15;
				}
				if (powerBudget + charmPower > totalBudget) {
					return false;
				}
				return true;
			} else if (p != null) {
				return true;
			}
		}
		return false;
	}

	public boolean removeCharm(Player p, ItemStack charm) {
		if (p != null && mPlayerCharms.get(p.getUniqueId()) != null) {
			//TODO make sure this actually removes the right charm (due to how the itemstack equals method works)

			if (mPlayerCharms.get(p.getUniqueId()).remove(charm)) {
				updateCharms(p, mPlayerCharms.get(p.getUniqueId()));
				return true;
			}
		}
		return false;
	}

	public boolean removeCharmBySlot(Player p, int slot) {
		if (p == null) {
			return false;
		}
		List<ItemStack> charms = mPlayerCharms.get(p.getUniqueId());
		if (charms != null && slot < charms.size()) {
			charms.remove(slot);
			return true;
		}
		return false;
	}


	public boolean clearCharms(Player p) {
		mPlayerCharms.get(p.getUniqueId()).clear();
		updateCharms(p, mPlayerCharms.get(p.getUniqueId()));
		return true;
	}

	public void updateCharms(Player p, List<ItemStack> equippedCharms) {
		//Calculate the map of effects to values
		Map<String, Double> allEffects = new HashMap<>();
		//Loop through each effect
		for (String charmEffect : mCharmEffectList) {
			double finalValue = 0;
			double finalValuePct = 0;
			//Loop through each charm the player has equipped
			for (ItemStack item : equippedCharms) {
				if (item == null || item.getType() == Material.AIR) {
					continue;
				}
				CharmParsedInfo parsedInfo = getPlayerItemLevel(item, charmEffect);
				if (parsedInfo.mIsPercent) {
					finalValuePct += parsedInfo.mValue;
				} else {
					finalValue += parsedInfo.mValue;
				}
			}
			//Separate out flat and percent values so charm effects can use both
			allEffects.put(charmEffect, finalValue);
			allEffects.put(charmEffect + "%", finalValuePct);

		}

		//Store to local map
		mPlayerCharmEffectMap.put(p.getUniqueId(), allEffects);

		//Loop through charms for onhit effects
		Multimap<ClassAbility, Effect> abilityEffects = ArrayListMultimap.create();
		for (ItemStack item : equippedCharms) {
			List<String> plainLoreLines = ItemStatUtils.getPlainCharmLore(new NBTItem(item));
			for (String plainLore : plainLoreLines) {
				if (plainLore.contains("Hit :")) {
					ClassAbility classAbility = null;
					//Step one- get the class ability
					String abilityName = plainLore.split("Hit :")[0]; //Trim back half of line with effect off
					abilityName = abilityName.split("n", 2)[1]; //Get rid of the initial "On"
					abilityName = abilityName.trim();
					for (ClassAbility ca : ClassAbility.values()) {
						if (ca.getName().toLowerCase().equals(abilityName.toLowerCase())) {
							classAbility = ca;
						}
					}
					if (classAbility == null) {
						continue;
					}
					//Step two- get the class effect
					String effectName = plainLore.split(":")[1]; //Trim front half of line with ability off
					Scanner s = new Scanner(effectName);
					//First word of the string will be the effect
					effectName = s.next().toLowerCase();
					double amplifier = -1;
					int duration = -1;
					Scanner s2 = new Scanner(plainLore.split(":")[1]).useDelimiter("\\D+");
					if (plainLore.contains("%")) {
						//Parse the amplifier first, if it exists (indicated by %)
						if (s2.hasNextInt()) {
							amplifier = s2.nextInt() / 100.0;
						}
					}
					if (s2.hasNextInt()) {
						//Parse the final duration now
						duration = s2.nextInt() * 20;
					}
					//Now, create the effect by name and add it
					Effect parsedEffect = null;
					//TODO add more effects to this
					if (effectName.equals("slowness")) {
						parsedEffect = new PercentSpeed(duration, amplifier * -1, CHARM_EFFECT_NAME);
					}
					if (parsedEffect != null) {
						//Add to the player's effect list
						abilityEffects.put(classAbility, parsedEffect);
					}
				}
			}
		}
		//Store to local map
		mPlayerAbilityEffectMap.put(p.getUniqueId(), abilityEffects);
		//Refresh class of player
		AbilityManager.getManager().updatePlayerAbilities(p, false);
	}

	//Helper method to parse item for charm effects
	private CharmParsedInfo getPlayerItemLevel(ItemStack itemStack, String effect) {
		List<String> plainLoreLines = ItemStatUtils.getPlainCharmLore(new NBTItem(itemStack));
		for (String plainLore : plainLoreLines) {
			if (plainLore.contains(effect)) {
				double value = parseValue(plainLore);
				if (plainLore.contains("%")) {
					return new CharmParsedInfo(value, true);
				} else {
					return new CharmParsedInfo(value, false);
				}
			}
		}
		return new CharmParsedInfo(0, false);
	}

	//Helper method to parse lore line for charm effects
	private double parseValue(String loreLine) {
		//Whether effect is being added to or subtracted
		boolean add = loreLine.contains("+");
		//Parse the value from the line

		loreLine = loreLine.split("\\+|-")[1];

		//First check for a double value in the lore line
		String stippedLoreLine = loreLine.replace('%', ' ');
		@SuppressWarnings("resource")
		Scanner s = new Scanner(stippedLoreLine);
		if (s.hasNextDouble()) {
			double sint = s.nextDouble();
			//If it's a negative effect
			if (!add) {
				sint = sint * -1.0;
			}
			s.close();
			return sint;
		}
		s.close();
		// If no double was found, check for just plain int
		s = new Scanner(loreLine).useDelimiter("\\D+");
		if (s.hasNextInt()) {
			int sint = s.nextInt();
			//If it's a negative effect
			if (!add) {
				sint = sint * -1;
			}
			s.close();
			return sint;
		}
		s.close();
		return 0;
	}

	/**
	 * This method will be called by abilities & enchantments to get the modifier value of all
	 * charms the player currently has equipped for a particular attribute.
	 *
	 * @param p              player to get
	 * @param charmAttribute string property of the charm to parse
	 * @return total value over all charms the player has equipped
	 */
	public double getValueOfAttribute(Player p, String charmAttribute) {
		//Check if charms are enabled (r3 shard), if not, return zero as the net effect
		if (!ServerProperties.getCharmsEnabled() || p == null) {
			return 0;
		}

		Map<String, Double> allEffects = mPlayerCharmEffectMap.get(p.getUniqueId());
		if (allEffects != null && allEffects.get(charmAttribute) != null) {
			return allEffects.get(charmAttribute).doubleValue();
		}
		return 0;
	}

	public String getSummaryOfAllAttributes(Player p) {
		Map<String, Double> allEffects = mPlayerCharmEffectMap.get(p.getUniqueId());
		if (allEffects != null) {
			String summary = "";
			for (String s : allEffects.keySet()) {
				if (allEffects.get(s) != 0) {
					if (s.contains("%")) {
						summary += s + " : " + (allEffects.get(s).toString() + 100) + "%" + "\n";
					} else {
						summary += s + " : " + allEffects.get(s).toString() + "\n";
					}
				}
			}
			//Finally, add the custom charm effects
			List<ItemStack> playerCharms = CharmManager.getInstance().mPlayerCharms.get(p.getUniqueId());
			List<String> effectsAlreadyAdded = new ArrayList<>();
			if (playerCharms != null) {
				for (ItemStack charm : playerCharms) {
					List<String> plainLoreLines = ItemStatUtils.getPlainCharmLore(new NBTItem(charm));
					for (String plainLore : plainLoreLines) {
						if (plainLore.contains("Hit :") && !effectsAlreadyAdded.contains(plainLore)) {
							summary += plainLore + "\n";
							effectsAlreadyAdded.add(plainLore);
						}
					}
				}
			}
			return summary;
		}
		return null;
	}

	public List<Component> getSummaryOfAllAttributesAsComponents(Player p) {
		Map<String, Double> allEffects = mPlayerCharmEffectMap.get(p.getUniqueId());
		if (allEffects == null) {
			return new ArrayList<>();
		}

		//Sort the effects in the order in the manager list
		List<String> orderedEffects = new ArrayList<>();
		for (String effect : mCharmEffectList) {
			if (allEffects.containsKey(effect)) {
				orderedEffects.add(effect);
			}

			String percentEffect = effect + "%";
			if (allEffects.containsKey(percentEffect)) {
				orderedEffects.add(percentEffect);
			}
		}

		List<Component> components = new ArrayList<>();
		for (String s : orderedEffects) {
			if (allEffects.get(s) != 0) {

				// Strip ugly .0s
				String desc = s + " : " + allEffects.get(s).toString();
				if (desc.endsWith(".0")) {
					desc = desc.substring(0, desc.length() - 2);
				}

				String charmColor = getCharmEffectColor(allEffects.get(s) > 0, s);

				if (s.contains("%")) {
					desc += "%";
				}

				components.add(Component.text(desc, TextColor.fromHexString(charmColor)).decoration(TextDecoration.ITALIC, false));
			}
		}
		//Now add the custom charm effects
		List<ItemStack> playerCharms = CharmManager.getInstance().mPlayerCharms.get(p.getUniqueId());
		List<String> effectsAlreadyAdded = new ArrayList<>();

		if (playerCharms != null) {
			for (ItemStack charm : playerCharms) {
				List<String> plainLoreLines = ItemStatUtils.getPlainCharmLore(new NBTItem(charm));
				for (String plainLore : plainLoreLines) {
					if (plainLore.contains("Hit :") && !effectsAlreadyAdded.contains(plainLore)) {
						components.add(Component.text(plainLore, TextColor.fromHexString("#C8A2C8")).decoration(TextDecoration.ITALIC, false));
						effectsAlreadyAdded.add(plainLore);
					}
				}
			}
		}
		return components;
	}

	public String getSummaryOfCharmNames(Player p) {
		List<ItemStack> charms = mPlayerCharms.get(p.getUniqueId());
		if (charms != null) {
			String summary = "";
			int powerBudget = 0;
			for (ItemStack item : charms) {
				if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
					summary += item.getItemMeta().getDisplayName() + "\n";
					powerBudget += ItemStatUtils.getCharmPower(item);
				}
			}
			summary += ChatColor.YELLOW + "Charm Power: " + powerBudget;
			return summary;
		}
		return null;
	}

	public int getCharmPower(Player p) {
		List<ItemStack> charms = mPlayerCharms.get(p.getUniqueId());
		int powerBudget = 0;
		if (charms != null) {
			for (ItemStack item : charms) {
				if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
					powerBudget += ItemStatUtils.getCharmPower(item);
				}
			}
		}
		return powerBudget;
	}

	//Handlers for player lifecycle events

	//Discard charm data a few ticks after player leaves shard
	//(give time for save event to register)
	public void onQuit(Player p) {
		new BukkitRunnable() {

			@Override
			public void run() {
				if (!p.isOnline()) {
					mPlayerCharms.remove(p.getUniqueId());
					mPlayerCharmEffectMap.remove(p.getUniqueId());
					mPlayerAbilityEffectMap.remove(p.getUniqueId());
				}
			}

		}.runTaskLater(Plugin.getInstance(), 100);
	}

	//Store local charm data into plugin data
	public void onSave(PlayerSaveEvent event) {
		Player player = event.getPlayer();
		List<ItemStack> charms = mPlayerCharms.get(player.getUniqueId());
		if (charms != null) {
			JsonObject data = new JsonObject();
			JsonArray charmArray = new JsonArray();
			data.add(KEY_CHARMS, charmArray);
			Iterator<ItemStack> iterCharms = charms.iterator();
			while (iterCharms.hasNext()) {
				ItemStack charm = iterCharms.next();

				JsonObject charmData = new JsonObject();
				charmData.addProperty(KEY_ITEM, NBTItem.convertItemtoNBT(charm).toString());
				charmArray.add(charmData);
			}
			event.setPluginData(KEY_PLUGIN_DATA, data);
		}
	}

	//Load plugin data into local charm data
	public void onJoin(Player p) {
		JsonObject charmPluginData = MonumentaRedisSyncAPI.getPlayerPluginData(p.getUniqueId(), KEY_PLUGIN_DATA);
		if (charmPluginData != null) {
			if (charmPluginData.has(KEY_CHARMS)) {
				JsonArray charmArray = charmPluginData.getAsJsonArray(KEY_CHARMS);
				List<ItemStack> playerCharms = new ArrayList<>();
				for (JsonElement charmElement : charmArray) {
					JsonObject data = charmElement.getAsJsonObject();
					if (data.has(KEY_ITEM) && data.get(KEY_ITEM).isJsonPrimitive() && data.getAsJsonPrimitive(KEY_ITEM).isString()) {
						ItemStack item = NBTItem.convertNBTtoItem(new NBTContainer(data.getAsJsonPrimitive(KEY_ITEM).getAsString()));
						if (item != null) {
							playerCharms.add(item);
						}
					}
				}
				//Check if we actually loaded any charms
				if (playerCharms.size() > 0) {
					mPlayerCharms.put(p.getUniqueId(), playerCharms);
					//Recalculate the charm map based on loaded charms by calling update
					updateCharms(p, mPlayerCharms.get(p.getUniqueId()));
				}
			}
		}
	}

	//Methods called by the abilities

	public static double getRadius(Player player, String charmEffectName, double baseRadius) {
		double level = CharmManager.getInstance().getValueOfAttribute(player, charmEffectName + "%");
		return baseRadius * ((level / 100.0) + 1);
	}

	public static double getExtraDamage(Player player, String charmEffectName) {
		double level = CharmManager.getInstance().getValueOfAttribute(player, charmEffectName);
		return level;
	}

	public static int getExtraDuration(Player player, String charmEffectName) {
		double level = CharmManager.getInstance().getValueOfAttribute(player, charmEffectName);
		return (int) (level * 20);
	}

	public static double getExtraPercentDamage(Player player, String charmEffectName, double baseDamage) {
		double percentage = CharmManager.getInstance().getValueOfAttribute(player, charmEffectName + "%");
		return baseDamage * (1 + (percentage / 100.0));
	}

	public static int getCooldown(Player player, String charmEffectName, int baseCooldown) {
		double level = CharmManager.getInstance().getValueOfAttribute(player, charmEffectName + "%");
		return (int) (baseCooldown * ((level / 100.0) + 1));
	}

	public static double getLevel(Player player, String charmEffectName) {
		return CharmManager.getInstance().getValueOfAttribute(player, charmEffectName);
	}

	public static double getLevelPercent(Player player, String charmEffectName) {
		return CharmManager.getInstance().getValueOfAttribute(player, charmEffectName + "%");
	}

	public static double getLevelPercentDecimal(Player player, String charmEffectName) {
		return CharmManager.getInstance().getValueOfAttribute(player, charmEffectName + "%") / 100.0;
	}

	public static double getExtraPercentHealing(Player player, String charmEffectName, double baseHealing) {
		double percentage = CharmManager.getInstance().getValueOfAttribute(player, charmEffectName + "%");
		return baseHealing * (1 + (percentage / 100.0));
	}

	public static double getExtraPercent(Player player, String charmEffectName, double base) {
		double percentage = CharmManager.getInstance().getValueOfAttribute(player, charmEffectName + "%");
		return base * (1 + (percentage / 100.0));
	}

	//Calculates the final amount using both flat and percent modifiers, applying flat before percent
	public static double calculateFlatAndPercentValue(Player player, String charmEffectName, double baseValue) {
		double flatLevel = CharmManager.getInstance().getValueOfAttribute(player, charmEffectName);
		double percentLevel = CharmManager.getInstance().getValueOfAttribute(player, charmEffectName + "%");

		return (baseValue + flatLevel) * ((percentLevel / 100.0) + 1);
	}

	public static String getCharmEffectColor(boolean isPositive, String charmEffectName) {
		String outColor = "#4AC2E5";
		if (isPositive) {
			for (String s : mInstance.mFlippedColorEffectSubstrings) {
				if (charmEffectName.contains(s)) {
					outColor = "#D02E28";
					break;
				}
			}
		} else {
			for (String s : mInstance.mFlippedColorEffectSubstrings) {
				if (charmEffectName.contains(s)) {
					break;
				}
			}
			outColor = "#D02E28";
		}
		return outColor;
	}

	private static class CharmParsedInfo {
		public double mValue;
		public boolean mIsPercent;

		public CharmParsedInfo(double value, boolean isPercent) {
			mValue = value;
			mIsPercent = isPercent;
		}
	}
}
