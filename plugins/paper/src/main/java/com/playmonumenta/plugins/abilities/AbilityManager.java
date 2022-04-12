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
import com.playmonumenta.plugins.abilities.cleric.ClericPassive;
import com.playmonumenta.plugins.abilities.cleric.Crusade;
import com.playmonumenta.plugins.abilities.cleric.DivineJustice;
import com.playmonumenta.plugins.abilities.cleric.HandOfLight;
import com.playmonumenta.plugins.abilities.cleric.HeavenlyBoon;
import com.playmonumenta.plugins.abilities.cleric.NonClericProvisionsPassive;
import com.playmonumenta.plugins.abilities.cleric.SacredProvisions;
import com.playmonumenta.plugins.abilities.cleric.SanctifiedArmor;
import com.playmonumenta.plugins.abilities.cleric.hierophant.EnchantedPrayer;
import com.playmonumenta.plugins.abilities.cleric.hierophant.HallowedBeam;
import com.playmonumenta.plugins.abilities.cleric.hierophant.ThuribleProcession;
import com.playmonumenta.plugins.abilities.cleric.paladin.ChoirBells;
import com.playmonumenta.plugins.abilities.cleric.paladin.HolyJavelin;
import com.playmonumenta.plugins.abilities.cleric.paladin.LuminousInfusion;
import com.playmonumenta.plugins.abilities.delves.Arcanic;
import com.playmonumenta.plugins.abilities.delves.Bloodthirsty;
import com.playmonumenta.plugins.abilities.delves.Carapace;
import com.playmonumenta.plugins.abilities.delves.Chivalrous;
import com.playmonumenta.plugins.abilities.delves.Colossal;
import com.playmonumenta.plugins.abilities.delves.Dreadful;
import com.playmonumenta.plugins.abilities.delves.Entropy;
import com.playmonumenta.plugins.abilities.delves.Infernal;
import com.playmonumenta.plugins.abilities.delves.Legionary;
import com.playmonumenta.plugins.abilities.delves.Pernicious;
import com.playmonumenta.plugins.abilities.delves.Relentless;
import com.playmonumenta.plugins.abilities.delves.Spectral;
import com.playmonumenta.plugins.abilities.delves.StatMultiplier;
import com.playmonumenta.plugins.abilities.delves.Transcendent;
import com.playmonumenta.plugins.abilities.delves.Twisted;
import com.playmonumenta.plugins.abilities.mage.ArcaneStrike;
import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.abilities.mage.FrostNova;
import com.playmonumenta.plugins.abilities.mage.MagePassive;
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
import com.playmonumenta.plugins.abilities.rogue.Dodging;
import com.playmonumenta.plugins.abilities.rogue.EscapeDeath;
import com.playmonumenta.plugins.abilities.rogue.RoguePassive;
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
import com.playmonumenta.plugins.abilities.scout.ScoutPassive;
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
import com.playmonumenta.plugins.abilities.warlock.WarlockPassive;
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
import com.playmonumenta.plugins.abilities.warrior.WarriorPassive;
import com.playmonumenta.plugins.abilities.warrior.WeaponMastery;
import com.playmonumenta.plugins.abilities.warrior.berserker.MeteorSlam;
import com.playmonumenta.plugins.abilities.warrior.berserker.Rampage;
import com.playmonumenta.plugins.abilities.warrior.berserker.RecklessSwing;
import com.playmonumenta.plugins.abilities.warrior.guardian.Bodyguard;
import com.playmonumenta.plugins.abilities.warrior.guardian.Challenge;
import com.playmonumenta.plugins.abilities.warrior.guardian.ShieldWall;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.PotionEffectApplyEvent;
import com.playmonumenta.plugins.itemstats.infusions.Vitality;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.DelvesUtils;
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
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.AbstractArrow;
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
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class AbilityManager {

	private static final String CLICK_TICK_METAKEY = "ClickedThisTickMetakey";
	private static final float DEFAULT_WALK_SPEED = 0.2f;

	private static @Nullable AbilityManager mManager = null;

	private final Plugin mPlugin;
	private final List<Ability> mReferenceAbilities;
	private final List<Ability> mDisabledAbilities;
	private final Map<UUID, AbilityCollection> mAbilities = new HashMap<>();

	//Only used for MultipleChargeAbilities
	private final Map<UUID, HashMap<ClassAbility, Integer>> mChargeTracker = new HashMap<>();
	private static final String KEY_PLUGIN_DATA = "AbilityCharges";

	//Public manager methods
	//---------------------------------------------------------------------------------------------------------------

	public AbilityManager(Plugin plugin) {
		mPlugin = plugin;
		mManager = this;

		mReferenceAbilities = new ArrayList<>();
		mDisabledAbilities = new ArrayList<>();

		mReferenceAbilities.addAll(Arrays.asList(
			// ALL (CLUCKING POTIONS)
			new CluckingPotions(mPlugin, null),

			// ALL PLAYERS (but technically for Cleric)
			new NonClericProvisionsPassive(mPlugin, null),

			// ALL Attriba
			new AttribaAttackDamage(mPlugin, null),
			new AttribaAttackSpeed(mPlugin, null),
			new AttribaKnockbackResistance(mPlugin, null),
			new AttribaMaxHealth(mPlugin, null),
			new AttribaMovementSpeed(mPlugin, null),

			// All other non-class abilities
			new PvP(mPlugin, null),
			new PatronWhite(mPlugin, null),
			new PatronGreen(mPlugin, null),
			new PatronPurple(mPlugin, null),
			new PatronRed(mPlugin, null)
		));

		List<Ability> classAbilities = Arrays.asList(

			//********** MAGE **********//
			new ArcaneStrike(mPlugin, null),
			new ThunderStep(mPlugin, null),
			new ElementalArrows(mPlugin, null),
			new FrostNova(mPlugin, null),
			new MagePassive(mPlugin, null),
			new MagmaShield(mPlugin, null),
			new ManaLance(mPlugin, null),
			new Spellshock(mPlugin, null),
			new PrismaticShield(mPlugin, null),

			//********** ROGUE **********//
			new AdvancingShadows(mPlugin, null),
			new ByMyBlade(mPlugin, null),
			new DaggerThrow(mPlugin, null),
			new Dodging(mPlugin, null),
			new RoguePassive(mPlugin, null),
			new Smokescreen(mPlugin, null),
			new ViciousCombos(mPlugin, null),
			new Skirmisher(mPlugin, null),
			new EscapeDeath(mPlugin, null),

			//********** SCOUT **********//
			new Agility(mPlugin, null),
			new HuntingCompanion(mPlugin, null),
			new Volley(mPlugin, null),
			new Swiftness(mPlugin, null),
			new EagleEye(mPlugin, null),
			new ScoutPassive(mPlugin, null),
			new SwiftCuts(mPlugin, null),
			new Sharpshooter(mPlugin, null),
			new WindBomb(mPlugin, null),

			//********** WARRIOR **********//
			new CounterStrike(mPlugin, null),
			new DefensiveLine(mPlugin, null),
			new Frenzy(mPlugin, null),
			new Riposte(mPlugin, null),
			new ShieldBash(mPlugin, null),
			new Toughness(mPlugin, null),
			new WarriorPassive(mPlugin, null),
			new WeaponMastery(mPlugin, null),
			new BruteForce(mPlugin, null),

			//********** CLERIC **********//
			new CelestialBlessing(mPlugin, null),
			new CleansingRain(mPlugin, null),
			new HandOfLight(mPlugin, null),
			new ClericPassive(mPlugin, null),
			new DivineJustice(mPlugin, null),
			new HeavenlyBoon(mPlugin, null),
			new Crusade(mPlugin, null),
			new SanctifiedArmor(mPlugin, null),
			new SacredProvisions(mPlugin, null),

			//********** WARLOCK **********//
			new AmplifyingHex(mPlugin, null),
			new PhlegmaticResolve(mPlugin, null),
			new CholericFlames(mPlugin, null),
			new CursedWound(mPlugin, null),
			new GraspingClaws(mPlugin, null),
			new WarlockPassive(mPlugin, null),
			new SanguineHarvest(mPlugin, null),
			new SoulRend(mPlugin, null),
			new MelancholicLament(mPlugin, null),

			//********** ALCHEMIST **********//
			new Bezoar(mPlugin, null),
			new AlchemicalArtillery(mPlugin, null),
			new EmpoweringOdor(mPlugin, null),
			new UnstableAmalgam(mPlugin, null),
			new IronTincture(mPlugin, null),
			new GruesomeAlchemy(mPlugin, null),
			new BrutalAlchemy(mPlugin, null),
			new EnergizingElixir(mPlugin, null),
			new AlchemistPotions(mPlugin, null)
		);

		List<Ability> specAbilities = Arrays.asList(
			//********** MAGE **********//
			// ELEMENTALIST
			new ElementalSpiritFire(mPlugin, null),
			new ElementalSpiritIce(mPlugin, null),
			new Blizzard(mPlugin, null),
			new Starfall(mPlugin, null),

			// ARCANIST
			new CosmicMoonblade(mPlugin, null),
			new AstralOmen(mPlugin, null),
			new SagesInsight(mPlugin, null),

			//********** ROGUE **********//
			// SWORDSAGE
			new WindWalk(mPlugin, null),
			new BladeDance(mPlugin, null),
			new DeadlyRonde(mPlugin, null),

			// ASSASSIN
			new BodkinBlitz(mPlugin, null),
			new CloakAndDagger(mPlugin, null),
			new CoupDeGrace(mPlugin, null),

			//********** SCOUT **********//
			// RANGER
			new TacticalManeuver(mPlugin, null),
			new WhirlingBlade(mPlugin, null),
			new Quickdraw(mPlugin, null),

			// HUNTER
			new PinningShot(mPlugin, null),
			new SplitArrow(mPlugin, null),
			new PredatorStrike(mPlugin, null),

			//********** WARRIOR **********//
			// BERSERKER
			new MeteorSlam(mPlugin, null),
			new Rampage(mPlugin, null),
			new RecklessSwing(mPlugin, null),

			// GUARDIAN
			new ShieldWall(mPlugin, null),
			new Challenge(mPlugin, null),
			new Bodyguard(mPlugin, null),

			//********** CLERIC **********//
			// PALADIN
			// LI needs to run first to process its passive melee damage
			new LuminousInfusion(mPlugin, null),
			// HJ runs afterwards and can use that value in the same event,
			// sharing it to its Javelin AoE
			new HolyJavelin(mPlugin, null),

			new ChoirBells(mPlugin, null),

			// HIEROPHANT
			new EnchantedPrayer(mPlugin, null),
			new HallowedBeam(mPlugin, null),
			new ThuribleProcession(mPlugin, null),

			//********** WARLOCK **********//
			// REAPER
			new JudgementChain(mPlugin, null),
			new VoodooBonds(mPlugin, null),
			new DarkPact(mPlugin, null),

			// TENEBRIST
			new WitheringGaze(mPlugin, null),
			new HauntingShades(mPlugin, null),
			new RestlessSouls(mPlugin, null),

			//********** ALCHEMIST **********//
			// HARBINGER
			new ScorchedEarth(mPlugin, null),
			new EsotericEnhancements(mPlugin, null),
			new Taboo(mPlugin, null),

			// APOTHECARY
			new Panacea(mPlugin, null),
			new TransmutationRing(mPlugin, null),
			new WardingRemedy(mPlugin, null)
		);

		if (ServerProperties.getShardName().contains("depths") || ServerProperties.getShardName().contains("dev")) {
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


		List<Ability> delveModifiers = Arrays.asList(
			//********** DELVES **********//
			new StatMultiplier(mPlugin, null),
			new Legionary(mPlugin, null),
			new Pernicious(mPlugin, null),
			new Relentless(mPlugin, null),
			new Arcanic(mPlugin, null),
			new Infernal(mPlugin, null),
			new Transcendent(mPlugin, null),
			new Spectral(mPlugin, null),
			new Dreadful(mPlugin, null),
			new Colossal(mPlugin, null),
			new Chivalrous(mPlugin, null),
			new Bloodthirsty(mPlugin, null),
			new Carapace(mPlugin, null),
			new Entropy(mPlugin, null),
			new Twisted(mPlugin, null)
		);

		mReferenceAbilities.addAll(delveModifiers);
		mReferenceAbilities.sort(Comparator.comparingDouble(Ability::getPriorityAmount));
	}

	public static AbilityManager getManager() {
		return mManager;
	}

	public AbilityCollection updatePlayerAbilities(Player player, boolean resetAbsorption) {
		// Clear self-given potions
		mPlugin.mPotionManager.clearPotionIDType(player, PotionID.ABILITY_SELF);

		AttributeInstance knockbackResistance = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
		AttributeInstance movementSpeed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
		AttributeInstance attackSpeed = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
		AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);

		// Reset passive buffs to player base attributes
		if (knockbackResistance != null) {
			knockbackResistance.setBaseValue(0);
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

		// Reset the DelveInfo mapping so a new one is generated
		DelvesUtils.removeDelveInfo(player);

		/* Get the old ability list and run invalidate() on all of them to clean up lingering runnables */
		if (mAbilities.containsKey(player.getUniqueId())) {
			for (Ability abil : getPlayerAbilities(player).getAbilitiesIgnoringSilence()) {
				abil.invalidate();
			}
		}

		// Removes things like attributes/tags applied by abilities
		for (Ability ability : mReferenceAbilities) {
			ability.remove(player);
		}

		List<Ability> abilities = new ArrayList<>();

		if (player.getScoreboardTags().contains("disable_class") || player.getGameMode().equals(GameMode.SPECTATOR)) {
			/* This player's abilities are disabled - give them an empty set and stop here */
			AbilityCollection collection = new AbilityCollection(abilities);
			mAbilities.put(player.getUniqueId(), collection);
			return collection;
		}

		try {
			for (Ability ab : mReferenceAbilities) {
				if (ab.canUse(player)) {
					Class<?>[] constructorTypes = new Class[2];
					constructorTypes[0] = Plugin.class;
					constructorTypes[1] = Player.class;

					Ability newAbility = ab.getClass().getDeclaredConstructor(constructorTypes).newInstance(mPlugin, player);
					abilities.add(newAbility);
				}
			}
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | java.lang.reflect.InvocationTargetException e) {
			e.printStackTrace();
		}

		AbilityCollection collection = new AbilityCollection(abilities);
		mAbilities.put(player.getUniqueId(), collection);

		// Set up new class potion abilities
		for (Ability abil : getPlayerAbilities(player).getAbilitiesIgnoringSilence()) {
			abil.setupClassPotionEffects();
		}

		ClientModHandler.updateAbilities(player);

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
	public List<Ability> getReferenceAbilities() {
		return mReferenceAbilities;
	}

	// This is for things that care about currently disabled abilities. (ex. Specs in R1)
	/* Do not modify the returned data! */
	public List<Ability> getDisabledAbilities() {
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
			if (abil.canCast()) {
				if (!abil.abilityCastEvent(event)) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean blockBreakEvent(Player player, BlockBreakEvent event) {
		return conditionalCastCancellable(player, (ability) -> ability.blockBreakEvent(event));
	}

	public void onDamage(Player player, DamageEvent event, LivingEntity enemy) {
		if (!EntityUtils.isHostileMob(enemy)) {
			return;
		}

		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			if (event.isCancelled()) {
				return;
			}
			String metaKey = "LastDamageTick_" + abil.getClass().getCanonicalName();
			if (abil.canCast() && !MetadataUtils.happenedThisTick(player, metaKey)) {
				if (abil.onDamage(event, enemy)) {
					MetadataUtils.checkOnceThisTick(mPlugin, player, metaKey);
				}
			}
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
		for (Ability ability : getPlayerAbilities(player).getAbilities()) {
			if (ability.canCast()) {
				func.run(ability);
			}
		}
	}

	private boolean conditionalCastCancellable(Player player, CastArgumentWithReturn func) {
		for (Ability ability : getPlayerAbilities(player).getAbilities()) {
			if (ability.canCast()) {
				if (!func.run(ability)) {
					return false;
				}
			}
		}
		return true;
	}

	public void onHurt(Player player, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			if (event.isCancelled()) {
				return;
			}
			if (abil.canCast()) {
				abil.onHurt(event, damager, source);
			}
		}
	}

	public void onHurtFatal(Player player, DamageEvent event) {
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			if (event.isCancelled() || event.getFinalDamage(true) < player.getHealth()) {
				return;
			}
			if (abil.canCast()) {
				abil.onHurtFatal(event);
			}
		}
	}

	public boolean playerCombustByEntityEvent(Player player, EntityCombustByEntityEvent event) {
		return conditionalCastCancellable(player, (ability) -> ability.playerCombustByEntityEvent(event));
	}

	public boolean playerShotArrowEvent(Player player, AbstractArrow arrow) {
		return conditionalCastCancellable(player, (ability) -> ability.playerShotArrowEvent(arrow));
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
		conditionalCast(player, (ability) -> ability.entityDeathEvent(event, shouldGenDrops));
	}

	public void entityDeathRadiusEvent(Player player, EntityDeathEvent event, boolean shouldGenDrops) {
		conditionalCast(player, (ability) -> {
			Location center = ability.entityDeathRadiusCenterLocation();
			if (center != null && event.getEntity().getLocation().distance(center) <= ability.entityDeathRadius()) {
				ability.entityDeathRadiusEvent(event, shouldGenDrops);
			}
		});
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

	/*
	 * EntityUtils.damageEntity() does not trigger the EntityDamageByEntityEvent, and it may not
	 * trigger the CustomDamageEvent either if the flag is set to such. Since we need to attach an
	 * event to ALL damage dealt (currently, for delves to work), we need to trigger a dummy event
	 * for this express purpose that isn't used in things like regular abilities.
	 */
	public void playerDealtUnregisteredCustomDamageEvent(Player player, DamageEvent event) {
		conditionalCast(player, (ability) -> ability.playerDealtUnregisteredCustomDamageEvent(event));
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
		conditionalCast(player, (ability) -> ability.playerAnimationEvent(event));
	}

	public void playerSwapHandItemsEvent(Player player, PlayerSwapHandItemsEvent event) {
		conditionalCast(player, (ability) -> ability.playerSwapHandItemsEvent(event));
	}

	public void playerRegainHealthEvent(Player player, EntityRegainHealthEvent event) {
		conditionalCast(player, (ability) -> ability.playerRegainHealthEvent(event));
	}

	public void playerQuitEvent(Player player, PlayerQuitEvent event) {
		conditionalCast(player, (ability) -> ability.playerQuitEvent(event));
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			UUID uuid = player.getUniqueId();
			mAbilities.remove(uuid);
			mChargeTracker.remove(uuid);
		}, 5);
	}

	public void playerSaveEvent(Player player, PlayerSaveEvent event) {
		HashMap<ClassAbility, Integer> charges = mChargeTracker.get(player.getUniqueId());
		if (charges != null) {
			JsonObject data = new JsonObject();
			for (Map.Entry<ClassAbility, Integer> entry : charges.entrySet()) {
				data.addProperty(entry.getKey().getName(), entry.getValue());
			}
			event.setPluginData(KEY_PLUGIN_DATA, data);
		}
	}

	public void playerJoinEvent(Player player, PlayerJoinEvent event) {
		UUID uuid = player.getUniqueId();
		JsonObject chargesData = MonumentaRedisSyncAPI.getPlayerPluginData(uuid, KEY_PLUGIN_DATA);
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
	}

	public void playerInteractEvent(Player player, Action action, Material blockClicked) {
		// Right clicking sometimes counts as two clicks, so make sure this can only be triggered once per tick
		if (MetadataUtils.checkOnceThisTick(mPlugin, player, CLICK_TICK_METAKEY)) {
			for (Ability ability : getPlayerAbilities(player).getAbilities()) {
				AbilityInfo abilityInfo = ability.getInfo();
				if (abilityInfo.mTrigger != null) {
					if (abilityInfo.mTrigger == AbilityTrigger.LEFT_CLICK) {
						if (
							action == Action.LEFT_CLICK_AIR
								|| action == Action.LEFT_CLICK_BLOCK
						) {
							if (ability.canCast()) {
								ability.cast(action);
							}
						}
					} else if (abilityInfo.mTrigger == AbilityTrigger.RIGHT_CLICK) {
						if (
							action == Action.RIGHT_CLICK_AIR
							|| (action == Action.RIGHT_CLICK_BLOCK && !ItemUtils.interactableBlocks.contains(blockClicked))
						) {
							if (ability.canCast()) {
								ability.cast(action);
							}
						}
					} else if (abilityInfo.mTrigger == AbilityTrigger.LEFT_CLICK_AIR) {
						if (action == Action.LEFT_CLICK_AIR) {
							if (ability.canCast()) {
								ability.cast(action);
							}
						}
					} else if (abilityInfo.mTrigger == AbilityTrigger.RIGHT_CLICK_AIR) {
						if (action == Action.RIGHT_CLICK_AIR) {
							if (ability.canCast()) {
								ability.cast(action);
							}
						}
					} else if (abilityInfo.mTrigger == AbilityTrigger.ALL) {
						if (ability.canCast()) {
							ability.cast(action);
						}
					}
				}
			}
		}
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
		// Clear all Reference Abilities from player
		for (Ability ref : mReferenceAbilities) {
			String scoreboard = ref.getScoreboard();
			if (scoreboard != null) {
				ScoreboardUtils.setScoreboardValue(player, scoreboard, 0);
			}
		}
		// Clear all Disabled Abilities from player
		for (Ability dRef : mDisabledAbilities) {
			String scoreboard = dRef.getScoreboard();
			if (scoreboard != null) {
				ScoreboardUtils.setScoreboardValue(player, scoreboard, 0);
			}
		}
		// Reset Skill and SkillSpec
		int skill = ScoreboardUtils.getScoreboardValue(player, "TotalLevel").orElse(0);
		int spec = ScoreboardUtils.getScoreboardValue(player, "TotalSpec").orElse(0);
		ScoreboardUtils.setScoreboardValue(player, "Skill", skill);
		ScoreboardUtils.setScoreboardValue(player, "SkillSpec", spec);

		// Run updatePlayerAbilities to clear existing ability effects.
		updatePlayerAbilities(player, true);
	}

	public void trackCharges(Player player, ClassAbility ability, int charges) {
		UUID uuid = player.getUniqueId();
		HashMap<ClassAbility, Integer> playerCharges = mChargeTracker.get(uuid);
		if (playerCharges == null) {
			playerCharges = new HashMap<>();
		}
		playerCharges.put(ability, charges);
		mChargeTracker.put(uuid, playerCharges);
	}

	public int getTrackedCharges(Player player, ClassAbility ability) {
		HashMap<ClassAbility, Integer> playerCharges = mChargeTracker.get(player.getUniqueId());
		if (playerCharges != null) {
			Integer charges = playerCharges.get(ability);
			if (charges != null) {
				return charges.intValue();
			}
		}
		return 0;
	}

	//---------------------------------------------------------------------------------------------------------------
}
