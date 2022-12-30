package com.playmonumenta.plugins.abilities;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
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
import com.playmonumenta.plugins.abilities.cleric.NonClericProvisionsPassive;
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
import com.playmonumenta.plugins.abilities.other.AttribaAttackDamage;
import com.playmonumenta.plugins.abilities.other.AttribaAttackSpeed;
import com.playmonumenta.plugins.abilities.other.AttribaKnockbackResistance;
import com.playmonumenta.plugins.abilities.other.AttribaMaxHealth;
import com.playmonumenta.plugins.abilities.other.AttribaMovementSpeed;
import com.playmonumenta.plugins.abilities.other.CluckingPotions;
import com.playmonumenta.plugins.abilities.other.PatronGreen;
import com.playmonumenta.plugins.abilities.other.PatronPurple;
import com.playmonumenta.plugins.abilities.other.PatronRed;
import com.playmonumenta.plugins.abilities.other.PatronWhite;
import com.playmonumenta.plugins.abilities.other.PvP;
import com.playmonumenta.plugins.abilities.rogue.AdvancingShadows;
import com.playmonumenta.plugins.abilities.rogue.ByMyBlade;
import com.playmonumenta.plugins.abilities.rogue.DaggerThrow;
import com.playmonumenta.plugins.abilities.rogue.Dethroner;
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
import com.playmonumenta.plugins.abilities.scout.Versatile;
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
import com.playmonumenta.plugins.abilities.warrior.Formidable;
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
import com.playmonumenta.plugins.commands.experiencinator.ExperiencinatorUtils;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.effects.AbilitySilence;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.Stasis;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.PotionEffectApplyEvent;
import com.playmonumenta.plugins.gallery.GalleryManager;
import com.playmonumenta.plugins.integrations.MonumentaNetworkChatIntegration;
import com.playmonumenta.plugins.itemstats.infusions.Vitality;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.Nullable;

public class AbilityManager {

	private static final String LEFT_CLICK_TICK_METAKEY = "IgnoreLeftClicksUntil";
	private static final String RIGHT_CLICK_TICK_METAKEY = "IgnoreRightClicksUntil";
	private static final float DEFAULT_WALK_SPEED = 0.2f;

	private static @MonotonicNonNull AbilityManager mManager = null;

	private final Plugin mPlugin;
	private final List<AbilityInfo<?>> mReferenceAbilities;
	private final List<AbilityInfo<?>> mDisabledAbilities;
	private final Map<UUID, AbilityCollection> mAbilities = new HashMap<>();

	//Only used for MultipleChargeAbilities
	private final Map<UUID, HashMap<ClassAbility, Integer>> mChargeTracker = new HashMap<>();
	private static final String KEY_CHARGES_PLUGIN_DATA = "AbilityCharges";

	private final Map<UUID, Map<String, AbilityTrigger>> mCustomTriggers = new HashMap<>();
	private static final String KEY_TRIGGERS_PLUGIN_DATA = "AbilityTriggers";

	//Public manager methods
	//---------------------------------------------------------------------------------------------------------------

