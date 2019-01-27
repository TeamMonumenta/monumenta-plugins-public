package com.playmonumenta.plugins.abilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.SplashPotion;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.abilities.alchemist.BasiliskPoison;
import com.playmonumenta.plugins.abilities.alchemist.BrutalAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.EnfeeblingElixir;
import com.playmonumenta.plugins.abilities.alchemist.GruesomeAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.IronTincture;
import com.playmonumenta.plugins.abilities.alchemist.NonAlchemistPotionPassive;
import com.playmonumenta.plugins.abilities.alchemist.PowerInjection;
import com.playmonumenta.plugins.abilities.alchemist.UnstableArrows;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.AdrenalSerum;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.NightmarishAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.harbinger.PurpleHaze;
import com.playmonumenta.plugins.abilities.cleric.Celestial;
import com.playmonumenta.plugins.abilities.cleric.CleansingRain;
import com.playmonumenta.plugins.abilities.cleric.ClericPassive;
import com.playmonumenta.plugins.abilities.cleric.DivineJustice;
import com.playmonumenta.plugins.abilities.cleric.HandOfLight;
import com.playmonumenta.plugins.abilities.cleric.HeavenlyBoon;
import com.playmonumenta.plugins.abilities.cleric.Rejuvenation;
import com.playmonumenta.plugins.abilities.cleric.Sanctified;
import com.playmonumenta.plugins.abilities.cleric.paladin.ChoirBells;
import com.playmonumenta.plugins.abilities.cleric.paladin.HolyJavelin;
import com.playmonumenta.plugins.abilities.cleric.paladin.LuminousInfusion;
import com.playmonumenta.plugins.abilities.mage.ArcaneStrike;
import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.abilities.mage.FrostNova;
import com.playmonumenta.plugins.abilities.mage.MagePassive;
import com.playmonumenta.plugins.abilities.mage.MagmaShield;
import com.playmonumenta.plugins.abilities.mage.ManaLance;
import com.playmonumenta.plugins.abilities.mage.PrismaticShield;
import com.playmonumenta.plugins.abilities.mage.Spellshock;
import com.playmonumenta.plugins.abilities.mage.elementalist.ElementalSpiritAbility;
import com.playmonumenta.plugins.abilities.mage.elementalist.FrostRay;
import com.playmonumenta.plugins.abilities.mage.elementalist.MeteorStrike;
import com.playmonumenta.plugins.abilities.rogue.AdvancingShadows;
import com.playmonumenta.plugins.abilities.rogue.ByMyBlade;
import com.playmonumenta.plugins.abilities.rogue.DaggerThrow;
import com.playmonumenta.plugins.abilities.rogue.Dodging;
import com.playmonumenta.plugins.abilities.rogue.EscapeDeath;
import com.playmonumenta.plugins.abilities.rogue.RoguePassive;
import com.playmonumenta.plugins.abilities.rogue.Smokescreen;
import com.playmonumenta.plugins.abilities.rogue.ViciousCombos;
import com.playmonumenta.plugins.abilities.rogue.swordsage.BladeDance;
import com.playmonumenta.plugins.abilities.rogue.swordsage.DeadlyRonde;
import com.playmonumenta.plugins.abilities.rogue.swordsage.WindWalk;
import com.playmonumenta.plugins.abilities.scout.Agility;
import com.playmonumenta.plugins.abilities.scout.BowMastery;
import com.playmonumenta.plugins.abilities.scout.EagleEye;
import com.playmonumenta.plugins.abilities.scout.ScoutPassive;
import com.playmonumenta.plugins.abilities.scout.Swiftness;
import com.playmonumenta.plugins.abilities.scout.Volley;
import com.playmonumenta.plugins.abilities.scout.ranger.Disengage;
import com.playmonumenta.plugins.abilities.scout.ranger.PrecisionStrike;
import com.playmonumenta.plugins.abilities.scout.ranger.Quickdraw;
import com.playmonumenta.plugins.abilities.warlock.AmplifyingHex;
import com.playmonumenta.plugins.abilities.warlock.BlasphemousAura;
import com.playmonumenta.plugins.abilities.warlock.ConsumingFlames;
import com.playmonumenta.plugins.abilities.warlock.CursedWound;
import com.playmonumenta.plugins.abilities.warlock.GraspingClaws;
import com.playmonumenta.plugins.abilities.warlock.SoulRend;
import com.playmonumenta.plugins.abilities.warlock.WarlockPassive;
import com.playmonumenta.plugins.abilities.warlock.reaper.DarkPact;
import com.playmonumenta.plugins.abilities.warlock.reaper.DeathsTouch;
import com.playmonumenta.plugins.abilities.warlock.reaper.HungeringVortex;
import com.playmonumenta.plugins.abilities.warrior.BruteForce;
import com.playmonumenta.plugins.abilities.warrior.CounterStrike;
import com.playmonumenta.plugins.abilities.warrior.DefensiveLine;
import com.playmonumenta.plugins.abilities.warrior.Frenzy;
import com.playmonumenta.plugins.abilities.warrior.Riposte;
import com.playmonumenta.plugins.abilities.warrior.Toughness;
import com.playmonumenta.plugins.abilities.warrior.WarriorPassive;
import com.playmonumenta.plugins.abilities.warrior.WeaponryMastery;
import com.playmonumenta.plugins.abilities.warrior.berserker.GrowingRage;
import com.playmonumenta.plugins.abilities.warrior.berserker.MeteorSlam;
import com.playmonumenta.plugins.abilities.warrior.berserker.Rampage;
import com.playmonumenta.plugins.classes.magic.AbilityCastEvent;
import com.playmonumenta.plugins.classes.magic.CustomDamageEvent;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;

