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
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.abilities.cleric.DivineJustice;
import com.playmonumenta.plugins.abilities.cleric.HandOfLight;
import com.playmonumenta.plugins.abilities.cleric.HeavenlyBoon;
import com.playmonumenta.plugins.abilities.cleric.Illuminate;
import com.playmonumenta.plugins.abilities.cleric.Rejuvenation;
import com.playmonumenta.plugins.abilities.cleric.SanctifiedArmor;
import com.playmonumenta.plugins.abilities.cleric.hierophant.EnchantedPrayer;
import com.playmonumenta.plugins.abilities.cleric.hierophant.HallowedBeam;
import com.playmonumenta.plugins.abilities.cleric.hierophant.ThuribleProcession;
import com.playmonumenta.plugins.abilities.cleric.paladin.ChoirBells;
import com.playmonumenta.plugins.abilities.cleric.paladin.HolyJavelin;
import com.playmonumenta.plugins.abilities.cleric.paladin.LuminousInfusion;
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
import com.playmonumenta.plugins.abilities.scout.Agility;
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
import com.playmonumenta.plugins.abilities.shaman.ChainLightning;
import com.playmonumenta.plugins.abilities.shaman.CleansingTotem;
import com.playmonumenta.plugins.abilities.shaman.CrystallineCombos;
import com.playmonumenta.plugins.abilities.shaman.EarthenTremor;
import com.playmonumenta.plugins.abilities.shaman.FlameTotem;
import com.playmonumenta.plugins.abilities.shaman.InterconnectedHavoc;
import com.playmonumenta.plugins.abilities.shaman.LightningTotem;
import com.playmonumenta.plugins.abilities.shaman.TotemicEmpowerment;
import com.playmonumenta.plugins.abilities.shaman.TotemicProjection;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.DecayedTotem;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.DesecratingShot;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.Devastation;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.ChainHealingWave;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.Sanctuary;
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
import de.tr7zw.nbtapi.NBTContainer;
import de.tr7zw.nbtapi.NBTItem;
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

	@SuppressWarnings("NullAway.Init") // fields are initialised by called methods
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
			PrismaticShield.CHARM_RADIUS,
			PrismaticShield.CHARM_ENHANCE_DAMAGE,
			PrismaticShield.CHARM_ENHANCE_DURATION,
			PrismaticShield.CHARM_ENHANCE_HEALING,
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
			MagmaShield.CHARM_ABILITY_BONUS,
			ArcaneStrike.CHARM_DAMAGE,
			ArcaneStrike.CHARM_RADIUS,
			ArcaneStrike.CHARM_BONUS,
			ArcaneStrike.CHARM_COOLDOWN,
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
			Spellshock.CHARM_ENHANCE_KNOCKBACK,
			Spellshock.CHARM_ENHANCE_DAMAGE,
			Spellshock.CHARM_ENHANCE_SLOW,
			Spellshock.CHARM_ENHANCE_WEAK,
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
			DivineJustice.CHARM_ENHANCE_DURATION,
			HeavenlyBoon.CHARM_CHANCE,
			HeavenlyBoon.CHARM_DURATION,
			HeavenlyBoon.CHARM_RADIUS,
			HeavenlyBoon.CHARM_HEAL_AMPLIFIER,
			HeavenlyBoon.CHARM_REGEN_AMPLIFIER,
			HeavenlyBoon.CHARM_SPEED_AMPLIFIER,
			HeavenlyBoon.CHARM_STRENGTH_AMPLIFIER,
			HeavenlyBoon.CHARM_RESIST_AMPLIFIER,
			HeavenlyBoon.CHARM_ABSORPTION_AMPLIFIER,
			HeavenlyBoon.CHARM_ENHANCE_COOLDOWN,
			HeavenlyBoon.CHARM_ENHANCE_CDR,
			HeavenlyBoon.CHARM_ENHANCE_CDR_CAP,
			Crusade.CHARM_DAMAGE,
			CleansingRain.CHARM_COOLDOWN,
			CleansingRain.CHARM_RANGE,
			CleansingRain.CHARM_REDUCTION,
			CleansingRain.CHARM_DURATION,
			HandOfLight.CHARM_COOLDOWN,
			HandOfLight.CHARM_DAMAGE,
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
			HolyJavelin.CHARM_COOLDOWN,
			HolyJavelin.CHARM_DAMAGE,
			HolyJavelin.CHARM_RANGE,
			HolyJavelin.CHARM_SIZE,
			ChoirBells.CHARM_COOLDOWN,
			ChoirBells.CHARM_RANGE,
			ChoirBells.CHARM_SLOW,
			ChoirBells.CHARM_DAMAGE,
			ChoirBells.CHARM_VULN,
			ChoirBells.CHARM_WEAKEN,
			ChoirBells.CHARM_DURATION,
			LuminousInfusion.CHARM_COOLDOWN,
			LuminousInfusion.CHARM_RADIUS,
			LuminousInfusion.CHARM_DAMAGE,
			LuminousInfusion.CHARM_HITS,
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
			HallowedBeam.CHARM_RESISTANCE,
			HallowedBeam.CHARM_RESISTANCE_DURATION,
			HallowedBeam.CHARM_HEALING_PERCENT_THRESHOLD,
			Rejuvenation.CHARM_THRESHOLD,
			SanctifiedArmor.CHARM_DAMAGE,
			SanctifiedArmor.CHARM_SLOW,
			SanctifiedArmor.CHARM_DURATION,

			//Rogue
			AdvancingShadows.CHARM_COOLDOWN,
			AdvancingShadows.CHARM_DAMAGE,
			AdvancingShadows.CHARM_KNOCKBACK,
			AdvancingShadows.CHARM_RANGE,
			AdvancingShadows.CHARM_ENHANCE_TIMER,
			ByMyBlade.CHARM_COOLDOWN,
			ByMyBlade.CHARM_DAMAGE,
			ByMyBlade.CHARM_HASTE_DURATION,
			ByMyBlade.CHARM_HASTE_AMPLIFIER,
			ByMyBlade.CHARM_HEALTH,
			ByMyBlade.CHARM_ELITE_HEALTH,
			DaggerThrow.CHARM_COOLDOWN,
			DaggerThrow.CHARM_DAMAGE,
			DaggerThrow.CHARM_RANGE,
			DaggerThrow.CHARM_VULN,
			DaggerThrow.CHARM_DAGGERS,
			Dodging.CHARM_COOLDOWN,
			Dodging.CHARM_SPEED,
			EscapeDeath.CHARM_ABSORPTION,
			EscapeDeath.CHARM_COOLDOWN,
			EscapeDeath.CHARM_SPEED,
			EscapeDeath.CHARM_JUMP,
			EscapeDeath.CHARM_STUN_DURATION,
			Skirmisher.CHARM_DAMAGE,
			Skirmisher.CHARM_RADIUS,
			Skirmisher.CHARM_TARGETS,
			Skirmisher.CHARM_ENHANCEMENT_DAMAGE,
			Smokescreen.CHARM_COOLDOWN,
			Smokescreen.CHARM_RANGE,
			Smokescreen.CHARM_WEAKEN,
			Smokescreen.CHARM_SLOW,
			Smokescreen.CHARM_DURATION,
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
			DeadlyRonde.CHARM_SPEED,
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
			CloakAndDagger.CHARM_STACKS_GAIN,
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
			BruteForce.CHARM_WAVE_DELAY,
			CounterStrike.CHARM_DAMAGE,
			CounterStrike.CHARM_RADIUS,
			CounterStrike.CHARM_DURATION,
			CounterStrike.CHARM_DAMAGE_REDUCTION,
			CounterStrike.CHARM_KBR,
			CounterStrike.CHARM_BLEED,
			CounterStrike.CHARM_BLEED_DURATION,
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
			ShieldBash.CHARM_PARRY_DURATION,
			ShieldBash.CHARM_CDR,
			Toughness.CHARM_HEALTH,
			Toughness.CHARM_REDUCTION,
			Toughness.CHARM_HEALING,
			WeaponMastery.CHARM_REDUCTION,
			WeaponMastery.CHARM_SPEED,
			WeaponMastery.CHARM_WEAKEN,
			WeaponMastery.CHARM_DURATION,
			GloriousBattle.CHARM_DAMAGE,
			GloriousBattle.CHARM_CHARGES,
			GloriousBattle.CHARM_VELOCITY,
			GloriousBattle.CHARM_RADIUS,
			GloriousBattle.CHARM_KNOCKBACK,
			GloriousBattle.CHARM_DAMAGE_MODIFIER,
			GloriousBattle.CHARM_BONUS_DAMAGE,
			GloriousBattle.CHARM_MOB_CAP,
			MeteorSlam.CHARM_COOLDOWN,
			MeteorSlam.CHARM_DAMAGE,
			MeteorSlam.CHARM_RADIUS,
			MeteorSlam.CHARM_JUMP_BOOST,
			MeteorSlam.CHARM_DURATION,
			MeteorSlam.CHARM_THRESHOLD,
			MeteorSlam.CHARM_SCALING,
			MeteorSlam.CHARM_REDUCED,
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
			AlchemicalArtillery.CHARM_COOLDOWN,
			AlchemicalArtillery.CHARM_AFTERSHOCK_DAMAGE,
			AlchemicalArtillery.CHARM_AFTERSHOCK_DELAY,
			AlchemicalArtillery.CHARM_DAMAGE,
			AlchemicalArtillery.CHARM_RADIUS,
			AlchemicalArtillery.CHARM_VELOCITY,
			AlchemicalArtillery.CHARM_SIZE,
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
			IronTincture.CHARM_REFILL,
			IronTincture.CHARM_ALLY_REFILL,
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
			Panacea.CHARM_DOT_DAMAGE,
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
			WardingRemedy.CHARM_DELAY,
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
			ScorchedEarth.CHARM_FIRE_DURATION,
			Taboo.CHARM_COOLDOWN,
			Taboo.CHARM_DAMAGE,
			Taboo.CHARM_BURST_DURATION,
			Taboo.CHARM_HEALING_PENALTY,
			Taboo.CHARM_ABSORPTION_PENALTY,
			Taboo.CHARM_KNOCKBACK_RESISTANCE,
			Taboo.CHARM_SELF_DAMAGE,
			Taboo.CHARM_RECHARGE,

			//Warlock
			AmplifyingHex.CHARM_CONE,
			AmplifyingHex.CHARM_COOLDOWN,
			AmplifyingHex.CHARM_DAMAGE,
			AmplifyingHex.CHARM_RANGE,
			AmplifyingHex.CHARM_POTENCY,
			AmplifyingHex.CHARM_POTENCY_CAP,
			AmplifyingHex.CHARM_ENHANCE_HEALTH,
			AmplifyingHex.CHARM_ENHANCE_DAMAGE,
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
			GraspingClaws.CHARM_CLEAVE_DAMAGE,
			GraspingClaws.CHARM_CLEAVE_RADIUS,
			GraspingClaws.CHARM_CAGE_RADIUS,
			GraspingClaws.CHARM_CAGE_HEALING,
			GraspingClaws.CHARM_CAGE_DURATION,
			MelancholicLament.CHARM_COOLDOWN,
			MelancholicLament.CHARM_ENHANCE_DAMAGE,
			MelancholicLament.CHARM_ENHANCE_DURATION,
			MelancholicLament.CHARM_RADIUS,
			MelancholicLament.CHARM_RECOVERY,
			MelancholicLament.CHARM_WEAKNESS,
			MelancholicLament.CHARM_SILENCE_RADIUS,
			MelancholicLament.CHARM_SILENCE_DURATION,
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
			SanguineHarvest.CHARM_BLEED,
			SanguineHarvest.CHARM_DAMAGE_BOOST,
			SoulRend.CHARM_COOLDOWN,
			SoulRend.CHARM_HEAL,
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
			Agility.CHARM_HASTE,
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
			Sharpshooter.CHARM_DISTANCE,
			SwiftCuts.CHARM_DAMAGE,
			SwiftCuts.CHARM_STACKS,
			SwiftCuts.CHARM_DURATION,
			SwiftCuts.CHARM_ENHANCE,
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
			PredatorStrike.CHARM_RANGE,
			PredatorStrike.CHARM_KNOCKBACK,
			PredatorStrike.CHARM_PIERCING,
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
			WhirlingBlade.CHARM_KNOCKBACK,
			WhirlingBlade.CHARM_WEAKEN,
			WhirlingBlade.CHARM_SLOWNESS,
			WhirlingBlade.CHARM_WEAKEN_DURATION,
			WhirlingBlade.CHARM_SLOWNESS_DURATION,
			WhirlingBlade.CHARM_STUN_DURATION,

			//Shaman
			TotemicEmpowerment.CHARM_SPEED,
			TotemicEmpowerment.CHARM_RESISTANCE,
			TotemicEmpowerment.CHARM_RADIUS,
			CleansingTotem.CHARM_DURATION,
			CleansingTotem.CHARM_RADIUS,
			CleansingTotem.CHARM_COOLDOWN,
			CleansingTotem.CHARM_HEALING,
			CleansingTotem.CHARM_CLEANSES,
			CleansingTotem.CHARM_ENHANCE_ABSORB_MAX,
			CleansingTotem.CHARM_PULSE_DELAY,
			ChainLightning.CHARM_COOLDOWN,
			ChainLightning.CHARM_DAMAGE,
			ChainLightning.CHARM_RADIUS,
			ChainLightning.CHARM_TARGETS,
			ChainLightning.CHARM_CHARGES,
			ChainLightning.CHARM_KNOCKBACK,
			CrystallineCombos.CHARM_CRYSTAL_DAMAGE,
			CrystallineCombos.CHARM_CRYSTAL_RANGE,
			CrystallineCombos.CHARM_CRYSTAL_STACK_THRESHOLD,
			CrystallineCombos.CHARM_CRYSTAL_TOTEM_COUNT_PERCENTAGE,
			CrystallineCombos.CHARM_SPEED_PER_STACK,
			CrystallineCombos.CHARM_MAX_SPEED,
			CrystallineCombos.CHARM_SPEED_DURATION,
			CrystallineCombos.CHARM_STACK_DECAY_TIME,
			CrystallineCombos.CHARM_SHOT_COUNT,
			CrystallineCombos.CHARM_SHOT_DELAY,
			EarthenTremor.CHARM_DAMAGE,
			EarthenTremor.CHARM_COOLDOWN,
			EarthenTremor.CHARM_RADIUS,
			EarthenTremor.CHARM_KNOCKBACK,
			EarthenTremor.CHARM_SILENCE_DURATION,
			EarthenTremor.CHARM_SHOCKWAVES,
			EarthenTremor.CHARM_SHOCKWAVE_DISTANCE,
			EarthenTremor.CHARM_SHOCKWAVE_RADIUS,
			FlameTotem.CHARM_DURATION,
			FlameTotem.CHARM_RADIUS,
			FlameTotem.CHARM_COOLDOWN,
			FlameTotem.CHARM_DAMAGE,
			FlameTotem.CHARM_FIRE_DURATION,
			FlameTotem.CHARM_BOMB_RADIUS,
			FlameTotem.CHARM_ENHANCE_INFERNO_SCALE,
			FlameTotem.CHARM_PULSE_DELAY,
			InterconnectedHavoc.CHARM_DAMAGE,
			InterconnectedHavoc.CHARM_RANGE,
			InterconnectedHavoc.CHARM_ENHANCEMENT_KNOCKBACK,
			InterconnectedHavoc.CHARM_ENHANCEMENT_STUN,
			LightningTotem.CHARM_DURATION,
			LightningTotem.CHARM_RADIUS,
			LightningTotem.CHARM_COOLDOWN,
			LightningTotem.CHARM_DAMAGE,
			LightningTotem.CHARM_STORM_DAMAGE,
			LightningTotem.CHARM_STORM_RADIUS,
			LightningTotem.CHARM_PULSE_DELAY,
			TotemicProjection.CHARM_COOLDOWN,
			TotemicProjection.CHARM_DISTANCE,
			TotemicProjection.CHARM_SLOWNESS_PERCENT,
			TotemicProjection.CHARM_SLOWNESS_DURATION,
			TotemicProjection.CHARM_DAMAGE_RADIUS,
			TotemicProjection.CHARM_ENHANCE_DAMAGE_DURATION,
			TotemicProjection.CHARM_ENHANCE_DAMAGE_PERCENT_PER,
			DecayedTotem.CHARM_DURATION,
			DecayedTotem.CHARM_RADIUS,
			DecayedTotem.CHARM_COOLDOWN,
			DecayedTotem.CHARM_DAMAGE,
			DecayedTotem.CHARM_SLOWNESS,
			DecayedTotem.CHARM_TARGETS,
			DecayedTotem.CHARM_FLAME_TOTEM_DAMAGE_BUFF,
			DecayedTotem.CHARM_LIGHTNING_TOTEM_DAMAGE_BUFF,
			DecayedTotem.CHARM_PULSE_DELAY,
			DesecratingShot.CHARM_COOLDOWN,
			DesecratingShot.CHARM_DAMAGE,
			DesecratingShot.CHARM_WEAKNESS,
			DesecratingShot.CHARM_DURATION,
			DesecratingShot.CHARM_RADIUS,
			Devastation.CHARM_DAMAGE,
			Devastation.CHARM_RADIUS,
			Devastation.CHARM_COOLDOWN,
			Devastation.CHARM_CDR,
			ChainHealingWave.CHARM_COOLDOWN,
			ChainHealingWave.CHARM_HEALING,
			ChainHealingWave.CHARM_CDR,
			ChainHealingWave.CHARM_TARGETS,
			ChainHealingWave.CHARM_RADIUS,
			ChainHealingWave.CHARM_CHARGES,
			Sanctuary.CHARM_WEAKNESS_PERCENT,
			Sanctuary.CHARM_SLOWNESS_PERCENT,
			Sanctuary.CHARM_VULNERABILITY_PERCENT,
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
			Starfall.CHARM_COOLDOWN,
			ElementalSpiritFire.CHARM_COOLDOWN,
			CelestialBlessing.CHARM_COOLDOWN,
			CleansingRain.CHARM_COOLDOWN,
			HandOfLight.CHARM_COOLDOWN,
			HeavenlyBoon.CHARM_ENHANCE_COOLDOWN,
			HolyJavelin.CHARM_COOLDOWN,
			ChoirBells.CHARM_COOLDOWN,
			LuminousInfusion.CHARM_COOLDOWN,
			Illuminate.CHARM_COOLDOWN,
			EnchantedPrayer.CHARM_COOLDOWN,
			ThuribleProcession.CHARM_COOLDOWN,
			HallowedBeam.CHARM_COOLDOWN,
			AdvancingShadows.CHARM_COOLDOWN,
			ByMyBlade.CHARM_COOLDOWN,
			DaggerThrow.CHARM_COOLDOWN,
			Dodging.CHARM_COOLDOWN,
			EscapeDeath.CHARM_COOLDOWN,
			Smokescreen.CHARM_COOLDOWN,
			BladeDance.CHARM_COOLDOWN,
			WindWalk.CHARM_COOLDOWN,
			BodkinBlitz.CHARM_COOLDOWN,
			BruteForce.CHARM_WAVE_DELAY,
			DefensiveLine.CHARM_COOLDOWN,
			Riposte.CHARM_COOLDOWN,
			ShieldBash.CHARM_COOLDOWN,
			MeteorSlam.CHARM_COOLDOWN,
			MeteorSlam.CHARM_THRESHOLD,
			Rampage.CHARM_THRESHOLD,
			Bodyguard.CHARM_COOLDOWN,
			Challenge.CHARM_COOLDOWN,
			ShieldWall.CHARM_COOLDOWN,
			Bezoar.CHARM_REQUIREMENT,
			IronTincture.CHARM_COOLDOWN,
			EnergizingElixir.CHARM_PRICE,
			UnstableAmalgam.CHARM_COOLDOWN,
			Panacea.CHARM_COOLDOWN,
			TransmutationRing.CHARM_COOLDOWN,
			WardingRemedy.CHARM_COOLDOWN,
			WardingRemedy.CHARM_DELAY,
			EsotericEnhancements.CHARM_COOLDOWN,
			EsotericEnhancements.CHARM_FUSE,
			ScorchedEarth.CHARM_COOLDOWN,
			Taboo.CHARM_COOLDOWN,
			Taboo.CHARM_SELF_DAMAGE,
			Taboo.CHARM_HEALING_PENALTY,
			Taboo.CHARM_ABSORPTION_PENALTY,
			AlchemicalArtillery.CHARM_COOLDOWN,
			AlchemicalArtillery.CHARM_AFTERSHOCK_DELAY,
			AmplifyingHex.CHARM_COOLDOWN,
			AmplifyingHex.CHARM_ENHANCE_HEALTH,
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
			EagleEye.CHARM_COOLDOWN,
			HuntingCompanion.CHARM_COOLDOWN,
			Volley.CHARM_COOLDOWN,
			WindBomb.CHARM_COOLDOWN,
			PredatorStrike.CHARM_COOLDOWN,
			Quickdraw.CHARM_COOLDOWN,
			TacticalManeuver.CHARM_COOLDOWN,
			WhirlingBlade.CHARM_COOLDOWN,
			CleansingTotem.CHARM_COOLDOWN,
			CleansingTotem.CHARM_PULSE_DELAY,
			ChainLightning.CHARM_COOLDOWN,
			CrystallineCombos.CHARM_CRYSTAL_STACK_THRESHOLD,
			CrystallineCombos.CHARM_SHOT_DELAY,
			FlameTotem.CHARM_COOLDOWN,
			FlameTotem.CHARM_PULSE_DELAY,
			EarthenTremor.CHARM_COOLDOWN,
			LightningTotem.CHARM_COOLDOWN,
			LightningTotem.CHARM_PULSE_DELAY,
			TotemicProjection.CHARM_COOLDOWN,
			DecayedTotem.CHARM_COOLDOWN,
			DecayedTotem.CHARM_PULSE_DELAY,
			DesecratingShot.CHARM_COOLDOWN,
			Devastation.CHARM_COOLDOWN,
			ChainHealingWave.CHARM_COOLDOWN,
			WhirlwindTotem.CHARM_COOLDOWN,
			WhirlwindTotem.CHARM_PULSE_DELAY,
			ElementalSpiritIce.CHARM_COOLDOWN2,
			ElementalSpiritFire.CHARM_COOLDOWN2
		)).toList();
	}

	/** Returns the charms a player has equipped that are of type <code>charmType</code>
	 *
	 * @param player The player
	 * @param charmType The type of charm to query
	 * @return The charms of the given type that the player currently has equipped.
	 */
	public @NonNull List<ItemStack> getCharms(Player player, CharmType charmType) {
		return charmType.mPlayerCharms.computeIfAbsent(player.getUniqueId(), key -> new ArrayList<>());
	}

	/** Add (equip) a charm onto a player.
	 * Returns false if passed a null player or if the charm is not valid.
	 *
	 * @param p The player
	 * @param charm The charm
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
				//Check naming of each charm
				for (ItemStack c : charms) {
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

		for (ItemStack charm : equippedCharms) {
			if (charm == null || charm.getType() == Material.AIR) {
				continue;
			}
			for (CharmParsedInfo info : readCharm(charm)) {
				if (!mCharmEffectList.contains(info.mEffect)) {
					MMLog.warning("Unknown effect '" + info.mEffect + "' in charm '" + ItemUtils.getPlainName(charm) + "'!");
					continue;
				}
				//Combine all effects
				allEffects.merge(info.mEffect + (info.mIsPercent ? "%" : ""), info.mValue, (a, b) -> Math.ceil((a + b) * 1000) / 1000);
			}
		}

		//Then calculate the cap. Just checks the last character for in case an effect has a percent in it
		allEffects.replaceAll((a, b) -> Math.ceil(getValueOrCap(b, a.charAt(a.length() - 1) == '%' ? a.substring(0, a.length() - 1) : a, charmType) * 1000) / 1000);

		//Store to local map
		mPlayerCharmEffectMap.put(uuid, allEffects);

		//Refresh class of player
		AbilityManager.getManager().updatePlayerAbilities(p, true);
	}

	private static final Pattern CHARM_LINE_PATTERN = Pattern.compile("([-+]?\\d+(?:\\.\\d+)?)(%)? (.+)");

	//Helper method to parse item for charm effects
	private List<CharmParsedInfo> readCharm(ItemStack itemStack) {
		List<CharmParsedInfo> effects = new ArrayList<>();
		List<String> plainLoreLines = ItemStatUtils.getPlainCharmLore(new NBTItem(itemStack));
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
		double value = Double.parseDouble(matcher.group(1));
		boolean percent = matcher.group(2) != null;
		String effect = matcher.group(3);
		return new CharmParsedInfo(value, percent, effect);
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

	/** Summarize a player's charm attributes into a map, given the type of charm to summarize.
	 *
	 * @param p The player
	 * @param charmType The type of the charm.
	 * @return A TreeMap sorted on effect name, representing a summary of all the <code>charmType</code> charm attributes of this player
	 * @see CharmManager#getSummaryOfAllAttributesAsComponents(Player, CharmType)
	 * @see com.playmonumenta.plugins.managers.DataCollectionManager.PlayerInformation#PlayerInformation(String, Player)
	 */
	@SuppressWarnings("JavadocReference")
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
					final var newName = charm.mEffect + (charm.mIsPercent ? "%" : ""); // we add a percent sign here (see method getSummaryOfAllAttributesAsComponents)
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

	/** Summarizes the charms of a given player that match the given CharmType.
	 * Used for rendering charm info into GUI components.
	 *
	 * @param p The player
	 * @param charmType The type of charms we want to summarize.
	 * @see CharmsGUI#setup()
	 * @return The summary of all the charm attributes of the given type as components.
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
			final var normalized = s.replace("%", "");
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

	/** Summarize the names of the charms a player has equipped of a given type, including the total charm power used.
	 *
	 * @param p The player
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

	/** Returns the total amount of charm power the player is using by their charms of type <code>charmType</code>
	 *
	 * @param p The player
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
					charmData.addProperty(KEY_ITEM, NBTItem.convertItemtoNBT(charm).toString());
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
							ItemStack item = NBTItem.convertNBTtoItem(new NBTContainer(data.getAsJsonPrimitive(KEY_ITEM).getAsString()));
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

	public static double getRadius(Player player, String charmEffectName, double baseRadius) {
		double level = getInstance().getValueOfAttribute(player, charmEffectName + "%");
		return Math.max(0.1, baseRadius * ((level / 100.0) + 1));
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

		return (int) ((baseDuration + flatLevel * 20) * ((percentLevel / 100.0) + 1));
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

	/** Returns the given value, or the cap of the charm's effect if the value is over it
	 *
	 * @param value The value
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

	public static class CharmParsedInfo {
		public double mValue;
		public boolean mIsPercent;
		public String mEffect;

		public CharmParsedInfo(double value, boolean isPercent, String effect) {
			mValue = value;
			mIsPercent = isPercent;
			mEffect = effect;
		}

		@Override
		public String toString() {
			return "CharmParsedInfo{" +
				"mValue=" + mValue +
				", mIsPercent=" + mIsPercent +
				", mEffect='" + mEffect + '\'' +
				'}';
		}
	}
}