	public AbilityManager(Plugin plugin) {
		mPlugin = plugin;
		mManager = this;

		mReferenceAbilities = new ArrayList<>();
		mDisabledAbilities = new ArrayList<>();

		mReferenceAbilities.addAll(Arrays.asList(
			// ALL (CLUCKING POTIONS)
			CluckingPotions.INFO,

			// ALL PLAYERS (but technically for Cleric)
			NonClericProvisionsPassive.INFO,

			// ALL Attriba
			AttribaAttackDamage.INFO,
			AttribaAttackSpeed.INFO,
			AttribaKnockbackResistance.INFO,
			AttribaMaxHealth.INFO,
			AttribaMovementSpeed.INFO,

			// All other non-class abilities
			PvP.INFO,
			PatronWhite.INFO,
			PatronGreen.INFO,
			PatronPurple.INFO,
			PatronRed.INFO
		));

		List<AbilityInfo<?>> classAbilities = Arrays.asList(

			//********** MAGE **********//
			ArcaneStrike.INFO,
			ThunderStep.INFO,
			ElementalArrows.INFO,
			FrostNova.INFO,
			Channeling.INFO,
			MagmaShield.INFO,
			ManaLance.INFO,
			Spellshock.INFO,
			PrismaticShield.INFO,

			//********** ROGUE **********//
			AdvancingShadows.INFO,
			ByMyBlade.INFO,
			DaggerThrow.INFO,
			Dodging.INFO,
			Dethroner.INFO,
			Smokescreen.INFO,
			ViciousCombos.INFO,
			Skirmisher.INFO,
			EscapeDeath.INFO,

			//********** SCOUT **********//
			Agility.INFO,
			HuntingCompanion.INFO,
			Volley.INFO,
			Swiftness.INFO,
			EagleEye.INFO,
			Versatile.INFO,
			SwiftCuts.INFO,
			Sharpshooter.INFO,
			WindBomb.INFO,

			//********** WARRIOR **********//
			CounterStrike.INFO,
			DefensiveLine.INFO,
			Frenzy.INFO,
			Riposte.INFO,
			ShieldBash.INFO,
			Toughness.INFO,
			Formidable.INFO,
			WeaponMastery.INFO,
			BruteForce.INFO,

			//********** CLERIC **********//
			CelestialBlessing.INFO,
			CleansingRain.INFO,
			HandOfLight.INFO,
			Rejuvenation.INFO,
			DivineJustice.INFO,
			HeavenlyBoon.INFO,
			Crusade.INFO,
			SanctifiedArmor.INFO,
			SacredProvisions.INFO,

			//********** WARLOCK **********//
			AmplifyingHex.INFO,
			PhlegmaticResolve.INFO,
			CholericFlames.INFO,
			CursedWound.INFO,
			GraspingClaws.INFO,
			Culling.INFO,
			SanguineHarvest.INFO,
			SoulRend.INFO,
			MelancholicLament.INFO,

			//********** ALCHEMIST **********//
			Bezoar.INFO,
			AlchemicalArtillery.INFO,
			EmpoweringOdor.INFO,
			UnstableAmalgam.INFO,
			IronTincture.INFO,
			GruesomeAlchemy.INFO,
			BrutalAlchemy.INFO,
			EnergizingElixir.INFO,
			AlchemistPotions.INFO
		);

		List<AbilityInfo<?>> specAbilities = Arrays.asList(
			//********** MAGE **********//
			// ELEMENTALIST
			ElementalSpiritFire.INFO,
			ElementalSpiritIce.INFO,
			Blizzard.INFO,
			Starfall.INFO,

			// ARCANIST
			CosmicMoonblade.INFO,
			AstralOmen.INFO,
			SagesInsight.INFO,

			//********** ROGUE **********//
			// SWORDSAGE
			WindWalk.INFO,
			BladeDance.INFO,
			DeadlyRonde.INFO,

			// ASSASSIN
			BodkinBlitz.INFO,
			CloakAndDagger.INFO,
			CoupDeGrace.INFO,

			//********** SCOUT **********//
			// RANGER
			TacticalManeuver.INFO,
			WhirlingBlade.INFO,
			Quickdraw.INFO,

			// HUNTER
			PinningShot.INFO,
			SplitArrow.INFO,
			PredatorStrike.INFO,

			//********** WARRIOR **********//
			// BERSERKER
			MeteorSlam.INFO,
			Rampage.INFO,
			GloriousBattle.INFO,

			// GUARDIAN
			ShieldWall.INFO,
			Challenge.INFO,
			Bodyguard.INFO,

			//********** CLERIC **********//
			// PALADIN
			// LI needs to run first to process its passive melee damage
			LuminousInfusion.INFO,
			// HJ runs afterwards and can use that value in the same event,
			// sharing it to its Javelin AoE
			HolyJavelin.INFO,

			ChoirBells.INFO,

			// HIEROPHANT
			EnchantedPrayer.INFO,
			HallowedBeam.INFO,
			ThuribleProcession.INFO,

			//********** WARLOCK **********//
			// REAPER
			JudgementChain.INFO,
			VoodooBonds.INFO,
			DarkPact.INFO,

			// TENEBRIST
			WitheringGaze.INFO,
			HauntingShades.INFO,
			RestlessSouls.INFO,

			//********** ALCHEMIST **********//
			// HARBINGER
			ScorchedEarth.INFO,
			EsotericEnhancements.INFO,
			Taboo.INFO,

			// APOTHECARY
			Panacea.INFO,
			TransmutationRing.INFO,
			WardingRemedy.INFO
		);

		if (ServerProperties.getDepthsEnabled()) {
			//Depths abilities
			mReferenceAbilities.addAll(DepthsManager.getAbilities());
		}
		if (!ServerProperties.getShardName().contains("depths")) {
			//Normal class and spec abilities
			mReferenceAbilities.addAll(classAbilities);

			if (ServerProperties.getClassSpecializationsEnabled()) {
				mReferenceAbilities.addAll(specAbilities);
			} else {
				mDisabledAbilities.addAll(specAbilities);
			}
		}

		mReferenceAbilities.sort(Comparator.comparingDouble(AbilityInfo::getPriorityAmount));
	}