public class AbilityManager {
	private static AbilityManager mManager = null;

	private Plugin mPlugin;
	private World mWorld;
	private Random mRandom;
	private List<Ability> mReferenceAbilities;
	private Map<UUID, AbilityCollection> mAbilities = new HashMap<UUID, AbilityCollection>();

	//Public manager methods
	//---------------------------------------------------------------------------------------------------------------

	public AbilityManager(Plugin plugin, World world, Random random) {
		mPlugin = plugin;
		mWorld = world;
		mRandom = random;
		mManager = this;

		mReferenceAbilities = Arrays.asList(
		                          // MAGE
		                          new ArcaneStrike(mPlugin, mWorld, mRandom, null),
		                          new ElementalArrows(mPlugin, mWorld, mRandom, null),
		                          new FrostNova(mPlugin, mWorld, mRandom, null),
		                          new MagePassive(mPlugin, mWorld, mRandom, null),
		                          new MagmaShield(mPlugin, mWorld, mRandom, null),
		                          new ManaLance(mPlugin, mWorld, mRandom, null),
		                          new PrismaticShield(mPlugin, mWorld, mRandom, null),
		                          new Spellshock(mPlugin, mWorld, mRandom, null),
		                          // ELEMENTALIST
		                          new FrostRay(mPlugin, mWorld, mRandom, null),
		                          new MeteorStrike(mPlugin, mWorld, mRandom, null),
		                          new ElementalSpiritAbility(mPlugin, mWorld, mRandom, null),

		                          // ROGUE
		                          new AdvancingShadows(mPlugin, mWorld, mRandom, null),
		                          new ByMyBlade(mPlugin, mWorld, mRandom, null),
		                          new DaggerThrow(mPlugin, mWorld, mRandom, null),
		                          new Dodging(mPlugin, mWorld, mRandom, null),
		                          new EscapeDeath(mPlugin, mWorld, mRandom, null),
		                          new RoguePassive(mPlugin, mWorld, mRandom, null),
		                          new Smokescreen(mPlugin, mWorld, mRandom, null),
		                          new ViciousCombos(mPlugin, mWorld, mRandom, null),

		                          // ROGUE -- SWORDSAGE

		                          new WindWalk(mPlugin, mWorld, mRandom, null),
		                          new BladeDance(mPlugin, mWorld, mRandom, null),
		                          new DeadlyRonde(mPlugin, mWorld, mRandom, null),

		                          // SCOUT
		                          new Agility(mPlugin, mWorld, mRandom, null),
		                          new BowMastery(mPlugin, mWorld, mRandom, null),
		                          new Volley(mPlugin, mWorld, mRandom, null),
		                          new Swiftness(mPlugin, mWorld, mRandom, null),
		                          new EagleEye(mPlugin, mWorld, mRandom, null),
		                          new ScoutPassive(mPlugin, mWorld, mRandom, null),

		                          //RANGER
		                          new Quickdraw(mPlugin, mWorld, mRandom, null),
		                          new Disengage(mPlugin, mWorld, mRandom, null),
		                          new PrecisionStrike(mPlugin, mWorld, mRandom, null),

		                          // WARRIOR
		                          new BruteForce(mPlugin, mWorld, mRandom, null),
		                          new CounterStrike(mPlugin, mWorld, mRandom, null),
		                          new DefensiveLine(mPlugin, mWorld, mRandom, null),
		                          new Frenzy(mPlugin, mWorld, mRandom, null),
		                          new Riposte(mPlugin, mWorld, mRandom, null),
		                          new Toughness(mPlugin, mWorld, mRandom, null),
		                          new WarriorPassive(mPlugin, mWorld, mRandom, null),
		                          new WeaponryMastery(mPlugin, mWorld, mRandom, null),
		                          // BERSERKER
		                          new MeteorSlam(mPlugin, mWorld, mRandom, null),
		                          new GrowingRage(mPlugin, mWorld, mRandom, null),
		                          new Rampage(mPlugin, mWorld, mRandom, null),

		                          // CLERIC
		                          new Celestial(mPlugin, mWorld, mRandom, null),
		                          new CleansingRain(mPlugin, mWorld, mRandom, null),
		                          new HandOfLight(mPlugin, mWorld, mRandom, null),
		                          new ClericPassive(mPlugin, mWorld, mRandom, null),
		                          new DivineJustice(mPlugin, mWorld, mRandom, null),
		                          new HeavenlyBoon(mPlugin, mWorld, mRandom, null),
		                          new Rejuvenation(mPlugin, mWorld, mRandom, null),
		                          new Sanctified(mPlugin, mWorld, mRandom, null),
		                          // PALADIN
		                          new HolyJavelin(mPlugin, mWorld, mRandom, null),
		                          new ChoirBells(mPlugin, mWorld, mRandom, null),
		                          new LuminousInfusion(mPlugin, mWorld, mRandom, null),

		                          //WARLOCK
		                          new AmplifyingHex(mPlugin, mWorld, mRandom, null),
		                          new BlasphemousAura(mPlugin, mWorld, mRandom, null),
		                          new ConsumingFlames(mPlugin, mWorld, mRandom, null),
		                          new CursedWound(mPlugin, mWorld, mRandom, null),
		                          new GraspingClaws(mPlugin, mWorld, mRandom, null),
		                          new SoulRend(mPlugin, mWorld, mRandom, null),
		                          new WarlockPassive(mPlugin, mWorld, mRandom, null),
		                          
		                          //REAPER
		                          new DarkPact(mPlugin, mWorld, mRandom, null),
		                          new DeathsTouch(mPlugin, mWorld, mRandom, null),
		                          new HungeringVortex(mPlugin, mWorld, mRandom, null),

		                          // ALCHEMIST
		                          new BasiliskPoison(mPlugin, mWorld, mRandom, null),
		                          new UnstableArrows(mPlugin, mWorld, mRandom, null),
		                          new PowerInjection(mPlugin, mWorld, mRandom, null),
		                          new IronTincture(mPlugin, mWorld, mRandom, null),
		                          new GruesomeAlchemy(mPlugin, mWorld, mRandom, null),
		                          new BrutalAlchemy(mPlugin, mWorld, mRandom, null),
		                          new EnfeeblingElixir(mPlugin, mWorld, mRandom, null),
		                          new AlchemistPotions(mPlugin, mWorld, mRandom, null),
		                          // HARBINGER
		                          new AdrenalSerum(mPlugin, mWorld, mRandom, null),
		                          new NightmarishAlchemy(mPlugin, mWorld, mRandom, null),
		                          new PurpleHaze(mPlugin, mWorld, mRandom, null),

		                          // ALL PLAYERS (but technically for Alchemist)
		                          new NonAlchemistPotionPassive(mPlugin, mWorld, mRandom, null)
		                      );
	}

