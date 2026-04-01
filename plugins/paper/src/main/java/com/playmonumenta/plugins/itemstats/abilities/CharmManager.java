package com.playmonumenta.plugins.itemstats.abilities;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.alchemist.AlchemicalArtillery;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.abilities.alchemist.Bezoar;
import com.playmonumenta.plugins.abilities.alchemist.BrutalAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.EnergizingElixir;
import com.playmonumenta.plugins.abilities.alchemist.GruesomeAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.IronTincture;
import com.playmonumenta.plugins.abilities.alchemist.UnstableAmalgam;
import com.playmonumenta.plugins.abilities.alchemist.VolatileReaction;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.Panacea;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.TransmutationRing;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.WardingRemedy;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.EsotericEnhancements;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.ScorchedEarth;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.Taboo;
import com.playmonumenta.plugins.abilities.cleric.CelestialBlessing;
import com.playmonumenta.plugins.abilities.cleric.CleansingRain;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.abilities.cleric.DivineJustice;
import com.playmonumenta.plugins.abilities.cleric.HandOfLight;
import com.playmonumenta.plugins.abilities.cleric.HeavenlyBoon;
import com.playmonumenta.plugins.abilities.cleric.Illuminate;
import com.playmonumenta.plugins.abilities.cleric.SanctifiedArmor;
import com.playmonumenta.plugins.abilities.cleric.TouchofRadiance;
import com.playmonumenta.plugins.abilities.cleric.paladin.ChoirBells;
import com.playmonumenta.plugins.abilities.cleric.paladin.HolyJavelin;
import com.playmonumenta.plugins.abilities.cleric.paladin.LuminousInfusion;
import com.playmonumenta.plugins.abilities.cleric.seraph.EtherealAscension;
import com.playmonumenta.plugins.abilities.cleric.seraph.HallowedBeam;
import com.playmonumenta.plugins.abilities.cleric.seraph.KeeperVirtue;
import com.playmonumenta.plugins.abilities.cleric.seraph.Rejuvenation;
import com.playmonumenta.plugins.abilities.mage.ArcaneStrike;
import com.playmonumenta.plugins.abilities.mage.Channeling;
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
import com.playmonumenta.plugins.abilities.mage.elementalist.ElementalSpiritIce;
import com.playmonumenta.plugins.abilities.mage.elementalist.Starfall;
import com.playmonumenta.plugins.abilities.rogue.AdvancingShadows;
import com.playmonumenta.plugins.abilities.rogue.ByMyBlade;
import com.playmonumenta.plugins.abilities.rogue.DaggerThrow;
import com.playmonumenta.plugins.abilities.rogue.Dodging;
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
import com.playmonumenta.plugins.abilities.scout.Fleetfooted;
import com.playmonumenta.plugins.abilities.scout.HuntingCompanion;
import com.playmonumenta.plugins.abilities.scout.PartingShot;
import com.playmonumenta.plugins.abilities.scout.Sharpshooter;
import com.playmonumenta.plugins.abilities.scout.ShrapnelBomb;
import com.playmonumenta.plugins.abilities.scout.SteelTrap;
import com.playmonumenta.plugins.abilities.scout.Swiftness;
import com.playmonumenta.plugins.abilities.scout.Volley;
import com.playmonumenta.plugins.abilities.scout.WindBomb;
import com.playmonumenta.plugins.abilities.scout.hunter.Lockdown;
import com.playmonumenta.plugins.abilities.scout.hunter.PredatorStrike;
import com.playmonumenta.plugins.abilities.scout.hunter.QuiverStorm;
import com.playmonumenta.plugins.abilities.scout.ranger.GaleShot;
import com.playmonumenta.plugins.abilities.scout.ranger.RendingRazor;
import com.playmonumenta.plugins.abilities.scout.ranger.TacticalManeuver;
import com.playmonumenta.plugins.abilities.shaman.ChainLightning;
import com.playmonumenta.plugins.abilities.shaman.CleansingTotem;
import com.playmonumenta.plugins.abilities.shaman.EarthenTremor;
import com.playmonumenta.plugins.abilities.shaman.FlameTotem;
import com.playmonumenta.plugins.abilities.shaman.IgnitionDrive;
import com.playmonumenta.plugins.abilities.shaman.InterconnectedHavoc;
import com.playmonumenta.plugins.abilities.shaman.LightningTotem;
import com.playmonumenta.plugins.abilities.shaman.Spiritualism;
import com.playmonumenta.plugins.abilities.shaman.TotemicProjection;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.DecayedTotem;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.Devastation;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.SpiritcatcherOrbs;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.SpiritualCombos;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.TotemicConsecration;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.WhirlwindTotem;
import com.playmonumenta.plugins.abilities.warlock.AmplifyingHex;
import com.playmonumenta.plugins.abilities.warlock.CholericFlames;
import com.playmonumenta.plugins.abilities.warlock.Culling;
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
import com.playmonumenta.plugins.abilities.warrior.berserker.Bloodlust;
import com.playmonumenta.plugins.abilities.warrior.berserker.GloriousBattle;
import com.playmonumenta.plugins.abilities.warrior.berserker.MeteorSlam;
import com.playmonumenta.plugins.abilities.warrior.berserker.Rampage;
import com.playmonumenta.plugins.abilities.warrior.guardian.Bodyguard;
import com.playmonumenta.plugins.abilities.warrior.guardian.Challenge;
import com.playmonumenta.plugins.abilities.warrior.guardian.ShieldWall;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.depths.charmfactory.CharmFactory;
import com.playmonumenta.plugins.itemstats.enchantments.HexEater;
import com.playmonumenta.plugins.itemstats.enchantments.IntoxicatingWarmth;
import com.playmonumenta.plugins.itemstats.enchantments.JunglesNourishment;
import com.playmonumenta.plugins.itemstats.enchantments.LiquidCourage;
import com.playmonumenta.plugins.itemstats.enchantments.RageOfTheKeter;
import com.playmonumenta.plugins.itemstats.enchantments.Recoil;
import com.playmonumenta.plugins.itemstats.enchantments.TemporalBender;
import com.playmonumenta.plugins.itemupdater.ItemUpdateManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableItemNBT;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public class CharmManager {

	public enum CharmType {
		NORMAL(AbilityUtils.CHARM_POWER, KEY_PLUGIN_DATA, ItemStatUtils::isNormalCharm, ItemStatUtils::getCharmClassComponent),
		ZENITH(AbilityUtils.DEPTHS_CHARM_POWER, KEY_PLUGIN_DATA_DEPTHS, ItemStatUtils::isZenithCharm, CharmFactory::getZenithCharmRarityComponent);

		public final @NonNull Map<UUID, List<ItemStack>> mPlayerCharms; // This is never null. Any given instance of CharmType has this initialized to at LEAST an empty HashMap.

		private final String mScoreboard;
		private final String mPluginDataKey;
		private final Predicate<ItemStack> mCharmPredicate;
		private final @Nullable Function<ReadableItemNBT, Component> mLabelFunction;

		CharmType(String scoreboard, String pluginDataKey, Predicate<ItemStack> predicate, @Nullable Function<ReadableItemNBT, @Nullable Component> labelFunction) {
			mPlayerCharms = new HashMap<>();
			mScoreboard = scoreboard;
			mPluginDataKey = pluginDataKey;
			mCharmPredicate = predicate;
			mLabelFunction = labelFunction;
		}

		public int getTotalCharmPower(Player player) {
			//Give 9 charm power by default for zenith
			if (mScoreboard.equals(AbilityUtils.DEPTHS_CHARM_POWER)) {
				int charmPower = ScoreboardUtils.getScoreboardValue(player, mScoreboard).orElse(0);
				if (charmPower < 9) {
					ScoreboardUtils.setScoreboardValue(player, mScoreboard, 9);
					return 9;
				}
				return charmPower;
			}
			return ScoreboardUtils.getScoreboardValue(player, mScoreboard).orElse(0);
		}

		public boolean isCharm(@Nullable ItemStack item) {
			return mCharmPredicate.test(item);
		}

		public @Nullable Component getLabel(ReadableItemNBT nbt) {
			return mLabelFunction == null ? null : mLabelFunction.apply(nbt);
		}

		public String getPluginDataKey() {
			return mPluginDataKey;
		}
	}

	public static final String KEY_PLUGIN_DATA = "R3Charms";
	public static final String KEY_PLUGIN_DATA_DEPTHS = "R3DepthsCharms";
	public static final String KEY_CHARMS = "charms";
	public static final String KEY_ITEM = "item";
	public static final int MAX_CHARM_COUNT = 7;

	public static final CharmManager INSTANCE = new CharmManager();

	public List<String> mCharmEffectList;

	public List<String> mFlippedColorEffectSubstrings;

	public Map<UUID, Map<String, Double>> mPlayerCharmEffectMap;

	public CharmType mEnabledCharmType;

	// fields are initialised by called methods
	private CharmManager() {
		mPlayerCharmEffectMap = new HashMap<>();
		if (ServerProperties.getDepthsEnabled()) {
			mEnabledCharmType = CharmType.ZENITH;
		} else {
			mEnabledCharmType = CharmType.NORMAL;
		}
		loadCharmEffects();
		loadFlippedColorEffects();
	}

	public static CharmManager getInstance() {
		return INSTANCE;
	}

	// Adding new Charm Effects:
	// Add the string in this list, with other effects from the same ability/enchantment
	// If it is a "debuff" (i.e. a greater number is worse), then ALSO list it in the next method
	private void loadCharmEffects() {
		mCharmEffectList = Stream.concat(Arrays.stream(CharmEffects.values()).map(effect -> effect.mEffectName), Stream.of(
			// Custom Enchantments (Only Epic Enchantments Allowed)
			JunglesNourishment.CHARM_COOLDOWN,
			JunglesNourishment.CHARM_HEALTH,
			JunglesNourishment.CHARM_RESISTANCE,
			JunglesNourishment.CHARM_DURATION,
			RageOfTheKeter.CHARM_COOLDOWN,
			RageOfTheKeter.CHARM_DURATION,
			RageOfTheKeter.CHARM_DAMAGE,
			RageOfTheKeter.CHARM_SPEED,
			Recoil.CHARM_VELOCITY,
			IntoxicatingWarmth.CHARM_COOLDOWN,
			IntoxicatingWarmth.CHARM_DURATION,
			IntoxicatingWarmth.CHARM_SATURATION,
			TemporalBender.CHARM_COOLDOWN,
			TemporalBender.CHARM_COOLDOWN_REDUCTION,
			LiquidCourage.CHARM_CHARGES,
			LiquidCourage.CHARM_DURATION,
			LiquidCourage.CHARM_COOLDOWN,
			LiquidCourage.CHARM_RESISTANCE,
			HexEater.CHARM_DAMAGE,

			// Classes
			// Mage
			ManaLance.CHARM_DAMAGE,
			ManaLance.CHARM_CHARGES,
			ManaLance.CHARM_COOLDOWN,
			ManaLance.CHARM_RANGE,
			ManaLance.CHARM_SIZE,
			ThunderStep.CHARM_DAMAGE,
			ThunderStep.CHARM_COOLDOWN,
			ThunderStep.CHARM_DISTANCE,
			ThunderStep.CHARM_RADIUS,
			ThunderStep.CHARM_ENHANCEMENT_DURATION,
			PrismaticShield.CHARM_DURATION,
			PrismaticShield.CHARM_KNOCKBACK,
			PrismaticShield.CHARM_STUN,
			PrismaticShield.CHARM_ABSORPTION,
			PrismaticShield.CHARM_TRIGGER,
			PrismaticShield.CHARM_COOLDOWN,
			PrismaticShield.CHARM_RADIUS,
			PrismaticShield.CHARM_ENHANCE_DAMAGE,
			PrismaticShield.CHARM_ENHANCE_DURATION,
			PrismaticShield.CHARM_ENHANCE_HEALING,
			PrismaticShield.CHARM_ENHANCE_CDR,
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
			MagmaShield.CHARM_FIRE_BONUS,
			MagmaShield.CHARM_ABILITY_BONUS,
			MagmaShield.CHARM_DAMAGE_BONUS_DURATION,
			MagmaShield.CHARM_ENHANCE_DAMAGE,
			MagmaShield.CHARM_CHARGES,
			ArcaneStrike.CHARM_DAMAGE,
			ArcaneStrike.CHARM_RADIUS,
			ArcaneStrike.CHARM_BONUS,
			ArcaneStrike.CHARM_COOLDOWN,
			ArcaneStrike.CHARM_WEAKEN_POTENCY,
			ArcaneStrike.CHARM_WEAKEN_DURATION,
			ElementalArrows.CHARM_DAMAGE,
			ElementalArrows.CHARM_AREA_DAMAGE,
			ElementalArrows.CHARM_DURATION,
			ElementalArrows.CHARM_STUN_DURATION,
			ElementalArrows.CHARM_SLOWNESS,
			ElementalArrows.CHARM_RANGE,
			ElementalArrows.CHARM_THUNDER_COOLDOWN,
			Spellshock.CHARM_SPEED,
			Spellshock.CHARM_SLOW,
			Spellshock.CHARM_SPELL,
			Spellshock.CHARM_MELEE,
			Spellshock.CHARM_RADIUS,
			Spellshock.CHARM_ENHANCE_DAMAGE,
			Spellshock.CHARM_ENHANCE_SLOW,
			Spellshock.CHARM_ENHANCE_WEAK,
			Spellshock.CHARM_ENHANCE_DOT_DAMAGE,
			Spellshock.CHARM_ENHANCE_DOT_DURATION,
			Spellshock.CHARM_ENHANCE_LIGHTNING_DAMAGE,
			Spellshock.CHARM_ENHANCE_LIGHTNING_RANGE,
			AstralOmen.CHARM_DAMAGE,
			AstralOmen.CHARM_RANGE,
			AstralOmen.CHARM_MODIFIER,
			AstralOmen.CHARM_STACK,
			AstralOmen.CHARM_PULL,
			CosmicMoonblade.CHARM_CAP,
			CosmicMoonblade.CHARM_DEATH_CAP,
			CosmicMoonblade.CHARM_DAMAGE,
			CosmicMoonblade.CHARM_RANGE,
			CosmicMoonblade.CHARM_COOLDOWN,
			CosmicMoonblade.CHARM_SPELL_COOLDOWN,
			CosmicMoonblade.CHARM_DEATH_COOLDOWN,
			CosmicMoonblade.CHARM_SLASH,
			CosmicMoonblade.CHARM_SLASH_INTERVAL,
			SagesInsight.CHARM_SPEED,
			SagesInsight.CHARM_ABILITY,
			SagesInsight.CHARM_DECAY,
			SagesInsight.CHARM_STACKS,
			Blizzard.CHARM_COOLDOWN,
			Blizzard.CHARM_SLOW,
			Blizzard.CHARM_DURATION,
			Blizzard.CHARM_RANGE,
			Blizzard.CHARM_DAMAGE,
			Blizzard.CHARM_DELAY,
			Starfall.CHARM_COOLDOWN,
			Starfall.CHARM_DAMAGE,
			Starfall.CHARM_RANGE,
			Starfall.CHARM_FIRE,
			Starfall.CHARM_RADIUS,
			Starfall.CHARM_FALL_SPEED,
			ElementalSpiritFire.CHARM_COOLDOWN,
			ElementalSpiritFire.CHARM_DAMAGE,
			ElementalSpiritFire.CHARM_SIZE,
			ElementalSpiritFire.CHARM_DAMAGE2,
			ElementalSpiritFire.CHARM_COOLDOWN2,
			ElementalSpiritIce.CHARM_DAMAGE2,
			ElementalSpiritIce.CHARM_COOLDOWN2,
			Channeling.CHARM_DAMAGE,
			Channeling.CHARM_HITS,

			//Cleric
			CelestialBlessing.CHARM_COOLDOWN,
			CelestialBlessing.CHARM_DAMAGE,
			CelestialBlessing.CHARM_DURATION,
			CelestialBlessing.CHARM_SPEED,
			CelestialBlessing.CHARM_RADIUS,
			DivineJustice.CHARM_DAMAGE,
			DivineJustice.CHARM_ALLY,
			DivineJustice.CHARM_SELF,
			DivineJustice.CHARM_HEAL_RADIUS,
			DivineJustice.CHARM_ENHANCE_DAMAGE,
			DivineJustice.CHARM_ENHANCE_PRIME_DURATION,
			DivineJustice.CHARM_ENHANCE_COMBO_TIMER,
			TouchofRadiance.CHARM_RANGE,
			TouchofRadiance.CHARM_WEAKNESS,
			TouchofRadiance.CHARM_RADIUS,
			TouchofRadiance.CHARM_CDR,
			TouchofRadiance.CHARM_CDR_ALLY,
			TouchofRadiance.CHARM_STUN_DURATION,
			TouchofRadiance.CHARM_WEAKNESS_DURATION,
			TouchofRadiance.CHARM_ENHANCE_DAMAGE,
			TouchofRadiance.CHARM_ENHANCE_BLIND_DURATION,
			TouchofRadiance.CHARM_ENHANCE_FIRE_DURATION,
			TouchofRadiance.CHARM_DURATION,
			TouchofRadiance.CHARM_COOLDOWN,
			HeavenlyBoon.CHARM_CHANCE,
			HeavenlyBoon.CHARM_EFFECT_DURATION,
			HeavenlyBoon.CHARM_RADIUS,
			HeavenlyBoon.CHARM_HEAL_AMPLIFIER,
			HeavenlyBoon.CHARM_REGEN_AMPLIFIER,
			HeavenlyBoon.CHARM_SPEED_AMPLIFIER,
			HeavenlyBoon.CHARM_STRENGTH_AMPLIFIER,
			HeavenlyBoon.CHARM_RESIST_AMPLIFIER,
			HeavenlyBoon.CHARM_ABSORPTION_AMPLIFIER,
			HeavenlyBoon.CHARM_COOLDOWN_RECHARE_RATE,
			Crusade.CHARM_DURATION,
			CleansingRain.CHARM_COOLDOWN,
			CleansingRain.CHARM_RANGE,
			CleansingRain.CHARM_HEALING,
			CleansingRain.CHARM_HEALING_MAX_DEBUFFS,
			CleansingRain.CHARM_REDUCTION,
			CleansingRain.CHARM_DURATION,
			CleansingRain.CHARM_HEALING,
			CleansingRain.CHARM_HEALING_MAX_DEBUFFS,
			HandOfLight.CHARM_COOLDOWN,
			HandOfLight.CHARM_DAMAGE,
			HandOfLight.CHARM_MAX_MOBS,
			HandOfLight.CHARM_HEALING,
			HandOfLight.CHARM_RANGE,
			Illuminate.CHARM_COOLDOWN,
			Illuminate.CHARM_RANGE,
			Illuminate.CHARM_VELOCITY,
			Illuminate.CHARM_TRAIL_WIDTH,
			Illuminate.CHARM_TRAIL_DURATION,
			Illuminate.CHARM_SPEED_BUFF,
			Illuminate.CHARM_STRENGTH_BUFF,
			Illuminate.CHARM_DAMAGE,
			Illuminate.CHARM_RADIUS,
			Illuminate.CHARM_KNOCKBACK,
			Illuminate.CHARM_ENHANCE_DAMAGE,
			Illuminate.CHARM_ENHANCE_RADIUS,
			Illuminate.CHARM_ENHANCE_TICK_DELAY,
			HolyJavelin.CHARM_COOLDOWN,
			HolyJavelin.CHARM_DAMAGE,
			HolyJavelin.CHARM_RANGE,
			HolyJavelin.CHARM_SIZE,
			HolyJavelin.CHARM_VELOCITY,
			HolyJavelin.CHARM_DJ_DAMAGE,
			HolyJavelin.CHARM_DJ_PRIME,
			ChoirBells.CHARM_COOLDOWN,
			ChoirBells.CHARM_RANGE,
			ChoirBells.CHARM_SLOW,
			ChoirBells.CHARM_DAMAGE,
			ChoirBells.CHARM_VULN,
			ChoirBells.CHARM_WEAKEN,
			ChoirBells.CHARM_DURATION,
			ChoirBells.CHARM_TOLLS,
			ChoirBells.CHARM_TOLL_DELAY,
			LuminousInfusion.CHARM_RADIUS,
			LuminousInfusion.CHARM_DAMAGE,
			LuminousInfusion.CHARM_COOLDOWN,
			LuminousInfusion.CHARM_CHARGES,
			LuminousInfusion.CHARM_FIRE_DURATION,
			LuminousInfusion.CHARM_BLIND_RADIUS,
			LuminousInfusion.CHARM_BLIND_DURATION,
			LuminousInfusion.CHARM_KNOCKBACK,
			EtherealAscension.CHARM_DAMAGE,
			EtherealAscension.CHARM_RANGE,
			EtherealAscension.CHARM_RADIUS,
			EtherealAscension.CHARM_TRAVEL_SPEED,
			EtherealAscension.CHARM_KNOCKBACK,
			EtherealAscension.CHARM_DAMAGE_BONUS,
			EtherealAscension.CHARM_HASTE,
			EtherealAscension.CHARM_BUFF_DURATION,
			EtherealAscension.CHARM_THROW_RATE,
			EtherealAscension.CHARM_DURATION_EXTENSION,
			EtherealAscension.CHARM_DURATION_MAX_EXTENSION,
			EtherealAscension.CHARM_LAUNCH_KNOCKBACK,
			EtherealAscension.CHARM_LAUNCH_KNOCKBACK_RADIUS,
			EtherealAscension.CHARM_HOVER_HEIGHT,
			EtherealAscension.CHARM_DASH_VELOCITY,
			EtherealAscension.CHARM_DURATION,
			EtherealAscension.CHARM_COOLDOWN,
			HallowedBeam.CHARM_CHARGE,
			HallowedBeam.CHARM_COOLDOWN,
			HallowedBeam.CHARM_DAMAGE,
			HallowedBeam.CHARM_RANGE,
			HallowedBeam.CHARM_RADIUS,
			HallowedBeam.CHARM_RADIUS_SCALING,
			HallowedBeam.CHARM_SCALING_DISTANCE,
			HallowedBeam.CHARM_STUN,
			HallowedBeam.CHARM_HEAL,
			HallowedBeam.CHARM_RESISTANCE,
			HallowedBeam.CHARM_RESISTANCE_DURATION,
			KeeperVirtue.CHARM_DAMAGE,
			KeeperVirtue.CHARM_HARMING_RADIUS,
			KeeperVirtue.CHARM_FIRE_DURATION,
			KeeperVirtue.CHARM_ABSORPTION,
			KeeperVirtue.CHARM_ABSORPTION_DURATION,
			KeeperVirtue.CHARM_HIT_NEGATIONS,
			KeeperVirtue.CHARM_HIT_NEGATION_DURATION,
			KeeperVirtue.CHARM_TOR_DAMAGE,
			KeeperVirtue.CHARM_TOR_STUN_DURATION,
			KeeperVirtue.CHARM_ACTION_TIME,
			KeeperVirtue.CHARM_ACTION_RANGE,
			KeeperVirtue.CHARM_REDIRECT_RANGE,
			KeeperVirtue.CHARM_HARMING_COOLDOWN,
			KeeperVirtue.CHARM_SHIELDING_COOLDOWN,
			KeeperVirtue.CHARM_HARMING,
			KeeperVirtue.CHARM_SHIELDING,
			Rejuvenation.CHARM_RADIUS,
			Rejuvenation.CHARM_HEALING,
			Rejuvenation.CHARM_THRESHOLD,
			SanctifiedArmor.CHARM_COOLDOWN,
			SanctifiedArmor.CHARM_RESISTANCE,
			SanctifiedArmor.CHARM_KBR,
			SanctifiedArmor.CHARM_DURATION,
			SanctifiedArmor.CHARM_SLOW,
			SanctifiedArmor.CHARM_SLOW_DURATION,
			SanctifiedArmor.CHARM_ELITE_DURATION,
			SanctifiedArmor.CHARM_ENHANCE_HEAL,

			//Rogue
			AdvancingShadows.CHARM_COOLDOWN,
			AdvancingShadows.CHARM_DAMAGE,
			AdvancingShadows.CHARM_DURATION,
			AdvancingShadows.CHARM_KNOCKBACK,
			AdvancingShadows.CHARM_KNOCKBACK_RADIUS,
			AdvancingShadows.CHARM_RANGE,
			AdvancingShadows.CHARM_ENHANCEMENT_TIMER,
			AdvancingShadows.CHARM_KILL_TIMER,
			AdvancingShadows.CHARM_ENHANCEMENT_MULTIPLIER,
			ByMyBlade.CHARM_COOLDOWN,
			ByMyBlade.CHARM_DAMAGE,
			ByMyBlade.CHARM_ATTACK_SPEED_DURATION,
			ByMyBlade.CHARM_ATTACK_SPEED_AMPLIFIER,
			ByMyBlade.CHARM_HASTE_AMPLIFIER,
			ByMyBlade.CHARM_HEALTH,
			ByMyBlade.CHARM_ELITE_HEALTH,
			DaggerThrow.CHARM_COOLDOWN,
			DaggerThrow.CHARM_DAMAGE,
			DaggerThrow.CHARM_RANGE,
			DaggerThrow.CHARM_VULN,
			DaggerThrow.CHARM_VULN_DURATION,
			DaggerThrow.CHARM_SILENCE_DURATION,
			DaggerThrow.CHARM_RECAST_DURATION,
			DaggerThrow.CHARM_RECAST_MULTIPLIER,
			DaggerThrow.CHARM_DAGGERS,
			Dodging.CHARM_COOLDOWN,
			Dodging.CHARM_SPEED,
			Dodging.CHARM_DURATION,
			Dodging.CHARM_MAGIC_DODGING_COOLDOWN,
			EscapeDeath.CHARM_ABSORPTION,
			EscapeDeath.CHARM_COOLDOWN,
			EscapeDeath.CHARM_SPEED,
			EscapeDeath.CHARM_JUMP,
			EscapeDeath.CHARM_DURATION,
			EscapeDeath.CHARM_RADIUS,
			EscapeDeath.CHARM_STUN_DURATION,
			EscapeDeath.CHARM_HUNT_RADIUS,
			EscapeDeath.CHARM_ELITE_HEALING,
			EscapeDeath.CHARM_ELITE_HUNT_DURATION,
			EscapeDeath.CHARM_HEALING,
			EscapeDeath.CHARM_REGENERATION_DURATION,
			Skirmisher.CHARM_DAMAGE,
			Skirmisher.CHARM_RADIUS,
			Skirmisher.CHARM_TARGETS,
			Skirmisher.CHARM_ENHANCEMENT_DAMAGE,
			Smokescreen.CHARM_COOLDOWN,
			Smokescreen.CHARM_RANGE,
			Smokescreen.CHARM_WEAKEN,
			Smokescreen.CHARM_SLOW,
			Smokescreen.CHARM_ENHANCEMENT_DURATION,
			Smokescreen.CHARM_EFFECT_DURATION,
			Smokescreen.CHARM_DAMAGE,
			ViciousCombos.CHARM_CDR,
			ViciousCombos.CHARM_RADIUS,
			ViciousCombos.CHARM_VULN,
			ViciousCombos.CHARM_WEAKEN,
			ViciousCombos.CHARM_DURATION,
			ViciousCombos.CHARM_DAMAGE_AMPLIFIER,
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
			DeadlyRonde.CHARM_STACK_GAIN,
			DeadlyRonde.CHARM_SPEED,
			DeadlyRonde.CHARM_DECAY_TIME,
			DeadlyRonde.CHARM_STACKS_REQ,
			DeadlyRonde.CHARM_ATTACK_SPEED_SCALING_PORTION,
			WindWalk.CHARM_CHARGE,
			WindWalk.CHARM_COOLDOWN,
			WindWalk.CHARM_COOLDOWN_REDUCTION,
			WindWalk.CHARM_DURATION,
			BodkinBlitz.CHARM_CHARGE,
			BodkinBlitz.CHARM_COOLDOWN,
			BodkinBlitz.CHARM_DAMAGE,
			BodkinBlitz.CHARM_DISTANCE,
			BodkinBlitz.CHARM_STEALTH,
			CloakAndDagger.CHARM_DAMAGE,
			CloakAndDagger.CHARM_STACKS,
			CloakAndDagger.CHARM_STACKS_GAIN,
			CloakAndDagger.CHARM_STEALTH,
			CoupDeGrace.CHARM_NORMAL,
			CoupDeGrace.CHARM_THRESHOLD,
			CoupDeGrace.CHARM_ELITE,

			//Warrior
			Bloodlust.CHARM_STACKS,
			Bloodlust.CHARM_THRESHOLD,
			BruteForce.CHARM_DAMAGE,
			BruteForce.CHARM_RADIUS,
			BruteForce.CHARM_KNOCKBACK,
			BruteForce.CHARM_WAVES,
			BruteForce.CHARM_WAVE_DAMAGE_RATIO,
			BruteForce.CHARM_WAVE_DELAY,
			CounterStrike.CHARM_DAMAGE,
			CounterStrike.CHARM_RADIUS,
			CounterStrike.CHARM_DURATION,
			CounterStrike.CHARM_DAMAGE_REDUCTION,
			CounterStrike.CHARM_ABSORPTION_DAMAGE_REDUCTION,
			CounterStrike.CHARM_KBR,
			DefensiveLine.CHARM_COOLDOWN,
			DefensiveLine.CHARM_DURATION,
			DefensiveLine.CHARM_KNOCKBACK,
			DefensiveLine.CHARM_REDUCTION,
			DefensiveLine.CHARM_RANGE,
			DefensiveLine.CHARM_RADIUS,
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
			ShieldBash.CHARM_PARRY_DURATION,
			ShieldBash.CHARM_CDR,
			ShieldBash.CHARM_KNOCKBACK,
			Toughness.CHARM_HEALTH,
			Toughness.CHARM_REDUCTION,
			Toughness.CHARM_HEALING,
			Toughness.CHARM_HEALTH_THRESHOLD,
			WeaponMastery.CHARM_REDUCTION,
			WeaponMastery.CHARM_SPEED,
			WeaponMastery.CHARM_WEAKEN,
			WeaponMastery.CHARM_DURATION,
			GloriousBattle.CHARM_DAMAGE,
			GloriousBattle.CHARM_PIERCE_DAMAGE,
			GloriousBattle.CHARM_VELOCITY,
			GloriousBattle.CHARM_RADIUS,
			GloriousBattle.CHARM_KNOCKBACK,
			GloriousBattle.CHARM_DURATION,
			GloriousBattle.CHARM_BLOODLUST_COST,
			GloriousBattle.CHARM_CRITICAL_DAMAGE,
			Rampage.CHARM_DAMAGE,
			Rampage.CHARM_DAMAGE_BUFF,
			Rampage.CHARM_MELEE_RESISTANCE,
			Rampage.CHARM_MAX_BLOODLUST_GAIN,
			Rampage.CHARM_COOLDOWN,
			Rampage.CHARM_DURATION_PER_STACK,
			Rampage.CHARM_KNOCKBACK,
			Rampage.CHARM_RADIUS,
			Rampage.CHARM_HEALING,
			Rampage.CHARM_BLOODLUST_COST,
			Rampage.CHARM_INITIAL_DURATION,
			Rampage.CHARM_MAX_DURATION,
			MeteorSlam.CHARM_SLAM_DAMAGE,
			MeteorSlam.CHARM_VELOCITY,
			MeteorSlam.CHARM_UP_DAMAGE,
			MeteorSlam.CHARM_CONE_ANGLE,
			MeteorSlam.CHARM_RANGE,
			MeteorSlam.CHARM_KNOCKBACK,
			MeteorSlam.CHARM_COOLDOWN,
			MeteorSlam.CHARM_THRESHOLD,
			MeteorSlam.CHARM_HEIGHT,
			MeteorSlam.CHARM_METEOR_SLAM_RADIUS,
			MeteorSlam.CHARM_BLOODLUST_COST,
			MeteorSlam.CHARM_GROUND_POUND_VELOCITY,
			MeteorSlam.CHARM_GROUND_POUND_BLOODLUST_COST,
			MeteorSlam.CHARM_GROUND_POUND_DAMAGE,
			MeteorSlam.CHARM_GROUND_POUND_RADIUS,
			MeteorSlam.CHARM_GROUND_POUND_FIRE_DURATION,
			MeteorSlam.CHARM_GROUND_POUND_KNOCKBACK,
			MeteorSlam.CHARM_GROUND_POUND_SLOWNESS_MULTIPLIER,
			MeteorSlam.CHARM_GROUND_POUND_SLOWNESS_DURATION,
			Bodyguard.CHARM_COOLDOWN,
			Bodyguard.CHARM_RADIUS,
			Bodyguard.CHARM_RANGE,
			Bodyguard.CHARM_ABSORPTION,
			Bodyguard.CHARM_ABSORPTION_DURATION,
			Bodyguard.CHARM_STUN_DURATION,
			Bodyguard.CHARM_KNOCKBACK,
			Challenge.CHARM_COOLDOWN,
			Challenge.CHARM_DAMAGE_PER,
			Challenge.CHARM_DAMAGE_MAX,
			Challenge.CHARM_ABSORPTION_PER,
			Challenge.CHARM_ABSORPTION_MAX,
			Challenge.CHARM_SPEED_PER,
			Challenge.CHARM_CDR_PER,
			Challenge.CHARM_DURATION,
			Challenge.CHARM_MAX_MOBS,
			Challenge.CHARM_RANGE,
			ShieldWall.CHARM_DAMAGE,
			ShieldWall.CHARM_COOLDOWN,
			ShieldWall.CHARM_DURATION,
			ShieldWall.CHARM_ANGLE,
			ShieldWall.CHARM_KNOCKBACK,
			ShieldWall.CHARM_HEIGHT,
			ShieldWall.CHARM_RADIUS,

			//Alchemist
			AlchemistPotions.CHARM_DAMAGE,
			AlchemistPotions.CHARM_RADIUS,
			AlchemistPotions.CHARM_CHARGES,
			AlchemistPotions.CHARM_RECHARGE_RATE,
			AlchemicalArtillery.CHARM_COOLDOWN,
			AlchemicalArtillery.CHARM_BOUNCE_COUNT,
			AlchemicalArtillery.CHARM_BOUNCE_DAMAGE_MULTIPLIER_INCREASE,
			AlchemicalArtillery.CHARM_BOUNCE_DAMAGE_RAW_INCREASE,
			AlchemicalArtillery.CHARM_BOUNCE_DAMAGE_FRACTION,
			AlchemicalArtillery.CHARM_DAMAGE,
			AlchemicalArtillery.CHARM_RADIUS,
			AlchemicalArtillery.CHARM_VELOCITY,
			AlchemicalArtillery.CHARM_SIZE,
			AlchemicalArtillery.CHARM_COST,
			Bezoar.CHARM_RADIUS,
			Bezoar.CHARM_REQUIREMENT,
			Bezoar.CHARM_DAMAGE,
			Bezoar.CHARM_DAMAGE_DURATION,
			Bezoar.CHARM_HEALING,
			Bezoar.CHARM_HEAL_DURATION,
			Bezoar.CHARM_LINGER_TIME,
			Bezoar.CHARM_DEBUFF_REDUCTION,
			Bezoar.CHARM_POTIONS,
			Bezoar.CHARM_PHILOSOPHER_STONE_SPAWN_RATE,
			Bezoar.CHARM_PHILOSOPHER_STONE_RECHARGE_RATE_BONUS,
			Bezoar.CHARM_PHILOSOPHER_STONE_RECHARGE_RATE_DURATION,
			Bezoar.CHARM_PHILOSOPHER_STONE_ABSORPTION,
			Bezoar.CHARM_PHILOSOPHER_STONE_ABSORPTION_DURATION,
			BrutalAlchemy.CHARM_DAMAGE_MULTIPLIER,
			BrutalAlchemy.CHARM_DURATION,
			BrutalAlchemy.CHARM_DOT_BASE_DAMAGE,
			BrutalAlchemy.CHARM_DOT_INCREASE_DAMAGE_FLAT,
			BrutalAlchemy.CHARM_DOT_INCREASE_DAMAGE_MULT,
			BrutalAlchemy.CHARM_DOT_EXPLOSION_DAMAGE_MULT,
			BrutalAlchemy.CHARM_ENHANCEMENT_ADDITIONAL_TICKS,
			BrutalAlchemy.CHARM_REFRESHES_NEEDED_TO_EXPLODE,
			EnergizingElixir.CHARM_SPEED,
			EnergizingElixir.CHARM_JUMP_BOOST,
			EnergizingElixir.CHARM_DURATION,
			EnergizingElixir.CHARM_COOLDOWN,
			EnergizingElixir.CHARM_DAMAGE_AMPLIFIER,
			EnergizingElixir.CHARM_ENHANCEMENT_ABSORPTION_AMOUNT,
			EnergizingElixir.CHARM_ENHANCEMENT_ABSORPTION_MAX,
			EnergizingElixir.CHARM_ENHANCEMENT_ABSORPTION_DURATION,
			EnergizingElixir.CHARM_ENHANCEMENT_DEBUFF_REDUCTION,
			GruesomeAlchemy.CHARM_DAMAGE_MULTIPLIER,
			GruesomeAlchemy.CHARM_DURATION,
			GruesomeAlchemy.CHARM_SLOWNESS,
			GruesomeAlchemy.CHARM_VULNERABILITY,
			GruesomeAlchemy.CHARM_WEAKEN,
			IronTincture.CHARM_COOLDOWN,
			IronTincture.CHARM_ABSORPTION,
			IronTincture.CHARM_DURATION,
			IronTincture.CHARM_RESISTANCE,
			IronTincture.CHARM_REFILL,
			IronTincture.CHARM_ALLY_REFILL,
			IronTincture.CHARM_VELOCITY,
			IronTincture.CHARM_ENHANCEMENT_EFFECT_RADIUS,
			IronTincture.CHARM_STUN_DURATION,
			IronTincture.CHARM_CHARGES,
			IronTincture.CHARM_ENHANCEMENT_ABSORPTION_ON_KILL_AMOUNT,
			IronTincture.CHARM_ENHANCEMENT_ABSORPTION_ON_KILL_MAX,
			IronTincture.CHARM_ENHANCEMENT_ABSORPTION_ON_KILL_DURATION,
			VolatileReaction.CHARM_COOLDOWN,
			VolatileReaction.CHARM_RADIUS_MULTIPLIER,
			VolatileReaction.CHARM_MAIN_DAMAGE_MULTIPLIER,
			VolatileReaction.CHARM_DETONATE_RADIUS,
			VolatileReaction.CHARM_DETONATE_DAMAGE_MULTIPLIER,
			VolatileReaction.CHARM_ENHANCEMENT_SPREAD_CAP,
			VolatileReaction.CHARM_ENHANCEMENT_DOT_POTENCY_PER_STACK,
			VolatileReaction.CHARM_ENHANCEMENT_DOT_TICKS_PER_STACK,
			VolatileReaction.CHARM_ENHANCEMENT_DOT_BASE_TICK_DELAY,
			VolatileReaction.CHARM_ENHANCEMENT_DOT_TICK_DELAY_PER_STACK,
			VolatileReaction.CHARM_ENHANCEMENT_DOT_SPREAD_RADIUS,
			UnstableAmalgam.CHARM_COOLDOWN,
			UnstableAmalgam.CHARM_DAMAGE,
			UnstableAmalgam.CHARM_DURATION,
			UnstableAmalgam.CHARM_KNOCKBACK_MOBS,
			UnstableAmalgam.CHARM_KNOCKBACK_PLAYERS,
			UnstableAmalgam.CHARM_RADIUS,
			UnstableAmalgam.CHARM_RANGE,
			UnstableAmalgam.CHARM_INSTABILITY_DURATION,
			UnstableAmalgam.CHARM_POTION_DAMAGE,
			Panacea.CHARM_DAMAGE,
			Panacea.CHARM_COOLDOWN,
			Panacea.CHARM_RADIUS,
			Panacea.CHARM_ABSORPTION_PLAYERS,
			Panacea.CHARM_ABSORPTION_MOBS,
			Panacea.CHARM_ABSORPTION_DURATION,
			Panacea.CHARM_ABSORPTION_MAX,
			Panacea.CHARM_SLOW_DURATION,
			Panacea.CHARM_MOVEMENT_DURATION,
			Panacea.CHARM_MOVEMENT_SPEED,
			Panacea.CHARM_MAX_MOB_SHIELD_HITS,
			Panacea.CHARM_MOB_SHIELD_RADIUS,
			Panacea.CHARM_DEBUFF_REDUCTION,
			TransmutationRing.CHARM_COOLDOWN,
			TransmutationRing.CHARM_DURATION,
			TransmutationRing.CHARM_RADIUS,
			TransmutationRing.CHARM_DAMAGE_AMPLIFIER,
			TransmutationRing.CHARM_MAX_KILLS,
			TransmutationRing.CHARM_DAMAGE,
			TransmutationRing.CHARM_ABSORPTION_PER_KILL,
			TransmutationRing.CHARM_ABSORPTION_DURATION,
			TransmutationRing.CHARM_POTION_REFUND_PER_KILL,
			TransmutationRing.CHARM_EXTRA_BUFF_DURATION_ON_MAX_STACKS,
			WardingRemedy.CHARM_COOLDOWN,
			WardingRemedy.CHARM_PULSES,
			WardingRemedy.CHARM_DELAY,
			WardingRemedy.CHARM_RADIUS,
			WardingRemedy.CHARM_ABSORPTION,
			WardingRemedy.CHARM_MAX_ABSORPTION,
			WardingRemedy.CHARM_ABSORPTION_DURATION,
			WardingRemedy.CHARM_RESISTANCE,
			EsotericEnhancements.CHARM_COOLDOWN,
			EsotericEnhancements.CHARM_DAMAGE,
			EsotericEnhancements.CHARM_SLOW,
			EsotericEnhancements.CHARM_DURATION,
			EsotericEnhancements.CHARM_RADIUS,
			EsotericEnhancements.CHARM_REACTION_TIME,
			EsotericEnhancements.CHARM_FUSE,
			EsotericEnhancements.CHARM_SPEED,
			EsotericEnhancements.CHARM_CREEPER,
			EsotericEnhancements.CHARM_KNOCKBACK,
			ScorchedEarth.CHARM_COOLDOWN,
			ScorchedEarth.CHARM_CHARGES,
			ScorchedEarth.CHARM_RADIUS,
			ScorchedEarth.CHARM_DURATION,
			ScorchedEarth.CHARM_SCORCH_ON_ENTER,
			ScorchedEarth.CHARM_SLOWNESS,
			ScorchedEarth.CHARM_WEAKNESS,
			ScorchedEarth.CHARM_SHRAPNEL_COUNT,
			ScorchedEarth.CHARM_SHRAPNEL_RADIUS,
			ScorchedEarth.CHARM_SCORCHED_DURATION,
			ScorchedEarth.CHARM_SCORCHED_MAX_DURATION,
			ScorchedEarth.CHARM_SCORCHED_DAMAGE,
			Taboo.CHARM_COOLDOWN,
			Taboo.CHARM_SELF_DAMAGE_RAMPING,
			Taboo.CHARM_MISSING_HEALTH_FRACTION_PER_ABSORPTION,
			Taboo.CHARM_ABSORPTION_ON_DEACTIVATION_AMOUNT,
			Taboo.CHARM_ABSORPTION_ON_DEACTIVATION_MAX,
			Taboo.CHARM_ABSORPTION_ON_DEACTIVATION_DURATION,
			Taboo.CHARM_DAMAGE,
			Taboo.CHARM_KNOCKBACK_RESISTANCE,
			Taboo.CHARM_RECHARGE_RATE,
			Taboo.CHARM_SELF_DAMAGE,
			//Warlock
			AmplifyingHex.CHARM_CONE,
			AmplifyingHex.CHARM_COOLDOWN,
			AmplifyingHex.CHARM_DAMAGE,
			AmplifyingHex.CHARM_RANGE,
			AmplifyingHex.CHARM_POTENCY,
			AmplifyingHex.CHARM_POTENCY_CAP,
			AmplifyingHex.CHARM_ENHANCE_HEALTH,
			AmplifyingHex.CHARM_ENHANCE_DAMAGE,
			AmplifyingHex.CHARM_MAX_DEBUFFS,
			CholericFlames.CHARM_COOLDOWN,
			CholericFlames.CHARM_DAMAGE,
			CholericFlames.CHARM_DURATION,
			CholericFlames.CHARM_KNOCKBACK,
			CholericFlames.CHARM_RANGE,
			CholericFlames.CHARM_INFERNO_CAP,
			CholericFlames.CHARM_ENHANCEMENT_RADIUS,
			CursedWound.CHARM_DAMAGE,
			CursedWound.CHARM_DOT,
			CursedWound.CHARM_CAP,
			CursedWound.CHARM_RADIUS,
			GraspingClaws.CHARM_COOLDOWN,
			GraspingClaws.CHARM_PROJ_SPEED,
			GraspingClaws.CHARM_PULL_STRENGTH,
			GraspingClaws.CHARM_PULL_DAMAGE,
			GraspingClaws.CHARM_PULL_RADIUS,
			GraspingClaws.CHARM_SLOW,
			GraspingClaws.CHARM_SLOW_DURATION,
			GraspingClaws.CHARM_CLEAVE_FLAT_DAMAGE,
			GraspingClaws.CHARM_CLEAVE_PERCENT_DAMAGE,
			GraspingClaws.CHARM_CLEAVE_RADIUS,
			GraspingClaws.CHARM_CAGE_RADIUS,
			GraspingClaws.CHARM_CAGE_HEALING,
			GraspingClaws.CHARM_CAGE_DURATION,
			MelancholicLament.CHARM_COOLDOWN,
			MelancholicLament.CHARM_RADIUS,
			MelancholicLament.CHARM_RECOVERY,
			MelancholicLament.CHARM_WEAKNESS,
			MelancholicLament.CHARM_WEAKNESS_DURATION,
			MelancholicLament.CHARM_SILENCE_RADIUS,
			MelancholicLament.CHARM_SILENCE_DURATION,
			MelancholicLament.CHARM_ENHANCE_RADIUS,
			MelancholicLament.CHARM_ENHANCE_DAMAGE,
			MelancholicLament.CHARM_ENHANCE_MAX_MOBS,
			MelancholicLament.CHARM_ENHANCE_DURATION,
			PhlegmaticResolve.CHARM_ALLY,
			PhlegmaticResolve.CHARM_RANGE,
			PhlegmaticResolve.CHARM_RESIST,
			PhlegmaticResolve.CHARM_KBR,
			PhlegmaticResolve.CHARM_ABILITY_CAP,
			PhlegmaticResolve.CHARM_ENHANCE_DAMAGE,
			PhlegmaticResolve.CHARM_ENHANCE_RADIUS,
			SanguineHarvest.CHARM_COOLDOWN,
			SanguineHarvest.CHARM_HEAL,
			SanguineHarvest.CHARM_KNOCKBACK,
			SanguineHarvest.CHARM_RADIUS,
			SanguineHarvest.CHARM_RANGE,
			SanguineHarvest.CHARM_DAMAGE_BOOST,
			SanguineHarvest.CHARM_BLIGHT_DURATION,
			SanguineHarvest.CHARM_BLIGHT_VULN_PER_DEBUFF,
			SanguineHarvest.CHARM_MARKS,
			SoulRend.CHARM_COOLDOWN,
			SoulRend.CHARM_HEAL,
			SoulRend.CHARM_PACT_HEAL,
			SoulRend.CHARM_MARK_DURATION,
			SoulRend.CHARM_MARK_COUNT,
			SoulRend.CHARM_ALLY,
			SoulRend.CHARM_RADIUS,
			SoulRend.CHARM_ABSORPTION_CAP,
			SoulRend.CHARM_ABSORPTION_DURATION,
			HauntingShades.CHARM_COOLDOWN,
			HauntingShades.CHARM_DURATION,
			HauntingShades.CHARM_HEALING,
			HauntingShades.CHARM_RADIUS,
			HauntingShades.CHARM_VULN,
			HauntingShades.CHARM_DAMAGE,
			RestlessSouls.CHARM_CAP,
			RestlessSouls.CHARM_DAMAGE,
			RestlessSouls.CHARM_DURATION,
			RestlessSouls.CHARM_RADIUS,
			RestlessSouls.CHARM_DEBUFF_RANGE,
			RestlessSouls.CHARM_DEBUFF_DURATION,
			RestlessSouls.CHARM_SPEED,
			WitheringGaze.CHARM_COOLDOWN,
			WitheringGaze.CHARM_DAMAGE,
			WitheringGaze.CHARM_STUN,
			WitheringGaze.CHARM_DOT,
			WitheringGaze.CHARM_RANGE,
			WitheringGaze.CHARM_CONE,
			DarkPact.CHARM_ABSORPTION,
			DarkPact.CHARM_ATTACK_SPEED,
			DarkPact.CHARM_CAP,
			DarkPact.CHARM_DAMAGE,
			DarkPact.CHARM_DURATION,
			DarkPact.CHARM_REFRESH,
			DarkPact.CHARM_COOLDOWN,
			JudgementChain.CHARM_COOLDOWN,
			JudgementChain.CHARM_CHARGES,
			JudgementChain.CHARM_RANGE,
			JudgementChain.CHARM_CHAIN_DURATION,
			JudgementChain.CHARM_CHAIN_DAMAGE,
			JudgementChain.CHARM_SLOWNESS,
			JudgementChain.CHARM_WEAKNESS,
			JudgementChain.CHARM_DEBUFF_DURATION,
			JudgementChain.CHARM_EXTRA_TARGETS,
			JudgementChain.CHARM_EXTRA_TARGET_RADIUS,
			VoodooBonds.CHARM_COOLDOWN,
			VoodooBonds.CHARM_CHARGES,
			VoodooBonds.CHARM_DAMAGE,
			VoodooBonds.CHARM_PIN_DAMAGE,
			VoodooBonds.CHARM_PIN_RANGE,
			VoodooBonds.CHARM_CURSE_DAMAGE,
			VoodooBonds.CHARM_CURSE_RADIUS,
			VoodooBonds.CHARM_CURSE_SPREAD_COUNT,
			VoodooBonds.CHARM_CURSE_SPREAD_RADIUS,
			VoodooBonds.CHARM_CURSE_DURATION,
			VoodooBonds.CHARM_PROTECT_DURATION,
			VoodooBonds.CHARM_RECEIVED_DAMAGE,
			Culling.CHARM_DURATION,
			Culling.CHARM_RESISTANCE,

			//Scout
			Fleetfooted.CHARM_SPEED,
			PartingShot.CHARM_COOLDOWN,
			PartingShot.CHARM_TRIGGER_THRESHOLD,
			PartingShot.CHARM_DURATION,
			PartingShot.CHARM_PARTING_RADIUS,
			PartingShot.CHARM_RECOIL_STRENGTH,
			PartingShot.CHARM_KNOCKBACK,
			PartingShot.CHARM_FALL_NEGATION,
			PartingShot.CHARM_WEAKNESS_DURATION,
			PartingShot.CHARM_WEAKNESS_AMPLIFIER,
			PartingShot.CHARM_STAGGER_DURATION,
			PartingShot.CHARM_BUFF_DURATION,
			PartingShot.CHARM_RECHARGE,
			PartingShot.CHARM_VULN_AMPLIFIER,
			PartingShot.CHARM_REVEAL_DURATION,
			PartingShot.CHARM_REVEAL_RADIUS,
			PartingShot.CHARM_DUMMY_STUN,
			HuntingCompanion.CHARM_DAMAGE,
			HuntingCompanion.CHARM_RANGE,
			HuntingCompanion.CHARM_POUNCE_DAMAGE,
			SteelTrap.CHARM_COOLDOWN,
			SteelTrap.CHARM_DAMAGE,
			SteelTrap.CHARM_RADIUS,
			SteelTrap.CHARM_STAGGER_DURATION,
			SteelTrap.CHARM_DURATION,
			SteelTrap.CHARM_PRIMING_DURATION,
			SteelTrap.CHARM_VELOCITY,
			SteelTrap.CHARM_CHARGES,
			SteelTrap.CHARM_TRIGGER_RADIUS,
			SteelTrap.CHARM_KNOCKBACK,
			SteelTrap.CHARM_VULN,
			SteelTrap.CHARM_VULN_DURATION,
			HuntingCompanion.CHARM_FOXES,
			HuntingCompanion.CHARM_SPEED,
			HuntingCompanion.CHARM_POUNCE_COOLDOWN,
			HuntingCompanion.CHARM_HEALING,
			HuntingCompanion.CHARM_POUNCE_RADIUS,
			Sharpshooter.CHARM_STACK_DAMAGE,
			Sharpshooter.CHARM_STACKS,
			Sharpshooter.CHARM_RETRIEVAL,
			Sharpshooter.CHARM_DECAY,
			Sharpshooter.CHARM_MISS,
			Sharpshooter.CHARM_HIT,
			Sharpshooter.CHARM_PROJ_SPEED,
			Sharpshooter.CHARM_PIERCE,
			Swiftness.CHARM_JUMP_BOOST,
			Swiftness.CHARM_COOLDOWN,
			Swiftness.CHARM_DOUBLE_JUMP_STRENGTH,
			Swiftness.CHARM_DASH_VULNERABILITY_AMPLIFIER,
			Swiftness.CHARM_DASH_VULNERABILITY_DURATION,
			Swiftness.CHARM_DASH_RESISTANCE_DURATION,
			Volley.CHARM_COOLDOWN,
			Volley.CHARM_ARROWS,
			Volley.CHARM_DAMAGE,
			Volley.CHARM_PIERCING,
			Volley.CHARM_CHARGES,
			Volley.CHARM_MULTISHOT_LEVEL,
			Volley.CHARM_MULTISHOT_SHOT,
			WindBomb.CHARM_COOLDOWN,
			WindBomb.CHARM_DAMAGE,
			WindBomb.CHARM_RADIUS,
			WindBomb.CHARM_DURATION,
			WindBomb.CHARM_PULL,
			WindBomb.CHARM_SLOW_FALL_DURATION,
			WindBomb.CHARM_SIZE,
			WindBomb.CHARM_VORTEX_DURATION,
			WindBomb.CHARM_VORTEX_RADIUS,
			WindBomb.CHARM_VORTEX_HEIGHT,
			ShrapnelBomb.CHARM_BOMB_DAMAGE,
			ShrapnelBomb.CHARM_BOMB_ENHANCEMENT_DAMAGE,
			ShrapnelBomb.CHARM_SHRAPNEL_DAMAGE,
			ShrapnelBomb.CHARM_BOMB_RADIUS,
			ShrapnelBomb.CHARM_BOMB_ENHANCEMENT_RADIUS,
			ShrapnelBomb.CHARM_BOMB_VELOCITY,
			ShrapnelBomb.CHARM_SHRAPNEL_COUNT,
			ShrapnelBomb.CHARM_SHRAPNEL_DISTANCE,
			ShrapnelBomb.CHARM_SHRAPNEL_SPREAD,
			ShrapnelBomb.CHARM_STAGGER_DURATION,
			ShrapnelBomb.CHARM_RANGE,
			ShrapnelBomb.CHARM_DAMAGE_BOOST,
			ShrapnelBomb.CHARM_DAMAGE_BOOST_DURATION,
			ShrapnelBomb.CHARM_DAMAGE_BOOST_HITS,
			ShrapnelBomb.CHARM_KNOCKBACK,
			ShrapnelBomb.CHARM_COOLDOWN,
			Lockdown.CHARM_SHOT_COUNT,
			Lockdown.CHARM_KNOCKBACK,
			Lockdown.CHARM_DAMAGE,
			Lockdown.CHARM_MAX_DISTANCE,
			Lockdown.CHARM_CHARGE_TIME,
			Lockdown.CHARM_INITIAL_CHARGE_TIME,
			Lockdown.CHARM_RADIUS,
			Lockdown.CHARM_PIERCE,
			Lockdown.CHARM_COOLDOWN,
			Lockdown.CHARM_KILL_BONUS,
			Lockdown.CHARM_MIDAIR_DURATION,
			PredatorStrike.CHARM_DAMAGE,
			PredatorStrike.CHARM_RADIUS,
			PredatorStrike.CHARM_COOLDOWN,
			PredatorStrike.CHARM_RANGE,
			PredatorStrike.CHARM_KNOCKBACK,
			PredatorStrike.CHARM_PIERCING,
			PredatorStrike.CHARM_SPLINTER_RADIUS,
			PredatorStrike.CHARM_SPLINTER_CONE,
			PredatorStrike.CHARM_SPLINTER_REQUIREMENT,
			PredatorStrike.CHARM_COOLDOWN_REDUCTION,
			QuiverStorm.CHARM_DAMAGE,
			QuiverStorm.CHARM_PASSIVE_ARROW,
			QuiverStorm.CHARM_MAX_STACKS,
			QuiverStorm.CHARM_DELAY,
			QuiverStorm.CHARM_PIERCE,
			QuiverStorm.CHARM_PSTRIKE_REFUND,
			QuiverStorm.CHARM_LOCKDOWN_REFUND,
			GaleShot.CHARM_DAMAGE_FLAT,
			GaleShot.CHARM_DAMAGE_PERCENT,
			GaleShot.CHARM_RANGE,
			GaleShot.CHARM_ABILITY_REQUIREMENT,
			GaleShot.CHARM_SHOT_REQUIREMENT,
			GaleShot.CHARM_DURATION,
			GaleShot.CHARM_SLOWNESS_DURATION,
			GaleShot.CHARM_SLOWNESS_AMPLIFIER,
			GaleShot.CHARM_COUNT,
			TacticalManeuver.CHARM_CHARGES,
			TacticalManeuver.CHARM_COOLDOWN,
			TacticalManeuver.CHARM_VELOCITY,
			TacticalManeuver.CHARM_RADIUS,
			TacticalManeuver.CHARM_DURATION,
			TacticalManeuver.CHARM_COOLDOWN_REDUCTION,
			TacticalManeuver.CHARM_MARK_DURATION,
			TacticalManeuver.CHARM_WEAKNESS,
			TacticalManeuver.CHARM_WEAKNESS_DURATION,
			RendingRazor.CHARM_DAMAGE,
			RendingRazor.CHARM_COOLDOWN,
			RendingRazor.CHARM_SPEED,
			RendingRazor.CHARM_COOLDOWN_REDUCTION,
			RendingRazor.CHARM_COOLDOWN_REDUCTION_DURATION,
			RendingRazor.CHARM_RAZOR_RANGE,
			RendingRazor.CHARM_RAZOR_SIZE,
			RendingRazor.CHARM_RAZOR_PIERCE,
			RendingRazor.CHARM_CHARGES,
			RendingRazor.CHARM_KNOCKBACK,
			//Shaman
			CleansingTotem.CHARM_DURATION,
			CleansingTotem.CHARM_RADIUS,
			CleansingTotem.CHARM_COOLDOWN,
			CleansingTotem.CHARM_HEALING,
			CleansingTotem.CHARM_ENHANCE_HEALING,
			CleansingTotem.CHARM_ENHANCE_ABSORB_MAX,
			CleansingTotem.CHARM_PULSE_DELAY,
			ChainLightning.CHARM_COOLDOWN,
			ChainLightning.CHARM_DAMAGE,
			ChainLightning.CHARM_ENHANCEMENT_DAMAGE,
			ChainLightning.CHARM_RADIUS,
			ChainLightning.CHARM_TARGETS,
			ChainLightning.CHARM_CHARGES,
			ChainLightning.CHARM_KNOCKBACK,
			ChainLightning.CHARM_INITIAL_RANGE,
			ChainLightning.CHARM_SUPPORT_TOTEM_EFFICIENCY,
			ChainLightning.CHARM_OFFENSIVE_TOTEM_EFFICIENCY,
			EarthenTremor.CHARM_DAMAGE,
			EarthenTremor.CHARM_COOLDOWN,
			EarthenTremor.CHARM_KNOCKUP,
			EarthenTremor.CHARM_RADIUS,
			EarthenTremor.CHARM_TOTEM_THROW_DAMAGE,
			EarthenTremor.CHARM_TOTEM_THROW_RADIUS,
			EarthenTremor.CHARM_ENHANCE_DURATION,
			EarthenTremor.CHARM_ENHANCE_MELEE,
			EarthenTremor.CHARM_ENHANCE_PROJ,
			EarthenTremor.CHARM_ENHANCE_RADIUS,
			EarthenTremor.CHARM_ROOT_DURATION,
			FlameTotem.CHARM_DURATION,
			FlameTotem.CHARM_RADIUS,
			FlameTotem.CHARM_COOLDOWN,
			FlameTotem.CHARM_ENHANCE_DAMAGE_BOOST,
			FlameTotem.CHARM_ABILITY_FLAT_BOOST,
			FlameTotem.CHARM_PULSE_DELAY,
			FlameTotem.CHARM_STACKING_DAMAGE,
			FlameTotem.CHARM_DAMAGE,
			InterconnectedHavoc.CHARM_DAMAGE,
			InterconnectedHavoc.CHARM_RANGE,
			InterconnectedHavoc.CHARM_ENHANCEMENT_KNOCKBACK,
			InterconnectedHavoc.CHARM_ENHANCEMENT_STUN,
			LightningTotem.CHARM_PULSE_DELAY,
			LightningTotem.CHARM_DURATION,
			LightningTotem.CHARM_RADIUS,
			LightningTotem.CHARM_COOLDOWN,
			LightningTotem.CHARM_DAMAGE,
			LightningTotem.CHARM_DAMAGE_P,
			LightningTotem.CHARM_ELITE_DAMAGE_P,
			LightningTotem.CHARM_DAMAGE_M,
			LightningTotem.CHARM_ELITE_DAMAGE_M,
			LightningTotem.CHARM_ADDITIONAL_DAMAGE,
			LightningTotem.CHARM_SHOCK_DURATION,
			LightningTotem.CHARM_STORM_DAMAGE,
			LightningTotem.CHARM_STORM_RADIUS,
			LightningTotem.CHARM_STORM_DURATION,
			LightningTotem.CHARM_STORM_PULSE_DELAY,
			Spiritualism.CHARM_HEALING,
			Spiritualism.CHARM_BONUS_HEALING,
			Spiritualism.CHARM_COOLDOWN_REFUND_PERCENT,
			Spiritualism.CHARM_COOLDOWN_REFUND_CAP,
			Spiritualism.CHARM_DAMAGE_BOOST,
			IgnitionDrive.CHARM_COOLDOWN,
			IgnitionDrive.CHARM_RADIUS,
			IgnitionDrive.CHARM_FIRE_DURATION,
			IgnitionDrive.CHARM_DAMAGE,
			IgnitionDrive.CHARM_LAUNCH_DISTANCE,
			IgnitionDrive.CHARM_STUN_LIMIT,
			IgnitionDrive.CHARM_STUN_DURATION,
			IgnitionDrive.CHARM_ENHANCE_UPPER_THRESHOLD,
			IgnitionDrive.CHARM_ENHANCE_COOLDOWN_REFRESH,
			IgnitionDrive.CHARM_ENHANCE_LOWER_THRESHOLD,
			IgnitionDrive.CHARM_ENHANCE_MAGIC_DMG,
			IgnitionDrive.CHARM_ENHANCE_MAGIC_DURATION,
			IgnitionDrive.CHARM_ENHANCE_STUN_DURATION,
			TotemicProjection.CHARM_COOLDOWN,
			TotemicProjection.CHARM_CHARGES,
			TotemicProjection.CHARM_DISTRIBUTION_RADIUS,
			DecayedTotem.CHARM_DURATION,
			DecayedTotem.CHARM_RADIUS,
			DecayedTotem.CHARM_COOLDOWN,
			DecayedTotem.CHARM_DAMAGE,
			DecayedTotem.CHARM_SLOWNESS,
			DecayedTotem.CHARM_TARGETS,
			DecayedTotem.CHARM_FLAME_TOTEM_DAMAGE_BUFF,
			DecayedTotem.CHARM_LIGHTNING_TOTEM_DAMAGE_BUFF,
			DecayedTotem.CHARM_PULSE_DELAY,
			Devastation.CHARM_DAMAGE,
			Devastation.CHARM_RADIUS,
			Devastation.CHARM_COOLDOWN,
			Devastation.CHARM_CDR,
			Devastation.CHARM_DECAY_DAMAGE,
			Devastation.CHARM_CLEANSE_DURATION,
			Devastation.CHARM_CLEANSE_WEAKEN,
			Devastation.CHARM_FIRE_STRENGTH,
			Devastation.CHARM_FIRE_STRENGTH_DURATION,
			Devastation.CHARM_LIGHTNING_DAMAGE,
			Devastation.CHARM_LIGHTNING_STUN,
			SpiritcatcherOrbs.CHARM_COUNT,
			SpiritcatcherOrbs.CHARM_VELOCITY,
			SpiritcatcherOrbs.CHARM_LIFETIME,
			SpiritcatcherOrbs.CHARM_SPIRITFLAME_DURATION,
			SpiritcatcherOrbs.CHARM_SPIRITFLAME_RANGE,
			SpiritcatcherOrbs.CHARM_SPIRITFLAME_DAMAGE,
			SpiritcatcherOrbs.CHARM_SPIRITFLAME_VULN,
			SpiritcatcherOrbs.CHARM_SPIRITFLAME_WEAKEN,
			SpiritcatcherOrbs.CHARM_MAX_STACKS,
			SpiritcatcherOrbs.CHARM_STACK_DECAY_TIME,
			SpiritcatcherOrbs.CHARM_IMBUED_DAMAGE_BONUS,
			TotemicConsecration.CHARM_COOLDOWN,
			TotemicConsecration.CHARM_CHARGES,
			TotemicConsecration.CHARM_DAMAGE,
			TotemicConsecration.CHARM_BONUS_DAMAGE,
			TotemicConsecration.CHARM_DURATION_PER_BONUS,
			TotemicConsecration.CHARM_ABSORPTION,
			TotemicConsecration.CHARM_HP_THRESHOLD,
			TotemicConsecration.CHARM_RADIUS_AMPLIFIER,
			TotemicConsecration.CHARM_RESISTANCE,
			TotemicConsecration.CHARM_SILENCE_DURATION,
			SpiritualCombos.CHARM_CRYSTAL_DAMAGE,
			SpiritualCombos.CHARM_CRYSTAL_RANGE,
			SpiritualCombos.CHARM_SHOT_COUNT,
			SpiritualCombos.CHARM_SHOT_DELAY,
			SpiritualCombos.CHARM_CRYSTAL_STACK_THRESHOLD,
			SpiritualCombos.CHARM_STACK_DECAY_TIME,
			SpiritualCombos.CHARM_SPEED,
			WhirlwindTotem.CHARM_DURATION,
			WhirlwindTotem.CHARM_RADIUS,
			WhirlwindTotem.CHARM_COOLDOWN,
			WhirlwindTotem.CHARM_CDR,
			WhirlwindTotem.CHARM_MAX_CDR,
			WhirlwindTotem.CHARM_SPEED,
			WhirlwindTotem.CHARM_DURATION_BOOST,
			WhirlwindTotem.CHARM_PULSE_DELAY
		)).toList();
	}

	private void loadFlippedColorEffects() {
		mFlippedColorEffectSubstrings = Stream.concat(Arrays.stream(CharmEffects.values()).filter(effect -> effect.mEffectCap < 0).map(effect -> effect.mEffectName), Stream.of(
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
			AstralOmen.CHARM_STACK,
			CosmicMoonblade.CHARM_COOLDOWN,
			SagesInsight.CHARM_STACKS,
			Blizzard.CHARM_COOLDOWN,
			Blizzard.CHARM_DELAY,
			Starfall.CHARM_COOLDOWN,
			ElementalSpiritFire.CHARM_COOLDOWN,
			CelestialBlessing.CHARM_COOLDOWN,
			CleansingRain.CHARM_COOLDOWN,
			TouchofRadiance.CHARM_COOLDOWN,
			HandOfLight.CHARM_COOLDOWN,
			HeavenlyBoon.CHARM_CHANCE_INCREASE_INTERVAL,
			SanctifiedArmor.CHARM_COOLDOWN,
			HolyJavelin.CHARM_COOLDOWN,
			ChoirBells.CHARM_COOLDOWN,
			ChoirBells.CHARM_TOLL_DELAY,
			LuminousInfusion.CHARM_COOLDOWN,
			Illuminate.CHARM_COOLDOWN,
			Illuminate.CHARM_ENHANCE_TICK_DELAY,
			EtherealAscension.CHARM_COOLDOWN,
			HallowedBeam.CHARM_COOLDOWN,
			KeeperVirtue.CHARM_HARMING_COOLDOWN,
			KeeperVirtue.CHARM_SHIELDING_COOLDOWN,
			KeeperVirtue.CHARM_ACTION_TIME,
			AdvancingShadows.CHARM_COOLDOWN,
			ByMyBlade.CHARM_COOLDOWN,
			DaggerThrow.CHARM_COOLDOWN,
			Dodging.CHARM_COOLDOWN,
			Dodging.CHARM_MAGIC_DODGING_COOLDOWN,
			EscapeDeath.CHARM_COOLDOWN,
			Smokescreen.CHARM_COOLDOWN,
			BladeDance.CHARM_COOLDOWN,
			DeadlyRonde.CHARM_STACKS_REQ,
			WindWalk.CHARM_COOLDOWN,
			BodkinBlitz.CHARM_COOLDOWN,
			Bloodlust.CHARM_THRESHOLD,
			BruteForce.CHARM_WAVE_DELAY,
			DefensiveLine.CHARM_COOLDOWN,
			Riposte.CHARM_COOLDOWN,
			ShieldBash.CHARM_COOLDOWN,
			Bodyguard.CHARM_COOLDOWN,
			Challenge.CHARM_COOLDOWN,
			ShieldWall.CHARM_COOLDOWN,
			MeteorSlam.CHARM_COOLDOWN,
			MeteorSlam.CHARM_THRESHOLD,
			MeteorSlam.CHARM_BLOODLUST_COST,
			MeteorSlam.CHARM_GROUND_POUND_BLOODLUST_COST,
			GloriousBattle.CHARM_BLOODLUST_COST,
			Rampage.CHARM_BLOODLUST_COST,
			Rampage.CHARM_COOLDOWN,
			BrutalAlchemy.CHARM_REFRESHES_NEEDED_TO_EXPLODE,
			Bezoar.CHARM_REQUIREMENT,
			IronTincture.CHARM_COOLDOWN,
			VolatileReaction.CHARM_COOLDOWN,
			VolatileReaction.CHARM_ENHANCEMENT_DOT_TICK_DELAY_PER_STACK,
			UnstableAmalgam.CHARM_COOLDOWN,
			Panacea.CHARM_COOLDOWN,
			TransmutationRing.CHARM_COOLDOWN,
			WardingRemedy.CHARM_COOLDOWN,
			WardingRemedy.CHARM_DELAY,
			EsotericEnhancements.CHARM_COOLDOWN,
			EsotericEnhancements.CHARM_FUSE,
			ScorchedEarth.CHARM_COOLDOWN,
			Taboo.CHARM_COOLDOWN,
			Taboo.CHARM_SELF_DAMAGE_RAMPING,
			Taboo.CHARM_SELF_DAMAGE,
			AlchemicalArtillery.CHARM_COOLDOWN,
			Bezoar.CHARM_PHILOSOPHER_STONE_SPAWN_RATE,
			AmplifyingHex.CHARM_COOLDOWN,
			AmplifyingHex.CHARM_ENHANCE_HEALTH,
			AmplifyingHex.CHARM_MAX_DEBUFFS,
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
			VoodooBonds.CHARM_RECEIVED_DAMAGE,
			PartingShot.CHARM_COOLDOWN,
			Sharpshooter.CHARM_MISS,
			ShrapnelBomb.CHARM_COOLDOWN,
			SteelTrap.CHARM_COOLDOWN,
			SteelTrap.CHARM_PRIMING_DURATION,
			HuntingCompanion.CHARM_POUNCE_COOLDOWN,
			Volley.CHARM_COOLDOWN,
			WindBomb.CHARM_COOLDOWN,
			Lockdown.CHARM_COOLDOWN,
			Lockdown.CHARM_CHARGE_TIME,
			Lockdown.CHARM_INITIAL_CHARGE_TIME,
			Swiftness.CHARM_COOLDOWN,
			PredatorStrike.CHARM_COOLDOWN,
			RendingRazor.CHARM_COOLDOWN,
			QuiverStorm.CHARM_DELAY,
			GaleShot.CHARM_ABILITY_REQUIREMENT,
			GaleShot.CHARM_SHOT_REQUIREMENT,
			TacticalManeuver.CHARM_COOLDOWN,
			RendingRazor.CHARM_COOLDOWN,
			CleansingTotem.CHARM_COOLDOWN,
			CleansingTotem.CHARM_PULSE_DELAY,
			ChainLightning.CHARM_COOLDOWN,
			FlameTotem.CHARM_COOLDOWN,
			FlameTotem.CHARM_PULSE_DELAY,
			EarthenTremor.CHARM_COOLDOWN,
			LightningTotem.CHARM_COOLDOWN,
			LightningTotem.CHARM_PULSE_DELAY,
			LightningTotem.CHARM_STORM_PULSE_DELAY,
			IgnitionDrive.CHARM_COOLDOWN,
			IgnitionDrive.CHARM_ENHANCE_UPPER_THRESHOLD,
			TotemicProjection.CHARM_COOLDOWN,
			DecayedTotem.CHARM_COOLDOWN,
			DecayedTotem.CHARM_PULSE_DELAY,
			Devastation.CHARM_COOLDOWN,
			WhirlwindTotem.CHARM_COOLDOWN,
			WhirlwindTotem.CHARM_PULSE_DELAY,
			SpiritualCombos.CHARM_SHOT_DELAY,
			SpiritualCombos.CHARM_CRYSTAL_STACK_THRESHOLD,
			TotemicConsecration.CHARM_DURATION_PER_BONUS,
			TotemicConsecration.CHARM_COOLDOWN,
			TotemicConsecration.CHARM_HP_THRESHOLD,
			ElementalSpiritIce.CHARM_COOLDOWN2,
			ElementalSpiritFire.CHARM_COOLDOWN2
		)).toList();
	}

	/**
	 * Returns the charms a player has equipped that are of type <code>charmType</code>
	 *
	 * @param player    The player
	 * @param charmType The type of charm to query
	 * @return The charms of the given type that the player currently has equipped.
	 */
	public @NonNull List<ItemStack> getCharms(Player player, CharmType charmType) {
		return charmType.mPlayerCharms.computeIfAbsent(player.getUniqueId(), key -> new ArrayList<>());
	}

	/**
	 * Add (equip) a charm onto a player.
	 * Returns false if passed a null player or if the charm is not valid.
	 *
	 * @param p         The player
	 * @param charm     The charm
	 * @param charmType The type of said charm.
	 * @return True if successful
	 */
	@Contract("null, _, _ -> false")
	public boolean addCharm(@Nullable Player p, ItemStack charm, CharmType charmType) {
		if (p == null || !validateCharm(p, charm, charmType)) {
			return false;
		}
		ItemStack charmCopy = new ItemStack(charm);
		getCharms(p, charmType).add(charmCopy);
		updateCharms(p, charmType);
		return true;
	}

	// This method validates the charm against the player's current charm list before actually adding it
	public boolean validateCharm(@Nullable Player p, @Nullable ItemStack charm, CharmType charmType) {
		//Check to make sure the added charm is a valid charm (check lore) and not a duplicate of one they already have!
		//Also make sure the charm list exists if we're checking against it
		//Also make sure the charm list has space for the new charm if it exists
		//Also make sure it's not a stack (only one item)
		//Also parse the charm's power budget and make sure adding it would not overflow
		//Also check there's no conflicts with locked stats

		//Check item stack for valid name and amount
		if (charm == null || p == null || charm.getAmount() != 1 || !charm.hasItemMeta() || !charm.getItemMeta().hasDisplayName()) {
			return false;
		}

		if (!charmType.isCharm(charm)) {
			return false;
		}

		// Charm Power Handling
		int charmPower = ItemStatUtils.getCharmPower(charm);
		if (charmPower > 0) {
			//Now check the player charms to make sure it is different from existing charms
			List<ItemStack> charms = charmType.mPlayerCharms.get(p.getUniqueId());
			if (charms != null) {
				//Check max charm count in list
				if (charms.size() >= MAX_CHARM_COUNT) {
					return false;
				}
				int powerBudget = 0;
				for (ItemStack c : charms) {
					//Check for conflicts with locked stats
					for (CharmManager.CharmParsedInfo info : readCharm(c)) {
						for (CharmManager.CharmParsedInfo info2 : readCharm(charm)) {
							if (info.mEffect.equals(info2.mEffect) && (info.mIsLocked || info2.mIsLocked)) {
								return false;
							}
						}
					}
					//Check naming of each charm
					if (Objects.equals(c.getItemMeta().displayName(), charm.getItemMeta().displayName())) {
						return false;
					}
					powerBudget += ItemStatUtils.getCharmPower(c);
				}
				//Check to see if adding the extra charm would exceed budget
				int totalBudget = charmType.getTotalCharmPower(p);
				return powerBudget + charmPower <= totalBudget;
			}
			return true;
		}
		return false;
	}

	public boolean removeCharm(@Nullable Player p, ItemStack charm, CharmType charmType) {
		if (p == null) {
			return false;
		}

		List<ItemStack> playerCharms = charmType.mPlayerCharms.get(p.getUniqueId());
		if (playerCharms != null) {
			if (playerCharms.remove(charm)) {
				updateCharms(p, charmType);
				return true;
			}
		}
		return false;
	}

	public boolean removeCharmBySlot(@Nullable Player p, int slot, CharmType charmType) {
		if (p == null) {
			return false;
		}
		List<ItemStack> charms = getCharms(p, charmType);
		return slot < charms.size() && null != charms.remove(slot);
	}

	public void clearCharms(@Nullable Player p, CharmType charmType) {
		if (p == null) {
			return;
		}
		getCharms(p, charmType).clear();
		updateCharms(p, charmType);
	}

	public void updateCharms(Player p, CharmType charmType) {
		updateCharms(p, charmType, getCharms(p, charmType));
	}

	public void updateCharms(@NonNull Player p, CharmType charmType, List<ItemStack> equippedCharms) {
		if (mEnabledCharmType != charmType) {
			return;
		}

		ItemUpdateManager.updateCharms(p, charmType, equippedCharms);

		UUID uuid = p.getUniqueId();

		//Calculate the map of effects to values
		Map<String, Double> allEffects = new HashMap<>();

		// Anticheat for locked+unlocked charms on the same stat at once, which can occur with item replacements
		Map<String, Boolean> statIsLocked = new HashMap<>();
		boolean badCombinationFound = false;

		for (ItemStack charm : equippedCharms) {
			if (charm == null || charm.getType() == Material.AIR) {
				continue;
			}
			for (CharmParsedInfo info : readCharm(charm)) {
				if (!mCharmEffectList.contains(info.mEffect)) {
					MMLog.warning("Unknown effect '" + info.mEffect + "' in charm '" + ItemUtils.getPlainName(charm) + "'!");
					continue;
				}
				if (statIsLocked.getOrDefault(info.mEffect, false) || (info.mIsLocked && statIsLocked.containsKey(info.mEffect))) {
					// found an invalid combination; either a previous charm had this stat locked,
					// or this charm has the stat locked and a previous charm had the stat at all
					badCombinationFound = true;
					break;
				}
				statIsLocked.put(info.mEffect, info.mIsLocked);
				//Combine all effects
				allEffects.merge(info.mEffect + (info.mIsPercent ? "%" : ""), info.mValue, (a, b) -> Math.ceil((a + b) * 1000) / 1000);
			}
			if (badCombinationFound) {
				allEffects.clear();
				p.sendMessage(Component.text("Your charms were temporarily disabled because of an invalid combination with locked charms. Remove one of the conflicting charms to fix this.", NamedTextColor.RED));
				break;
			}
		}

		//Then calculate the cap. Just checks the last character for in case an effect has a percent in it
		allEffects.replaceAll((a, b) -> Math.ceil(getValueOrCap(b, a.charAt(a.length() - 1) == '%' ? a.substring(0, a.length() - 1) : a, charmType) * 1000) / 1000);

		//Store to local map
		mPlayerCharmEffectMap.put(uuid, allEffects);

		//Refresh class of player
		AbilityManager.getManager().updatePlayerAbilities(p, true);
	}

	private static final Pattern CHARM_LINE_PATTERN = Pattern.compile("(# )?([-+]?\\d+(?:\\.\\d+)?)(%)? (.+)");

	//Helper method to parse item for charm effects
	private List<CharmParsedInfo> readCharm(ItemStack itemStack) {
		List<CharmParsedInfo> effects = new ArrayList<>();
		List<String> plainLoreLines = NBT.get(itemStack, (ReadableItemNBT nbt) -> ItemStatUtils.getPlainCharmLore(nbt));
		for (String plainLore : plainLoreLines) {
			if (plainLore.isEmpty()) {
				continue;
			}
			CharmParsedInfo info = readCharmLine(plainLore);
			if (info == null) {
				MMLog.warning("Unparseable charm lore line '" + plainLore + "' in charm '" + ItemUtils.getPlainName(itemStack) + "'!");
				continue;
			}
			effects.add(info);
		}
		return effects;
	}

	public static @Nullable CharmParsedInfo readCharmLine(String line) {
		Matcher matcher = CHARM_LINE_PATTERN.matcher(line);
		if (!matcher.matches()) {
			return null;
		}
		boolean locked = matcher.group(1) != null;
		double value = Double.parseDouble(matcher.group(2));
		boolean percent = matcher.group(3) != null;
		String effect = matcher.group(4);
		return new CharmParsedInfo(value, percent, locked, effect);
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
		if (!ServerProperties.getAbilityEnhancementsEnabled(p)) {
			return 0;
		}

		Map<String, Double> allEffects = mPlayerCharmEffectMap.get(p.getUniqueId());
		if (allEffects != null && allEffects.get(charmAttribute) != null) {
			return allEffects.get(charmAttribute);
		}
		return 0;
	}

	/**
	 * Summarize a player's charm attributes into a map, given the type of charm to summarize.
	 *
	 * @param p         The player
	 * @param charmType The type of the charm.
	 * @return A TreeMap sorted on effect name, representing a summary of all the <code>charmType</code> charm attributes of this player
	 * @see CharmManager#getSummaryOfAllAttributesAsComponents(Player, CharmType)
	 */
	@Contract(value = "_, _ -> !null", pure = true)
	public @NonNull Map<String, Double> getSummaryOfAllAttributes(Player p, CharmType charmType) {
		if (getCharms(p, charmType).isEmpty()) {
			return Collections.emptyMap();
		}
		return ImmutableMap.copyOf(getCharms(p, charmType)
			.stream()
			.map(this::readCharm)
			.flatMap(Collection::stream)
			.reduce(
				new TreeMap<>(),
				(map, charm) -> {
					final var newName = charm.mEffect + (charm.mIsPercent ? " %" : "") + (charm.mIsLocked ? " (LOCKED)" : ""); // we add a percent sign here (see method getSummaryOfAllAttributesAsComponents)
					map.put(newName, map.getOrDefault(newName, 0.0) + charm.mValue);
					return map;
				},
				(resultMap, acc) -> {
					acc.forEach((key, value) ->
						resultMap.merge(key, value, Double::sum)
					);
					return resultMap;
				}
			));
	}

	private static final DecimalFormat valueFormatter = new DecimalFormat("#.###"); // i hate floats

	/**
	 * Summarizes the charms of a given player that match the given CharmType.
	 * Used for rendering charm info into GUI components.
	 *
	 * @param p         The player
	 * @param charmType The type of charms we want to summarize.
	 * @return The summary of all the charm attributes of the given type as components.
	 * @see CharmsGUI#setup()
	 */
	public List<Component> getSummaryOfAllAttributesAsComponents(Player p, CharmType charmType) {
		List<Component> output = new ArrayList<>();
		if (mEnabledCharmType != charmType) {
			output.add(Component.text("These Charms are currently disabled!", NamedTextColor.RED)); // let the player know the charms are disabled
		}

		Map<String, Double> summary = getSummaryOfAllAttributes(p, charmType);

		//Sort the effects in the order in the manager list
		Set<String> orderedEffects = summary.keySet();

		for (String s : orderedEffects) {
			final String normalized = getPlainEffectName(s);
			double baseValue = summary.getOrDefault(s, 0.0);
			double value = getValueOrCap(baseValue, normalized, charmType); // we need to replace % here otherwise things don't work correctly (see bug 19814)
			if (value != 0) {

				// Strip .0s and calm down floating point lengths by restricting to 3 decimal places.
				String desc = s + " : " + valueFormatter.format(value);

				TextColor charmColor = getCharmEffectColor(value > 0, normalized);

				if (s.contains("%")) {
					desc += "%";
				}

				// If depths effect is maxed, display as such
				if (charmType == CharmType.ZENITH) {
					CharmEffects effect = CharmEffects.getEffect(normalized);
					if (effect != null) {
						if (value == effect.mEffectCap) {
							charmColor = TextColor.fromHexString("#e49b20");

							if (baseValue != value) {
								// always display positive overflow value, even if the diff is negative
								// such as in -cooldown charms
								desc += " (MAX; " + valueFormatter.format(Math.abs(baseValue - value));

								if (s.contains("%")) {
									desc += "%";
								}

								desc += " overflow)";
							} else {
								// not sure if it's possible for this case to even happen - adding fallback regardless.
								desc += " (MAX)";
							}
						}
					}
				}

				output.add(Component.text(desc, charmColor).decoration(TextDecoration.ITALIC, false));
			}
		}
		return output;
	}

	/**
	 * Summarize the names of the charms a player has equipped of a given type, including the total charm power used.
	 *
	 * @param p         The player
	 * @param charmType The type of the charms.
	 * @return A summary of charm names of a given type on a given player.
	 */
	public Component getSummaryOfCharmNames(Player p, CharmType charmType) {
		List<ItemStack> charms = getCharms(p, charmType);
		if (!charms.isEmpty()) {
			Component summary = Component.text("");
			int powerBudget = 0;
			for (ItemStack item : charms) {
				summary = summary.append(Component.text(item.displayName() + "\n"));
				powerBudget += ItemStatUtils.getCharmPower(item);
			}
			summary = summary.append(Component.text("Charm Power: " + powerBudget, NamedTextColor.YELLOW));
			return summary;
		}
		return Component.text("no charms", NamedTextColor.GRAY);
	}

	/**
	 * Returns the total amount of charm power the player is using by their charms of type <code>charmType</code>
	 *
	 * @param p         The player
	 * @param charmType The type of the charms.
	 * @return The total used charm power of all the charms the player is using of type <code>charmType</code>
	 */
	public int getUsedCharmPower(Player p, CharmType charmType) {
		return getCharms(p, charmType).stream().map(ItemStatUtils::getCharmPower).reduce(0, Integer::sum);
	}

	//Handlers for player lifecycle events

	//Discard charm data a few ticks after player leaves shard
	//(give time for save event to register)
	public void onQuit(Player p) {
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			if (!p.isOnline()) {
				UUID uuid = p.getUniqueId();
				for (CharmType charmType : CharmType.values()) {
					charmType.mPlayerCharms.remove(uuid);
				}
				mPlayerCharmEffectMap.remove(uuid);
			}
		}, 100);
	}

	//Store local charm data into plugin data
	public void onSave(PlayerSaveEvent event) {
		for (CharmType charmType : CharmType.values()) {
			Player player = event.getPlayer();
			List<ItemStack> charms = charmType.mPlayerCharms.get(player.getUniqueId());
			if (charms != null) {
				JsonObject data = new JsonObject();
				JsonArray charmArray = new JsonArray();
				data.add(KEY_CHARMS, charmArray);
				for (ItemStack charm : charms) {
					JsonObject charmData = new JsonObject();
					charmData.addProperty(KEY_ITEM, NBT.itemStackToNBT(charm).toString());
					charmArray.add(charmData);
				}
				event.setPluginData(charmType.getPluginDataKey(), data);
			}
		}
	}

	//Load plugin data into local charm data
	public void onJoin(Player p) {
		for (CharmType charmType : CharmType.values()) {
			JsonObject charmPluginData = MonumentaRedisSyncAPI.getPlayerPluginData(p.getUniqueId(), charmType.getPluginDataKey());
			if (charmPluginData != null) {
				if (charmPluginData.has(KEY_CHARMS)) {
					JsonArray charmArray = charmPluginData.getAsJsonArray(KEY_CHARMS);
					List<ItemStack> playerCharms = new ArrayList<>();
					for (JsonElement charmElement : charmArray) {
						JsonObject data = charmElement.getAsJsonObject();
						if (data.has(KEY_ITEM) && data.get(KEY_ITEM).isJsonPrimitive() && data.getAsJsonPrimitive(KEY_ITEM).isString()) {
							ItemStack item = NBT.itemStackFromNBT(NBT.parseNBT(data.getAsJsonPrimitive(KEY_ITEM).getAsString()));
							if (item != null) {

								ItemStatUtils.cleanIfNecessary(item);

								playerCharms.add(item);
							}
						}
					}
					//Check if we actually loaded any charms
					if (!playerCharms.isEmpty()) {
						charmType.mPlayerCharms.put(p.getUniqueId(), playerCharms);
						//Recalculate the charm map based on loaded charms by calling update
						updateCharms(p, charmType);
					}
				}
			}
		}
	}

	//Methods called by the abilities

	/**
	 * Get the scaled radius of a skill
	 *
	 * @param player          The player with the charm effect
	 * @param charmEffectName Name of the charm effect
	 * @param baseRadius      Initial radius of the skill
	 * @return The scaled radius or 0.1, whichever is greater
	 */
	public static double getRadius(Player player, String charmEffectName, double baseRadius) {
		return Math.max(0.1, calculateFlatAndPercentValue(player, charmEffectName, baseRadius));
	}

	// This is still used in two places which do not handle the conversion to getDuration well, so they have been left for now
	@Deprecated
	public static int getExtraDuration(Player player, String charmEffectName) {
		double level = getInstance().getValueOfAttribute(player, charmEffectName);
		return (int) (level * 20);
	}

	public static int getDuration(Player player, String charmEffectName, int baseDuration) {
		double flatLevel = getInstance().getValueOfAttribute(player, charmEffectName);
		double percentLevel = getInstance().getValueOfAttribute(player, charmEffectName + "%");

		return (int) Math.round((baseDuration + flatLevel * 20) * ((percentLevel / 100.0) + 1));
	}

	public static int getCooldown(Player player, String charmEffectName, int baseCooldown) {
		double level = getInstance().getValueOfAttribute(player, charmEffectName + "%");
		return (int) (baseCooldown * ((level / 100.0) + 1));
	}

	public static double getLevel(Player player, String charmEffectName) {
		return getInstance().getValueOfAttribute(player, charmEffectName);
	}

	public static double getLevelPercentDecimal(Player player, String charmEffectName) {
		return getInstance().getValueOfAttribute(player, charmEffectName + "%") / 100.0;
	}

	public static double getExtraPercent(Player player, String charmEffectName, double base) {
		double percentage = getInstance().getValueOfAttribute(player, charmEffectName + "%");
		return base * (1 + (percentage / 100.0));
	}

	//Calculates the final amount using both flat and percent modifiers, applying flat before percent
	public static double calculateFlatAndPercentValue(Player player, String charmEffectName, double baseValue) {
		double flatLevel = getInstance().getValueOfAttribute(player, charmEffectName);
		double percentLevel = getInstance().getValueOfAttribute(player, charmEffectName + "%");
		return (baseValue + flatLevel) * ((percentLevel / 100.0) + 1);
	}

	/**
	 * Returns the given value, or the cap of the charm's effect if the value is over it
	 *
	 * @param value           The value
	 * @param charmEffectName The effect name
	 * @return Returns the passed value, or returns the max value for the stat.
	 */
	public double getValueOrCap(double value, String charmEffectName, CharmType charmType) {
		if (charmType == CharmType.ZENITH) {
			CharmEffects effect = CharmEffects.getEffect(charmEffectName);
			if (effect != null && shouldCap(value, effect)) {
				return effect.mEffectCap;
			}
		}
		return value;
	}

	private boolean shouldCap(double value, CharmEffects effect) {
		return (effect.mEffectCap > 0 && effect.mEffectCap <= value) || (effect.mEffectCap < 0 && effect.mEffectCap >= value);
	}

	public static TextColor getCharmEffectColor(boolean isPositive, String charmEffectName) {
		return getCharmEffectColor(isPositive, INSTANCE.mFlippedColorEffectSubstrings.contains(charmEffectName));
	}

	public static TextColor getCharmEffectColor(boolean isPositive, boolean invertColor) {
		return TextColor.fromHexString(isPositive != invertColor ? "#4AC2E5" : "#D02E28");
	}

	public static String getPlainEffectName(String effect) {
		return effect.replace("%", "").replace("# ", "").replace("(LOCKED)", "").trim();
	}

	public static class CharmParsedInfo {
		public double mValue;
		public boolean mIsPercent;
		public boolean mIsLocked;
		public String mEffect;

		public CharmParsedInfo(double value, boolean isPercent, boolean locked, String effect) {
			mValue = value;
			mIsPercent = isPercent;
			mIsLocked = locked;
			mEffect = effect;
		}

		@Override
		public String toString() {
			return "CharmParsedInfo{" +
				"mValue=" + mValue +
				", mIsPercent=" + mIsPercent +
				", mIsLocked=" + mIsLocked +
				", mEffect='" + mEffect + '\'' +
				'}';
		}
	}
}