	public static AbilityManager getManager() {
		return mManager;
	}

	public AbilityCollection updatePlayerAbilities(Player player, boolean resetAbsorption) {
		// Clear self-given potions
		mPlugin.mPotionManager.clearPotionIDType(player, PotionID.ABILITY_SELF);

		AttributeInstance knockbackResistance = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
		AttributeInstance movementSpeed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
		AttributeInstance attackDamage = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
		AttributeInstance attackSpeed = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
		AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);

		// Reset passive buffs to player base attributes
		if (knockbackResistance != null) {
			knockbackResistance.setBaseValue(0);
		}
		if (attackDamage != null) {
			attackDamage.setBaseValue(1.0);
		}
		if (attackSpeed != null) {
			attackSpeed.setBaseValue(4.0);
		}
		if (maxHealth != null) {
			maxHealth.setBaseValue(20);
		}
		// This zooms the player's screen obnoxiously, so try not to do it if it's not needed
		if (movementSpeed != null
			&& movementSpeed.getValue() != 0.1
			&& !player.getGameMode().equals(GameMode.CREATIVE)
			&& !player.getGameMode().equals(GameMode.SPECTATOR)) {
			movementSpeed.setBaseValue(0.1);
		}

		/*
		 * Supposing that all instances of attribute modifiers are from abilities (which is the
		 * case currently), this code is fine. If we use attribute modifiers for other things
		 * in the future, then we'll need some way to differentiate them.
		 *
		 * This accounts for skipping over modifiers from items, but will remove all other
		 * attribute modifiers, so hopefully those aren't used anywhere else in Vanilla...
		 *
		 * Haha turns out potions use modifiers too. So double hopefully these aren't used
		 * anywhere else in Vanilla.
		 *
		 * As a side note, we should only really ever be adding modifiers to KBR, Speed, and
		 * Health, but check all of them here just in case.
		 *
		 *
		 *
		 * TODO: This should be replaced with calls to remove() in each respective ability.
		 */
		@Nullable AttributeInstance[] instances = {
			knockbackResistance,
			attackDamage,
			attackSpeed,
			movementSpeed,
			maxHealth
		};

		for (AttributeInstance instance : instances) {
			if (instance != null) {
				for (AttributeModifier mod : instance.getModifiers()) {
					String name = mod.getName();
					// The name of modifiers from vanilla attributes or potions or the vitality infusion
					if (!name.equals("Modifier")
						&& !name.startsWith("minecraft:generic.")
						&& !name.startsWith("effect.minecraft.")
						&& !name.startsWith("Armor ")
						&& !name.startsWith("Weapon ")
						&& !name.equals(Vitality.MODIFIER)) {
						instance.removeModifier(mod);
					}
				}
			}
		}

		player.setWalkSpeed(DEFAULT_WALK_SPEED);
		player.setInvulnerable(false);

		// The absorption tracker may lose track of the player when doing things like shard transfers, so reset absorption
		if (resetAbsorption) {
			AbsorptionUtils.setAbsorption(player, 0, -1);
		}


		/* Get the old ability list and run invalidate() on all of them to clean up lingering runnables */
		if (mAbilities.containsKey(player.getUniqueId())) {
			for (Ability abil : getPlayerAbilities(player).getAbilitiesIgnoringSilence()) {
				abil.invalidate();
			}
		}

		// Removes things like attributes/tags applied by abilities
		for (AbilityInfo<?> ability : mReferenceAbilities) {
			ability.onRemove(player);
		}

		List<Ability> abilities = new ArrayList<>();

		if (player.getScoreboardTags().contains("disable_class") || player.getGameMode().equals(GameMode.SPECTATOR)) {
			/* This player's abilities are disabled - give them an empty set and stop here */
			AbilityCollection collection = new AbilityCollection(abilities);
			mAbilities.put(player.getUniqueId(), collection);
			return collection;
		}

		for (AbilityInfo<?> ability : mReferenceAbilities) {
			if (ability.testCanUse(player)) {
				Ability newAbility = ability.newInstance(mPlugin, player);
				abilities.add(newAbility);
			}
		}