	public static AbilityManager getManager() {
		return mManager;
	}

	@SuppressWarnings("rawtypes")
	public void updatePlayerAbilities(Player player) {
		// Clear self-given potions
		mPlugin.mPotionManager.clearPotionIDType(player, PotionID.ABILITY_SELF);

		// Make sure non-warriors don't have passive knockback resistance or armor
		player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(0);
		player.getAttribute(Attribute.GENERIC_ARMOR).setBaseValue(0);
		player.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(1);

		List<Ability> abilities = new ArrayList<Ability>();

		/* If player has the "disable_class" tag, no abilities are assigned to them */
		for (String tag : player.getScoreboardTags()) {
			if (tag.equals("disable_class")) {
				/* This player's abilities are disabled - give them an empty set and stop here */
				mAbilities.put(player.getUniqueId(), new AbilityCollection(abilities));
				return;
			}
		}

		try {
			for (Ability ab : mReferenceAbilities) {
				if (ab.canUse(player)) {
					Class[] constructorTypes = new Class[4];
					constructorTypes[0] = Plugin.class;
					constructorTypes[1] = World.class;
					constructorTypes[2] = Random.class;
					constructorTypes[3] = Player.class;

					Ability newAbility = ab.getClass().getDeclaredConstructor(constructorTypes).newInstance(mPlugin, mWorld, mRandom, player);
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

	public Ability getPlayerAbility(Player player, Class<?> cls) {
		return getPlayerAbilities(player).getAbility(cls);
	}

	public String printInfo(Player player) {
		JsonObject object = getPlayerAbilities(player).getAsJsonObject();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(object);
	}

	/**
	 * Called whenever a player deals damage.
	 *
	 * All classes that modify player damage should register in this function
	 */
	public void modifyDamage(Player player, EntityDamageByEntityEvent event) {
		Celestial.modifyDamage(player, event);
	}

	//Events
	//---------------------------------------------------------------------------------------------------------------

	public boolean AbilityCastEvent(Player player, AbilityCastEvent event) {
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			if (abil.canCast()) {
				if (!abil.AbilityCastEvent(event)) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean LivingEntityDamagedByPlayerEvent(Player player, EntityDamageByEntityEvent event) {
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			if (abil.canCast()) {
				if (!abil.LivingEntityDamagedByPlayerEvent(event)) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean PlayerDamagedEvent(Player player, EntityDamageEvent event) {
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			if (abil.canCast()) {
				if (!abil.PlayerDamagedEvent(event)) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean PlayerDamagedByLivingEntityEvent(Player player, EntityDamageByEntityEvent event) {
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			if (abil.canCast()) {
				if (!abil.PlayerDamagedByLivingEntityEvent(event)) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean PlayerDamagedByProjectileEvent(Player player, EntityDamageByEntityEvent event) {
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			if (abil.canCast()) {
				if (!abil.PlayerDamagedByProjectileEvent(event)) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean PlayerCombustByEntityEvent(Player player, EntityCombustByEntityEvent event) {
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			if (abil.canCast()) {
				if (!abil.PlayerCombustByEntityEvent(event)) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean LivingEntityShotByPlayerEvent(Player player, Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			if (abil.canCast()) {
				if (!abil.LivingEntityShotByPlayerEvent(arrow, (LivingEntity)damagee, event)) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean PlayerShotArrowEvent(Player player, Arrow arrow) {
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			if (abil.canCast()) {
				if (!abil.PlayerShotArrowEvent(arrow)) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean PlayerThrewSplashPotionEvent(Player player, SplashPotion potion) {
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			if (abil.canCast()) {
				if (!abil.PlayerThrewSplashPotionEvent(potion)) {
					return false;
				}
			}
		}
		return true;
	}

	public void EntityDeathEvent(Player player, EntityDeathEvent event, boolean shouldGenDrops) {
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			if (abil.canCast()) {
				abil.EntityDeathEvent(event, shouldGenDrops);
			}
		}
	}

	public void PlayerItemHeldEvent(Player player, ItemStack mainHand, ItemStack offHand) {
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			if (abil.canCast()) {
				abil.PlayerItemHeldEvent(mainHand, offHand);
			}
		}
	}

	public void ProjectileHitEvent(Player player, ProjectileHitEvent event, Arrow arrow) {
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			if (abil.canCast()) {
				abil.ProjectileHitEvent(event, arrow);
			}
		}
	}

	public void PlayerExtendedSneakEvent(Player player) {
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			if (abil.canCast()) {
				abil.PlayerExtendedSneakEvent();
			}
		}
	}

	public boolean PlayerSplashPotionEvent(Player player, Collection<LivingEntity> affectedEntities,
	                                       ThrownPotion potion, PotionSplashEvent event) {
		boolean re = true;
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			if (abil.canCast()) {
				if (!abil.PlayerSplashPotionEvent(affectedEntities, potion, event)) {
					re = false;
				}
			}
		}
		return re;
	}

	public boolean PlayerSplashedByPotionEvent(Player player, Collection<LivingEntity> affectedEntities,
	                                           ThrownPotion potion, PotionSplashEvent event) {
		boolean re = true;
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			if (abil.canCast()) {
				if (!abil.PlayerSplashedByPotionEvent(affectedEntities, potion, event)) {
					re = false;
				}
			}
		}
		return re;
	}

	public void PeriodicTrigger(Player player, boolean twoHertz, boolean oneSecond, boolean twoSeconds, boolean fourtySeconds, boolean sixtySeconds, int originalTime) {
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			if (abil.canCast()) {
				abil.PeriodicTrigger(twoHertz, oneSecond, twoSeconds, fourtySeconds, sixtySeconds, originalTime);
			}
		}
	}

	public void PlayerInteractEvent(Player player, Action action, ItemStack itemInHand, Material blockClicked) {
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			AbilityInfo info = abil.getInfo();
			if (info.trigger != null) {
				if (info.trigger == AbilityTrigger.LEFT_CLICK) {
					if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
						if (abil.runCheck() && !abil.isOnCooldown()) {
							if (abil.cast()) {
								abil.putOnCooldown();
							}
						}
					}
				} else if (info.trigger == AbilityTrigger.RIGHT_CLICK) {
					if (action == Action.RIGHT_CLICK_AIR
					    || (action == Action.RIGHT_CLICK_BLOCK && !blockClicked.isInteractable())) {
						if (abil.runCheck() && !abil.isOnCooldown()) {
							abil.cast();
						}
					}
				}
			}
		}
	}

	public void PlayerDealtCustomDamageEvent(Player player, CustomDamageEvent event) {
		for (Ability abil : getPlayerAbilities(player).getAbilities()) {
			if (abil.canCast()) {
				abil.PlayerDealtCustomDamageEvent(event);
			}
		}
	}

	//---------------------------------------------------------------------------------------------------------------

	//Private methods
	private AbilityCollection getPlayerAbilities(Player player) {
		if (!mAbilities.containsKey(player.getUniqueId())) {
			updatePlayerAbilities(player);
		}
		return mAbilities.get(player.getUniqueId());
	}

	//---------------------------------------------------------------------------------------------------------------
}
