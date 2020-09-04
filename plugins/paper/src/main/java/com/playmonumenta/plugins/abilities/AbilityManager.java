package com.playmonumenta.plugins.abilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.alchemist.AlchemicalArtillery;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.abilities.alchemist.BasiliskPoison;
import com.playmonumenta.plugins.abilities.alchemist.BrutalAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.EnfeeblingElixir;
import com.playmonumenta.plugins.abilities.alchemist.GruesomeAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.IronTincture;
import com.playmonumenta.plugins.abilities.alchemist.NonAlchemistPotionPassive;
import com.playmonumenta.plugins.abilities.alchemist.PowerInjection;
import com.playmonumenta.plugins.abilities.alchemist.UnstableArrows;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.AlchemicalAmalgam;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.InvigoratingOdor;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.WardingRemedy;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.WardingRemedyNonApothecary;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.NightmarishAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.PurpleHaze;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.ScorchedEarth;
import com.playmonumenta.plugins.abilities.cleric.Celestial;
import com.playmonumenta.plugins.abilities.cleric.CleansingRain;
import com.playmonumenta.plugins.abilities.cleric.ClericPassive;
import com.playmonumenta.plugins.abilities.cleric.DivineJustice;
import com.playmonumenta.plugins.abilities.cleric.HandOfLight;
import com.playmonumenta.plugins.abilities.cleric.HeavenlyBoon;
import com.playmonumenta.plugins.abilities.cleric.NonClericProvisionsPassive;
import com.playmonumenta.plugins.abilities.cleric.Rejuvenation;
import com.playmonumenta.plugins.abilities.cleric.SacredProvisions;
import com.playmonumenta.plugins.abilities.cleric.Sanctified;
import com.playmonumenta.plugins.abilities.cleric.hierophant.EnchantedPrayer;
import com.playmonumenta.plugins.abilities.cleric.hierophant.HallowedBeam;
import com.playmonumenta.plugins.abilities.cleric.hierophant.ThuribleProcession;
import com.playmonumenta.plugins.abilities.cleric.paladin.ChoirBells;
import com.playmonumenta.plugins.abilities.cleric.paladin.HolyJavelin;
import com.playmonumenta.plugins.abilities.cleric.paladin.LuminousInfusion;
import com.playmonumenta.plugins.abilities.delves.cursed.Mystic;
import com.playmonumenta.plugins.abilities.delves.cursed.Ruthless;
import com.playmonumenta.plugins.abilities.delves.cursed.Spectral;
import com.playmonumenta.plugins.abilities.delves.cursed.Unyielding;
import com.playmonumenta.plugins.abilities.delves.twisted.Arcanic;
import com.playmonumenta.plugins.abilities.delves.twisted.Dreadful;
import com.playmonumenta.plugins.abilities.delves.twisted.Merciless;
import com.playmonumenta.plugins.abilities.delves.twisted.Relentless;
import com.playmonumenta.plugins.abilities.mage.ArcaneStrike;
import com.playmonumenta.plugins.abilities.mage.Channeling;
import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.abilities.mage.FrostNova;
import com.playmonumenta.plugins.abilities.mage.MagePassive;
import com.playmonumenta.plugins.abilities.mage.MagmaShield;
import com.playmonumenta.plugins.abilities.mage.ManaLance;
import com.playmonumenta.plugins.abilities.mage.PrismaticShield;
import com.playmonumenta.plugins.abilities.mage.Spellshock;
import com.playmonumenta.plugins.abilities.mage.arcanist.FlashSword;
import com.playmonumenta.plugins.abilities.mage.arcanist.Overload;
import com.playmonumenta.plugins.abilities.mage.arcanist.SagesInsight;
import com.playmonumenta.plugins.abilities.mage.elementalist.Blizzard;
import com.playmonumenta.plugins.abilities.mage.elementalist.ElementalSpiritFire;
import com.playmonumenta.plugins.abilities.mage.elementalist.ElementalSpiritIce;
import com.playmonumenta.plugins.abilities.mage.elementalist.Starfall;
import com.playmonumenta.plugins.abilities.other.CluckingPotions;
import com.playmonumenta.plugins.abilities.other.PatreonGreen;
import com.playmonumenta.plugins.abilities.other.PatreonPurple;
import com.playmonumenta.plugins.abilities.other.PatreonRed;
import com.playmonumenta.plugins.abilities.other.PatreonWhite;
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
import com.playmonumenta.plugins.abilities.scout.BowMastery;
import com.playmonumenta.plugins.abilities.scout.EagleEye;
import com.playmonumenta.plugins.abilities.scout.FinishingBlow;
import com.playmonumenta.plugins.abilities.scout.ScoutPassive;
import com.playmonumenta.plugins.abilities.scout.Sharpshooter;
import com.playmonumenta.plugins.abilities.scout.SwiftCuts;
import com.playmonumenta.plugins.abilities.scout.Swiftness;
import com.playmonumenta.plugins.abilities.scout.Volley;
import com.playmonumenta.plugins.abilities.scout.hunter.EnchantedShot;
import com.playmonumenta.plugins.abilities.scout.hunter.PinningShot;
import com.playmonumenta.plugins.abilities.scout.hunter.SplitArrow;
import com.playmonumenta.plugins.abilities.scout.ranger.Quickdraw;
import com.playmonumenta.plugins.abilities.scout.ranger.Reflexes;
import com.playmonumenta.plugins.abilities.scout.ranger.TacticalManeuver;
import com.playmonumenta.plugins.abilities.warlock.AmplifyingHex;
import com.playmonumenta.plugins.abilities.warlock.BlasphemousAura;
import com.playmonumenta.plugins.abilities.warlock.ConsumingFlames;
import com.playmonumenta.plugins.abilities.warlock.CursedWound;
import com.playmonumenta.plugins.abilities.warlock.Exorcism;
import com.playmonumenta.plugins.abilities.warlock.GraspingClaws;
import com.playmonumenta.plugins.abilities.warlock.Harvester;
import com.playmonumenta.plugins.abilities.warlock.SoulRend;
import com.playmonumenta.plugins.abilities.warlock.WarlockPassive;
import com.playmonumenta.plugins.abilities.warlock.reaper.DarkPact;
import com.playmonumenta.plugins.abilities.warlock.reaper.DeathsTouch;
import com.playmonumenta.plugins.abilities.warlock.reaper.DeathsTouchNonReaper;
import com.playmonumenta.plugins.abilities.warlock.reaper.HungeringVortex;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.EerieEminence;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.FractalEnervation;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.WitheringGaze;
import com.playmonumenta.plugins.abilities.warrior.BruteForce;
import com.playmonumenta.plugins.abilities.warrior.CounterStrike;
import com.playmonumenta.plugins.abilities.warrior.DefensiveLine;
import com.playmonumenta.plugins.abilities.warrior.Frenzy;
import com.playmonumenta.plugins.abilities.warrior.Riposte;
import com.playmonumenta.plugins.abilities.warrior.ShieldBash;
import com.playmonumenta.plugins.abilities.warrior.Toughness;
import com.playmonumenta.plugins.abilities.warrior.WarriorPassive;
import com.playmonumenta.plugins.abilities.warrior.WeaponryMastery;
import com.playmonumenta.plugins.abilities.warrior.berserker.MeteorSlam;
import com.playmonumenta.plugins.abilities.warrior.berserker.Rampage;
import com.playmonumenta.plugins.abilities.warrior.berserker.RecklessSwing;
import com.playmonumenta.plugins.abilities.warrior.guardian.Bodyguard;
import com.playmonumenta.plugins.abilities.warrior.guardian.Challenge;
import com.playmonumenta.plugins.abilities.warrior.guardian.ShieldWall;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.events.PotionEffectApplyEvent;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class AbilityManager {

	private static final String CLICK_TICK_METAKEY = "ClickedThisTickMetakey";
	private static final float DEFAULT_WALK_SPEED = 0.2f;

	private static AbilityManager mManager = null;

	private final Plugin mPlugin;
	private final World mWorld;
	private final List<Ability> mReferenceAbilities;
	private final List<Ability> mDisabledAbilities;
	private final Map<UUID, AbilityCollection> mAbilities = new HashMap<>();

	//Public manager methods
	//---------------------------------------------------------------------------------------------------------------

	public AbilityManager(Plugin plugin, World world) {
		mPlugin = plugin;
		mWorld = world;
		mManager = this;

		mReferenceAbilities = new ArrayList<Ability>();
		mDisabledAbilities = new ArrayList<Ability>();

		List<Ability> specAbilitiesPriority = Arrays.asList(
				                           // Damage multiplying skills must come before damage bonus skills
			                               new RecklessSwing(mPlugin, mWorld, null),
			                               new DarkPact(mPlugin, mWorld, null),

			                               // Starfall needs to come before Mana Lance
			                               new Starfall(mPlugin, mWorld, null)
			                           );

		if (ServerProperties.getClassSpecializationsEnabled()) {
			mReferenceAbilities.addAll(specAbilitiesPriority);
		} else {
			mDisabledAbilities.addAll(specAbilitiesPriority);
		}

		mReferenceAbilities.addAll(Arrays.asList(
		                               // ALL (CLUCKING POTIONS)
		                               new CluckingPotions(mPlugin, mWorld, null),

		                               // ALL PLAYERS (but technically for Alchemist)
		                               new NonAlchemistPotionPassive(mPlugin, mWorld, null),
		                               // ALL PLAYERS (but technically for Cleric)
		                               new NonClericProvisionsPassive(mPlugin, mWorld, null),

		                               // All other non-class abilities
		                               new PvP(mPlugin, mWorld, null),
		                               new PatreonWhite(mPlugin, mWorld, null),
		                               new PatreonGreen(mPlugin, mWorld, null),
		                               new PatreonPurple(mPlugin, mWorld, null),
		                               new PatreonRed(mPlugin, mWorld, null),

		                               //********** MAGE **********//
		                               new ArcaneStrike(mPlugin, mWorld, null),
		                               new Channeling(mPlugin, mWorld, null),
		                               new ElementalArrows(mPlugin, mWorld, null),
		                               new FrostNova(mPlugin, mWorld, null),
		                               new MagePassive(mPlugin, mWorld, null),
		                               new MagmaShield(mPlugin, mWorld, null),
		                               new ManaLance(mPlugin, mWorld, null),
		                               new Spellshock(mPlugin, mWorld, null),

		                               //********** ROGUE **********//
		                               new AdvancingShadows(mPlugin, mWorld, null),
		                               new ByMyBlade(mPlugin, mWorld, null),
		                               new DaggerThrow(mPlugin, mWorld, null),
		                               new Dodging(mPlugin, mWorld, null),
		                               new RoguePassive(mPlugin, mWorld, null),
		                               new Smokescreen(mPlugin, mWorld, null),
		                               new ViciousCombos(mPlugin, mWorld, null),
		                               new Skirmisher(mPlugin, mWorld, null),

		                               //********** SCOUT **********//
		                               new Agility(mPlugin, mWorld, null),
		                               new BowMastery(mPlugin, mWorld, null),
		                               new Volley(mPlugin, mWorld, null),
		                               new Swiftness(mPlugin, mWorld, null),
		                               new EagleEye(mPlugin, mWorld, null),
		                               new ScoutPassive(mPlugin, mWorld, null),
		                               new SwiftCuts(mPlugin, mWorld, null),
		                               new Sharpshooter(mPlugin, mWorld, null),
		                               new FinishingBlow(mPlugin, mWorld, null),

		                               //********** WARRIOR **********//
		                               new BruteForce(mPlugin, mWorld, null),
		                               new CounterStrike(mPlugin, mWorld, null),
		                               new DefensiveLine(mPlugin, mWorld, null),
		                               new Frenzy(mPlugin, mWorld, null),
		                               new Riposte(mPlugin, mWorld, null),
		                               new ShieldBash(mPlugin, mWorld, null),
		                               new Toughness(mPlugin, mWorld, null),
		                               new WarriorPassive(mPlugin, mWorld, null),
		                               new WeaponryMastery(mPlugin, mWorld, null),

		                               //********** CLERIC **********//
		                               new Celestial(mPlugin, mWorld, null),
		                               new CleansingRain(mPlugin, mWorld, null),
		                               new HandOfLight(mPlugin, mWorld, null),
		                               new ClericPassive(mPlugin, mWorld, null),
		                               new DivineJustice(mPlugin, mWorld, null),
		                               new HeavenlyBoon(mPlugin, mWorld, null),
		                               new Rejuvenation(mPlugin, mWorld, null),
		                               new Sanctified(mPlugin, mWorld, null),
		                               new SacredProvisions(mPlugin, mWorld, null),

		                               //********** WARLOCK **********//
		                               new AmplifyingHex(mPlugin, mWorld, null),
		                               new BlasphemousAura(mPlugin, mWorld, null),
		                               new ConsumingFlames(mPlugin, mWorld, null),
		                               new CursedWound(mPlugin, mWorld, null),
		                               new GraspingClaws(mPlugin, mWorld, null),
		                               new WarlockPassive(mPlugin, mWorld, null),
		                               new Harvester(mPlugin, mWorld, null),
		                               new SoulRend(mPlugin, mWorld, null),
		                               new Exorcism(mPlugin, mWorld, null),

		                               //********** ALCHEMIST **********//
		                               new AlchemicalArtillery(mPlugin, mWorld, null),
		                               new BasiliskPoison(mPlugin, mWorld, null),
		                               new UnstableArrows(mPlugin, mWorld, null),
		                               new PowerInjection(mPlugin, mWorld, null),
		                               new IronTincture(mPlugin, mWorld, null),
		                               new GruesomeAlchemy(mPlugin, mWorld, null),
		                               new BrutalAlchemy(mPlugin, mWorld, null),
		                               new EnfeeblingElixir(mPlugin, mWorld, null),
		                               new AlchemistPotions(mPlugin, mWorld, null)
		                           ));

		List<Ability> specAbilities = Arrays.asList(
                //********** MAGE **********//
                // ELEMENTALIST
				   // Starfall up above
                new ElementalSpiritFire(mPlugin, mWorld, null),
                new ElementalSpiritIce(mPlugin, mWorld, null),
                new Blizzard(mPlugin, mWorld, null),

                // MAGE SWORDSMAN
                new FlashSword(mPlugin, mWorld, null),
                new Overload(mPlugin, mWorld, null),
                new SagesInsight(mPlugin, mWorld, null),

                //********** ROGUE **********//
                // SWORDSAGE
                new WindWalk(mPlugin, mWorld, null),
                new BladeDance(mPlugin, mWorld, null),
                new DeadlyRonde(mPlugin, mWorld, null),

                // ASSASSIN
                new BodkinBlitz(mPlugin, mWorld, null),
                new CloakAndDagger(mPlugin, mWorld, null),
                new CoupDeGrace(mPlugin, mWorld, null),

                //********** SCOUT **********//
                // RANGER
                new TacticalManeuver(mPlugin, mWorld, null),
                new Reflexes(mPlugin, mWorld, null),
                new Quickdraw(mPlugin, mWorld, null),

                // HUNTER
                new EnchantedShot(mPlugin, mWorld, null),
                new PinningShot(mPlugin, mWorld, null),
                new SplitArrow(mPlugin, mWorld, null),

                //********** WARRIOR **********//
                // BERSERKER
                new MeteorSlam(mPlugin, mWorld, null),
                new Rampage(mPlugin, mWorld, null),

                // GUARDIAN
                new ShieldWall(mPlugin, mWorld, null),
                new Challenge(mPlugin, mWorld, null),
                new Bodyguard(mPlugin, mWorld, null),

                //********** CLERIC **********//
                // PALADIN
                new HolyJavelin(mPlugin, mWorld, null),
                new ChoirBells(mPlugin, mWorld, null),
                new LuminousInfusion(mPlugin, mWorld, null),

                // HIEROPHANT
                new EnchantedPrayer(mPlugin, mWorld, null),
                new HallowedBeam(mPlugin, mWorld, null),
                new ThuribleProcession(mPlugin, mWorld, null),

                //********** WARLOCK **********//
                // REAPER
                new DeathsTouch(mPlugin, mWorld, null),
                new HungeringVortex(mPlugin, mWorld, null),
                new DeathsTouchNonReaper(mPlugin, mWorld, null),

                // TENEBRIST
                new EerieEminence(mPlugin, mWorld, null),
                new FractalEnervation(mPlugin, mWorld, null),
                new WitheringGaze(mPlugin, mWorld, null),

                //********** ALCHEMIST **********//
                // HARBINGER
                new ScorchedEarth(mPlugin, mWorld, null),
                new NightmarishAlchemy(mPlugin, mWorld, null),
                new PurpleHaze(mPlugin, mWorld, null),

                // APOTHECARY
                new AlchemicalAmalgam(mPlugin, mWorld, null),
                new InvigoratingOdor(mPlugin, mWorld, null),
                new WardingRemedy(mPlugin, mWorld, null),
                new WardingRemedyNonApothecary(mPlugin, mWorld, null)
            );

		if (ServerProperties.getClassSpecializationsEnabled()) {
			mReferenceAbilities.addAll(specAbilities);
		} else {
			mDisabledAbilities.addAll(specAbilities);
		}

		// These abilities should trigger after all event damage is calculated
		mReferenceAbilities.addAll(Arrays.asList(
									   //********** DELVES **********//
		                               // CURSED
		                               new Ruthless(mPlugin, mWorld, null),
		                               new Unyielding(mPlugin, mWorld, null),
		                               new Mystic(mPlugin, mWorld, null),
		                               new Spectral(mPlugin, mWorld, null),
		                               // TWISTED
		                               new Merciless(mPlugin, mWorld, null),
		                               new Relentless(mPlugin, mWorld, null),
		                               new Arcanic(mPlugin, mWorld, null),
		                               new Dreadful(mPlugin, mWorld, null),

									   new PrismaticShield(mPlugin, mWorld, null),
		                               new EscapeDeath(mPlugin, mWorld, null)
		                           ));
	}

	public static AbilityManager getManager() {
		return mManager;
	}

	public void updatePlayerAbilities(Player player) {
		// Clear self-given potions
		mPlugin.mPotionManager.clearPotionIDType(player, PotionID.ABILITY_SELF);

		AttributeInstance knockbackResistance = player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
		AttributeInstance movementSpeed = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
		AttributeInstance armor = player.getAttribute(Attribute.GENERIC_ARMOR);
		AttributeInstance toughness = player.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS);
		AttributeInstance attackDamage = player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
		AttributeInstance attackSpeed = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
		AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);

		// Reset passive buffs to player base attributes
		if (knockbackResistance != null) {
			knockbackResistance.setBaseValue(0);
		}
		if (armor != null) {
			armor.setBaseValue(0);
		}
		if (toughness != null) {
			toughness.setBaseValue(0);
		}
		if (attackDamage != null) {
			attackDamage.setBaseValue(1);
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
		Toughness.removeModifier(player);
		Swiftness.removeModifier(player);
		player.setWalkSpeed(DEFAULT_WALK_SPEED);
		player.setInvulnerable(false);
		// The absorption tracker may lose track of the player when doing things like shard transfers, so reset absorption
		AbsorptionUtils.setAbsorption(player, 0, -1);

		/* Get the old ability list and run invalidate() on all of them to clean up lingering runnables */
		if (mAbilities.containsKey(player.getUniqueId())) {
			for (Ability abil : getPlayerAbilities(player).getAbilities()) {
				abil.invalidate();
			}
		}

		List<Ability> abilities = new ArrayList<>();

		if (player.getScoreboardTags().contains("disable_class") || player.getGameMode().equals(GameMode.SPECTATOR)) {
			/* This player's abilities are disabled - give them an empty set and stop here */
			mAbilities.put(player.getUniqueId(), new AbilityCollection(abilities));
			return;
		}

		try {
			for (Ability ab : mReferenceAbilities) {
				if (ab.canUse(player)) {
					Class[] constructorTypes = new Class[3];
					constructorTypes[0] = Plugin.class;
					constructorTypes[1] = World.class;
					constructorTypes[2] = Player.class;

					Ability newAbility = ab.getClass().getDeclaredConstructor(constructorTypes).newInstance(mPlugin, mWorld, player);
					abilities.add(newAbility);
				}
			}
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | java.lang.reflect.InvocationTargetException e) {
			e.printStackTrace();
		}

		mAbilities.put(player.getUniqueId(), new AbilityCollection(abilities));

		// Set up new class potion abilities
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			abil.setupClassPotionEffects();
		}
	}

	public <T extends Ability> T getPlayerAbility(Player player, Class<T> cls) {
		return getPlayerAbilities(player).getAbility(cls);
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
		return getPlayerAbilities(player).getAbility(PvP.class) != null;
	}

	public String printInfo(Player player) {
		JsonObject object = getPlayerAbilities(player).getAsJsonObject();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(object);
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

	public boolean livingEntityDamagedByPlayerEvent(Player player, EntityDamageByEntityEvent event) {
		// Use the counter instead of scoreboard name because some "abilities" do not have a scoreboard
		int i = 0;
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			i++;
			if (abil.canCast()) {
				// Do not allow any skills with no cooldown to apply damage more than once
				// Always allow sweep attacks to go through for things like Deadly Ronde
				if (event.getCause() != DamageCause.ENTITY_SWEEP_ATTACK && !abil.getInfo().mIgnoreTriggerCap
				    && (abil.getInfo().mCooldown == 0 || abil.getInfo().mIgnoreCooldown)
				    && !MetadataUtils.checkOnceThisTick(mPlugin, player, i + "LivingEntityDamagedByPlayerEventTickTriggered")) {
					return true;
				}
				if (!abil.livingEntityDamagedByPlayerEvent(event)) {
					return false;
				}
				if (AbilityUtils.isStealthed(player) && !abil.onStealthAttack(event)) {
					return false;
				}
			}
		}
		return true;
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

	public boolean playerDamagedEvent(Player player, EntityDamageEvent event) {
		return conditionalCastCancellable(player, (ability) -> ability.playerDamagedEvent(event));
	}

	public boolean playerDamagedByLivingEntityEvent(Player player, EntityDamageByEntityEvent event) {
		return conditionalCastCancellable(player, (ability) -> ability.playerDamagedByLivingEntityEvent(event));
	}

	public boolean playerDamagedByProjectileEvent(Player player, EntityDamageByEntityEvent event) {
		return conditionalCastCancellable(player, (ability) -> ability.playerDamagedByProjectileEvent(event));
	}

	public boolean playerCombustByEntityEvent(Player player, EntityCombustByEntityEvent event) {
		return conditionalCastCancellable(player, (ability) -> ability.playerCombustByEntityEvent(event));
	}

	public boolean livingEntityShotByPlayerEvent(Player player, Projectile proj, LivingEntity damagee, EntityDamageByEntityEvent event) {
		return conditionalCastCancellable(player, (ability) -> ability.livingEntityShotByPlayerEvent(proj, damagee, event));
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
			if (event.getEntity().getLocation().distance(player.getLocation()) <= ability.entityDeathRadius()) {
				ability.entityDeathRadiusEvent(event, shouldGenDrops);
			}
		});
	}

	public void playerItemHeldEvent(Player player, ItemStack mainHand, ItemStack offHand) {
		conditionalCast(player, (ability) -> ability.playerItemHeldEvent(mainHand, offHand));
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

	public void periodicTrigger(Player player, boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		conditionalCast(player, (ability) -> ability.periodicTrigger(fourHertz, twoHertz, oneSecond, ticks));
	}

	public void playerDealtCustomDamageEvent(Player player, CustomDamageEvent event) {
		conditionalCast(player, (ability) -> ability.playerDealtCustomDamageEvent(event));
	}

	/*
	 * EntityUtils.damageEntity() does not trigger the EntityDamageByEntityEvent, and it may not
	 * trigger the CustomDamageEvent either if the flag is set to such. Since we need to attach an
	 * event to ALL damage dealt (currently, for delves to work), we need to trigger a dummy event
	 * for this express purpose that isn't used in things like regular abilities.
	 */
	public void playerDealtUnregisteredCustomDamageEvent(Player player, CustomDamageEvent event) {
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

	public void playerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		// Right clicking sometimes counts as two clicks, so make sure this can only be triggered once per tick
		if (MetadataUtils.checkOnceThisTick(mPlugin, player, CLICK_TICK_METAKEY)) {
			for (Ability abil : getPlayerAbilities(player).getAbilities()) {
				AbilityInfo info = abil.getInfo();
				if (info.mTrigger != null) {
					if (info.mTrigger == AbilityTrigger.LEFT_CLICK) {
						if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
							if (abil.runCheck() && !abil.isOnCooldown()) {
								abil.cast(action);
							}
						}
					} else if (info.mTrigger == AbilityTrigger.RIGHT_CLICK) {
						if (action == Action.RIGHT_CLICK_AIR
						    || (action == Action.RIGHT_CLICK_BLOCK && !ItemUtils.interactableBlocks.contains(blockClicked))) {
							if (abil.runCheck() && !abil.isOnCooldown()) {
								abil.cast(action);
							}
						}
					} else if (info.mTrigger == AbilityTrigger.LEFT_CLICK_AIR) {
						if (action == Action.LEFT_CLICK_AIR) {
							if (abil.runCheck() && !abil.isOnCooldown()) {
								abil.cast(action);
							}
						}
					} else if (info.mTrigger == AbilityTrigger.RIGHT_CLICK_AIR) {
						if (action == Action.RIGHT_CLICK_AIR) {
							if (abil.runCheck() && !abil.isOnCooldown()) {
								abil.cast(action);
							}
						}
					} else if (info.mTrigger == AbilityTrigger.ALL) {
						abil.cast(action);
					}
				}
			}
		}
	}

	//---------------------------------------------------------------------------------------------------------------

	//Private methods
	public AbilityCollection getPlayerAbilities(Player player) {
		if (!mAbilities.containsKey(player.getUniqueId())) {
			updatePlayerAbilities(player);
		}
		return mAbilities.get(player.getUniqueId());
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
		int skill = ScoreboardUtils.getScoreboardValue(player, "TotalLevel");
		int spec = ScoreboardUtils.getScoreboardValue(player, "TotalSpec");
		ScoreboardUtils.setScoreboardValue(player, "Skill", skill);
		ScoreboardUtils.setScoreboardValue(player, "SkillSpec", spec);

		// Run updatePlayerAbilities to clear existing ability effects.
		updatePlayerAbilities(player);
	}

	//---------------------------------------------------------------------------------------------------------------
}