		AbilityCollection collection = new AbilityCollection(abilities);
		mAbilities.put(player.getUniqueId(), collection);

		// Set up new class potion abilities
		// Needs to run at the end of the tick, or it can inconsistently not apply the effects
		Bukkit.getScheduler().runTask(mPlugin, () -> {
			for (Ability abil : getPlayerAbilities(player).getAbilitiesIgnoringSilence()) {
				abil.setupClassPotionEffects();
			}
		});

		GalleryManager.refreshEffects(player);
		MonumentaNetworkChatIntegration.refreshPlayer(player);
		ClientModHandler.updateAbilities(player);

		updateSilence(player, collection, false);

		return collection;
	}

	public @Nullable <T extends Ability> T getPlayerAbility(@Nullable Player player, Class<T> cls) {
		if (player == null) {
			return null;
		}
		return getPlayerAbilities(player).getAbility(cls);
	}

	public @Nullable <T extends Ability> T getPlayerAbilityIgnoringSilence(@Nullable Player player, Class<T> cls) {
		if (player == null) {
			return null;
		}
		return getPlayerAbilities(player).getAbilityIgnoringSilence(cls);
	}

	/* Do not modify the returned data! */
	public List<AbilityInfo<?>> getReferenceAbilities() {
		return mReferenceAbilities;
	}

	// This is for things that care about currently disabled abilities. (ex. Specs in R1)
	/* Do not modify the returned data! */
	public List<AbilityInfo<?>> getDisabledAbilities() {
		return mDisabledAbilities;
	}

	/* Convenience method */
	public boolean isPvPEnabled(Player player) {
		return getPlayerAbilities(player).getAbilityIgnoringSilence(PvP.class) != null;
	}

	public JsonElement getAsJson(Player player) {
		return getPlayerAbilities(player).getAsJson();
	}

	//Events
	//---------------------------------------------------------------------------------------------------------------

	public boolean abilityCastEvent(Player player, AbilityCastEvent event) {
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			if (!abil.abilityCastEvent(event)) {
				return false;
			}
		}
		return true;
	}

	public boolean blockBreakEvent(Player player, BlockBreakEvent event) {
		return conditionalCastCancellable(player, (ability) -> ability.blockBreakEvent(event));
	}

	public void onDamage(Player player, DamageEvent event, LivingEntity enemy) {
		if (EntityUtils.isHostileMob(enemy)) {
			for (Ability abil : getPlayerAbilities(player).getAbilities()) {
				if (event.isCancelled()) {
					break;
				}
				String metaKey = "LastDamageTick_" + abil.getClass().getCanonicalName();
				if (!MetadataUtils.happenedThisTick(player, metaKey)) {
					if (abil.onDamage(event, enemy)) {
						MetadataUtils.checkOnceThisTick(mPlugin, player, metaKey);
					}
				}
			}
		}

		// after the damage event, as HolyJavelin casts itself on melee attacks with added functionality
		if (event.getType() == DamageEvent.DamageType.MELEE) {
			checkTrigger(player, AbilityTrigger.Key.LEFT_CLICK);
		}
	}

	@FunctionalInterface
	private interface CastArgumentNoReturn {
		void run(Ability ability);
	}

	@FunctionalInterface
	private interface CastArgumentWithReturn {
		boolean run(Ability ability);
	}

	private void conditionalCast(Player player, CastArgumentNoReturn func) {
		if (player.getGameMode() == GameMode.SPECTATOR) {
			return;
		}
		for (Ability ability : getPlayerAbilities(player).getAbilities()) {
			func.run(ability);
		}
	}

	private boolean conditionalCastCancellable(Player player, CastArgumentWithReturn func) {
		if (player.getGameMode() == GameMode.SPECTATOR) {
			return true;
		}
		for (Ability ability : getPlayerAbilities(player).getAbilities()) {
			if (!func.run(ability)) {
				return false;
			}
		}
		return true;
	}

	public void onHurt(Player player, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			if (event.isCancelled()) {
				return;
			}
				abil.onHurt(event, damager, source);
		}
	}

	public void onHurtFatal(Player player, DamageEvent event) {
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			if (event.isCancelled() || event.getFinalDamage(true) < player.getHealth()) {
				return;
			}
			abil.onHurtFatal(event);
		}
	}

	public boolean playerCombustByEntityEvent(Player player, EntityCombustByEntityEvent event) {
		return conditionalCastCancellable(player, (ability) -> ability.playerCombustByEntityEvent(event));
	}

	public boolean playerShotProjectileEvent(Player player, Projectile projectile) {
		return conditionalCastCancellable(player, (ability) -> ability.playerShotProjectileEvent(projectile));
	}

	public boolean playerThrewSplashPotionEvent(Player player, ThrownPotion potion) {
		return conditionalCastCancellable(player, (ability) -> ability.playerThrewSplashPotionEvent(potion));
	}

	public boolean playerThrewLingeringPotionEvent(Player player, ThrownPotion potion) {
		return conditionalCastCancellable(player, (ability) -> ability.playerThrewLingeringPotionEvent(potion));
	}

	public boolean playerSplashPotionEvent(Player player, Collection<LivingEntity> affectedEntities,
										   ThrownPotion potion, PotionSplashEvent event) {
		return conditionalCastCancellable(player, (ability) -> ability.playerSplashPotionEvent(affectedEntities, potion, event));
	}

	public boolean playerSplashedByPotionEvent(Player player, Collection<LivingEntity> affectedEntities,
											   ThrownPotion potion, PotionSplashEvent event) {
		return conditionalCastCancellable(player, (ability) -> ability.playerSplashedByPotionEvent(affectedEntities, potion, event));
	}

	public void playerItemConsumeEvent(Player player, PlayerItemConsumeEvent event) {
		conditionalCast(player, (ability) -> ability.playerItemConsumeEvent(event));
	}

	public void playerItemDamageEvent(Player player, PlayerItemDamageEvent event) {
		conditionalCast(player, (ability) -> ability.playerItemDamageEvent(event));
	}

	public void entityDeathEvent(Player player, EntityDeathEvent event, boolean shouldGenDrops) {
		if (!event.getEntity().getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)) {
			conditionalCast(player, (ability) -> ability.entityDeathEvent(event, shouldGenDrops));
		}
	}

	public void entityDeathRadiusEvent(Player player, EntityDeathEvent event, boolean shouldGenDrops) {
		LivingEntity entity = event.getEntity();
		if (!entity.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)) {
			conditionalCast(player, (ability) -> {
				Location center = ability.entityDeathRadiusCenterLocation();
				if (center != null && entity.getWorld() == center.getWorld() && entity.getLocation().distance(center) <= ability.entityDeathRadius()) {
					ability.entityDeathRadiusEvent(event, shouldGenDrops);
				}
			});
		}
	}

	public void projectileHitEvent(Player player, ProjectileHitEvent event, Projectile proj) {
		conditionalCast(player, (ability) -> ability.projectileHitEvent(event, proj));
	}

	public void playerExtendedSneakEvent(Player player) {
		conditionalCast(player, Ability::playerExtendedSneakEvent);
	}

	public void playerHitByProjectileEvent(Player player, ProjectileHitEvent event) {
		conditionalCast(player, (ability) -> ability.playerHitByProjectileEvent(event));
	}

	public void periodicTrigger(Player player, boolean twoHertz, boolean oneSecond, int ticks) {
		conditionalCast(player, (ability) -> ability.periodicTrigger(twoHertz, oneSecond, ticks));
	}

	public void entityTargetLivingEntityEvent(Player player, EntityTargetLivingEntityEvent event) {
		conditionalCast(player, (ability) -> ability.entityTargetLivingEntityEvent(event));
	}

	public void potionEffectApplyEvent(Player player, PotionEffectApplyEvent event) {
		conditionalCast(player, (ability) -> ability.potionApplyEvent(event));
	}

	public void playerDeathEvent(Player player, PlayerDeathEvent event) {
		conditionalCast(player, (ability) -> ability.playerDeathEvent(event));
	}

	public void playerAnimationEvent(Player player, PlayerAnimationEvent event) {
		checkTrigger(player, AbilityTrigger.Key.LEFT_CLICK);

		conditionalCast(player, (ability) -> ability.playerAnimationEvent(event));
	}

	public void playerSwapHandItemsEvent(Player player, PlayerSwapHandItemsEvent event) {
		if (checkTrigger(player, AbilityTrigger.Key.SWAP)) {
			event.setCancelled(true);
		}
	}

	public void playerRegainHealthEvent(Player player, EntityRegainHealthEvent event) {
		conditionalCast(player, (ability) -> ability.playerRegainHealthEvent(event));
	}

	public void playerTeleportEvent(Player player, PlayerTeleportEvent event) {
		conditionalCast(player, (ability) -> ability.playerTeleportEvent(event));
	}

	public void playerQuitEvent(Player player, PlayerQuitEvent event) {
		conditionalCast(player, (ability) -> ability.playerQuitEvent(event));
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			if (!player.isOnline()) {
				UUID uuid = player.getUniqueId();
				mAbilities.remove(uuid);
				mChargeTracker.remove(uuid);
				mCustomTriggers.remove(uuid);
			}
		}, 5);
	}

	public void playerSaveEvent(Player player, PlayerSaveEvent event) {
		HashMap<ClassAbility, Integer> charges = mChargeTracker.get(player.getUniqueId());
		if (charges != null) {
			JsonObject data = new JsonObject();
			for (Map.Entry<ClassAbility, Integer> entry : charges.entrySet()) {
				data.addProperty(entry.getKey().getName(), entry.getValue());
			}
			event.setPluginData(KEY_CHARGES_PLUGIN_DATA, data);
		}

		Map<String, AbilityTrigger> customTriggers = mCustomTriggers.get(player.getUniqueId());
		if (customTriggers != null) {
			JsonObject data = new JsonObject();
			for (Map.Entry<String, AbilityTrigger> entry : customTriggers.entrySet()) {
				data.add(entry.getKey(), entry.getValue().toJson());
			}
			event.setPluginData(KEY_TRIGGERS_PLUGIN_DATA, data);
		}

	}

	public void playerJoinEvent(Player player, PlayerJoinEvent event) {
		// Anticheat for skills
		AbilityUtils.updateAbilityScores(player);
		UUID uuid = player.getUniqueId();
		JsonObject chargesData = MonumentaRedisSyncAPI.getPlayerPluginData(uuid, KEY_CHARGES_PLUGIN_DATA);
		if (chargesData != null) {
			HashMap<ClassAbility, Integer> chargeMap = new HashMap<>();
			for (Map.Entry<String, JsonElement> entry : chargesData.entrySet()) {
				ClassAbility ability = ClassAbility.getAbility(entry.getKey());
				if (ability != null) {
					if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber()) {
						chargeMap.put(ability, entry.getValue().getAsJsonPrimitive().getAsInt());
					} else {
						mPlugin.getLogger().warning("Got player " + player.getName() + " with ability charge for " + ability.getName() + " with unknown value '" + entry.getValue() + "'");
					}
				} else {
					mPlugin.getLogger().warning("Got player " + player.getName() + " with unknown ability charges: " + entry.getKey());
				}
			}
			if (!chargeMap.isEmpty()) {
				mChargeTracker.put(uuid, chargeMap);
			}
		}

		JsonObject triggerData = MonumentaRedisSyncAPI.getPlayerPluginData(uuid, KEY_TRIGGERS_PLUGIN_DATA);
		if (triggerData != null) {
			Map<String, AbilityTrigger> customTriggers = new HashMap<>();
			for (Map.Entry<String, JsonElement> entry : triggerData.entrySet()) {
				AbilityTrigger trigger = AbilityTrigger.fromJson(entry.getValue().getAsJsonObject());
				if (trigger != null) {
					customTriggers.put(entry.getKey(), trigger);
				}
			}
			mCustomTriggers.put(uuid, customTriggers);
		}
	}

	public void playerInteractEntityEvent(PlayerInteractEntityEvent event) {
		// This causes a left click to trigger if clicking a villager or item frame for example
		MetadataUtils.setMetadata(event.getPlayer(), LEFT_CLICK_TICK_METAKEY, Bukkit.getServer().getCurrentTick() + 1);
	}

	public void playerInteractEvent(PlayerInteractEvent event, Material blockClicked) {
		Player player = event.getPlayer();
		Action action = event.getAction();
		// Right-clicking sometimes counts as two clicks, so make sure this can only be triggered once per tick
		// Right clicks also sometimes creates an additional left click up to 2 ticks later, thus check within the past 2 ticks.
		if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
			checkTrigger(player, AbilityTrigger.Key.LEFT_CLICK);
		} else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
			if (action == Action.RIGHT_CLICK_BLOCK && (ItemUtils.interactableBlocks.contains(blockClicked) || blockClicked == Material.AIR)) {
				MetadataUtils.setMetadata(player, LEFT_CLICK_TICK_METAKEY, Bukkit.getServer().getCurrentTick() + 1);
				return;
			}

			checkTrigger(player, AbilityTrigger.Key.RIGHT_CLICK);

			// When blocking with an offhand shield, the client first sends a mainhand click, then an offhand click,
			// thus we have to ignore the usual click limiter here.
			if (player.getInventory().getItem(event.getHand()).getType() == Material.SHIELD
				    && player.getCooldown(Material.SHIELD) == 0
				    && MetadataUtils.checkOnceThisTick(mPlugin, player, "BlockTrigger")) {
				conditionalCast(player, Ability::blockWithShieldEvent);
			}
		}
	}

	private final ConcurrentSkipListSet<UUID> mDropKeyDisabledLeftClicks = new ConcurrentSkipListSet<>();

	// Called asynchronously by the drop key packet listener, thus cannot use most of the Bukkit API
	public void preDropKey(Player player) {
		mDropKeyDisabledLeftClicks.add(player.getUniqueId());
	}

	public boolean checkTrigger(Player player, AbilityTrigger.Key key) {
		// click rate limiter: the client sends multiple click events per physical button press, especially for right clicks that send a right or a left click up to 2 ticks later
		// thus, limit clicks to once per tick, and additionally make right clicks ignore left or right clicks for 1/2 ticks, depending on the held item's type
		if (key == AbilityTrigger.Key.LEFT_CLICK || key == AbilityTrigger.Key.RIGHT_CLICK) {
			if (key == AbilityTrigger.Key.LEFT_CLICK && mDropKeyDisabledLeftClicks.contains(player.getUniqueId())) {
				return false;
			}
			int currentTick = Bukkit.getServer().getCurrentTick();
			int noClicksUntil = MetadataUtils.getMetadata(player, key == AbilityTrigger.Key.LEFT_CLICK ? LEFT_CLICK_TICK_METAKEY : RIGHT_CLICK_TICK_METAKEY, currentTick - 100);
			if (noClicksUntil >= currentTick) {
				return false;
			}
			if (key == AbilityTrigger.Key.LEFT_CLICK) {
				MetadataUtils.setMetadata(player, LEFT_CLICK_TICK_METAKEY, currentTick);
			} else {
				// If right-clicking with a "useable" item, the client will send a left click soon after, so need to ignore that click when it happens
				// Need to also check vanity, as the item type on the client is what matters
				List<@org.jetbrains.annotations.Nullable ItemStack> potentiallyUseableItems = Arrays.asList(player.getInventory().getItemInMainHand(),
					player.getInventory().getItemInOffHand(),
					mPlugin.mVanityManager.getData(player).getEquipped(EquipmentSlot.OFF_HAND));
				if (potentiallyUseableItems.stream()
					    .anyMatch(item -> item != null && (ItemUtils.isSomePotion(item) || ItemUtils.isProjectileWeapon(item) || item.getType().isBlock()
						                                       || item.getType() == Material.ENDER_PEARL || item.getType() == Material.ENDER_EYE))) {
					MetadataUtils.setMetadata(player, RIGHT_CLICK_TICK_METAKEY, currentTick);
					MetadataUtils.setMetadata(player, LEFT_CLICK_TICK_METAKEY, currentTick + 2);
				} else {
					MetadataUtils.setMetadata(player, RIGHT_CLICK_TICK_METAKEY, currentTick + 1);
				}
			}
		} else if (key == AbilityTrigger.Key.DROP) {
			// clear the custom left click prevention and add the usual metadata key instead to prevent left clicks for the rest of the tick
			mDropKeyDisabledLeftClicks.remove(player.getUniqueId());
			MetadataUtils.setMetadata(player, LEFT_CLICK_TICK_METAKEY, Bukkit.getServer().getCurrentTick());
		}

		// Disable sneak+left click abilities when using an Experiencinator/Crystallizer
		// TODO would be nice if all SQ interactibles would prevent skill activation - but how to do that in general?
		// Cancelling events cannot be used, as that is used to prevent vanilla actions from occurring. Some custom way to annotate an event as handled by SQ would be needed.
		if (key == AbilityTrigger.Key.LEFT_CLICK
			    && player.isSneaking()
			    && ExperiencinatorUtils.getConfig(player.getLocation(), false).getExperiencinator(player.getInventory().getItemInMainHand()) != null) {
			return false;
		}

		AbilityCollection playerAbilities = getPlayerAbilities(player);
		if (playerAbilities.isSilenced()) {
			// if silenced, just return if any trigger matches (to cancel the swap event properly)
			return playerAbilities.getAbilitiesIgnoringSilence().stream()
				       .flatMap(a -> a.mCustomTriggers.stream())
				       .anyMatch(t -> t.check(player, key));
		}
		for (Ability ability : playerAbilities.getAbilitiesInTriggerOrder()) {
			for (AbilityTriggerInfo<?> triggerInfo : ability.mCustomTriggers) {
				if (triggerInfo.check(player, key)) {
					((Consumer<Ability>) triggerInfo.getAction()).accept(ability);
					if (!(ability instanceof EagleEye)) { // hardcoded exception for eagle eye to keep triggering other abilities
						return true;
					}
				}
			}
		}
		return false;
	}

	//---------------------------------------------------------------------------------------------------------------

	//public methods
	public AbilityCollection getPlayerAbilities(Player player) {
		AbilityCollection collection = mAbilities.get(player.getUniqueId());
		if (collection == null) {
			return updatePlayerAbilities(player, true);
		}
		return collection;
	}

	public void resetPlayerAbilities(Player player) {
		ScoreboardUtils.setScoreboardValue(player, AbilityUtils.SCOREBOARD_CLASS_NAME, 0);
		ScoreboardUtils.setScoreboardValue(player, AbilityUtils.SCOREBOARD_SPEC_NAME, 0);
		AbilityUtils.updateAbilityScores(player);

		// Run updatePlayerAbilities to clear existing ability effects.
		updatePlayerAbilities(player, true);
	}

	public void updateSilence(Player player, boolean forceUpdateClientMod) {
		updateSilence(player, getPlayerAbilities(player), forceUpdateClientMod);
	}

	public void updateSilence(Player player, AbilityCollection playerAbilities, boolean forceUpdateClientMod) {
		NavigableSet<AbilitySilence> silence = mPlugin.mEffectManager.getEffects(player, AbilitySilence.class);
		NavigableSet<Stasis> stasis = mPlugin.mEffectManager.getEffects(player, Stasis.class);
		int silenceDuration = Stream.concat(silence.stream(), stasis.stream())
			                      .mapToInt(Effect::getDuration).max().orElse(0);
		boolean silenced = silenceDuration > 0;
		if (playerAbilities.isSilenced() != silenced || forceUpdateClientMod) {
			ClientModHandler.silenced(player, silenceDuration);
		}
		playerAbilities.setSilenced(silenced);
	}

	public void trackCharges(Player player, ClassAbility ability, int charges) {
		mChargeTracker.computeIfAbsent(player.getUniqueId(), key -> new HashMap<>())
			.put(ability, charges);
	}

	public int getTrackedCharges(Player player, ClassAbility ability) {
		HashMap<ClassAbility, Integer> playerCharges = mChargeTracker.get(player.getUniqueId());
		if (playerCharges != null) {
			return playerCharges.getOrDefault(ability, 0);
		}
		return 0;
	}

	public int getNumberOfCustomTriggers(Player player) {
		Map<String, AbilityTrigger> triggerMap = mCustomTriggers.get(player.getUniqueId());
		return triggerMap == null ? 0 : triggerMap.size();
	}

	public @Nullable AbilityTrigger getCustomTrigger(Player player, AbilityInfo<?> ability, String triggerId) {
		ClassAbility classAbility = ability.getLinkedSpell();
		if (classAbility == null) {
			return null;
		}
		Map<String, AbilityTrigger> triggerMap = mCustomTriggers.get(player.getUniqueId());
		if (triggerMap == null) {
			return null;
		}
		return triggerMap.get(classAbility.name() + "_" + triggerId);
	}

	public void setCustomTrigger(Player player, AbilityInfo<?> ability, String triggerId, @Nullable AbilityTrigger trigger) {
		ClassAbility classAbility = ability.getLinkedSpell();
		if (classAbility == null) {
			return;
		}
		Map<String, AbilityTrigger> triggerMap = mCustomTriggers.computeIfAbsent(player.getUniqueId(), key -> new HashMap<>());
		String key = classAbility.name() + "_" + triggerId;
		if (trigger == null) {
			triggerMap.remove(key);
		} else {
			triggerMap.put(key, trigger);
		}
	}

	public void clearCustomTriggers(Player player) {
		// Need to explicitly set it to an empty map so that the change is saved
		mCustomTriggers.computeIfAbsent(player.getUniqueId(), key -> new HashMap<>()).clear();
	}

	//---------------------------------------------------------------------------------------------------------------
}
