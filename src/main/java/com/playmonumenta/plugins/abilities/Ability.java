package com.playmonumenta.plugins.abilities;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.mage.ArcaneStrike;
import com.playmonumenta.plugins.abilities.mage.ElementalArrows;
import com.playmonumenta.plugins.abilities.mage.FrostNova;
import com.playmonumenta.plugins.abilities.mage.MagePassive;
import com.playmonumenta.plugins.abilities.mage.MagmaShield;
import com.playmonumenta.plugins.abilities.mage.ManaLance;
import com.playmonumenta.plugins.abilities.mage.PrismaticShield;
import com.playmonumenta.plugins.abilities.mage.Spellshock;
import com.playmonumenta.plugins.abilities.rogue.AdvancingShadows;
import com.playmonumenta.plugins.abilities.rogue.ByMyBlade;
import com.playmonumenta.plugins.abilities.rogue.DaggerThrow;
import com.playmonumenta.plugins.abilities.rogue.Dodging;
import com.playmonumenta.plugins.abilities.rogue.EscapeDeath;
import com.playmonumenta.plugins.abilities.rogue.RoguePassive;
import com.playmonumenta.plugins.abilities.rogue.Smokescreen;
import com.playmonumenta.plugins.abilities.rogue.ViciousCombos;
import com.playmonumenta.plugins.abilities.scout.Agility;
import com.playmonumenta.plugins.abilities.scout.BowMastery;
import com.playmonumenta.plugins.abilities.scout.Volley;
import com.playmonumenta.plugins.abilities.warrior.BruteForce;
import com.playmonumenta.plugins.abilities.warrior.CounterStrike;
import com.playmonumenta.plugins.abilities.warrior.DefensiveLine;
import com.playmonumenta.plugins.abilities.warrior.Frenzy;
import com.playmonumenta.plugins.abilities.warrior.Riposte;
import com.playmonumenta.plugins.abilities.warrior.Toughness;
import com.playmonumenta.plugins.abilities.warrior.WarriorPassive;
import com.playmonumenta.plugins.abilities.warrior.WeaponryMastery;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class Ability {

	private static List<Ability> abilities = new ArrayList<Ability>();

	protected World mWorld;
	protected Plugin mPlugin;
	protected Random mRandom;

	public Player player = null;

	private static Ability abil = new Ability();
	
	public Ability() { }

	public static Ability getBadInstance() { return abil; }

	public void initialize(World mWorld, Plugin mPlugin, Random mRandom) {
		this.mWorld = mWorld;
		this.mPlugin = mPlugin;
		this.mRandom = mRandom;
		System.out.println("" + (mPlugin == null) + " " + (mWorld == null) + " " + (mRandom == null));
		System.out.println("" + (this.mPlugin == null) + " " + (this.mWorld == null) + " " + (this.mRandom == null));
	}

	/**
	 * This is used when the ability is casted manually when its
	 * AbilityTrigger (Right Click/Left Click), along with whatever
	 * runCheck() may contain, is correct.
	 * @return if the player managed to cast the spell successfully.
	 */
	public boolean cast(Player player) { return true; }

	/**
	 * Gets the AbilityInfo object, which contains the small data side of the ability itself, and is required to have for any ability.
	 * @return the AbilityInfo object, if one exists. If not, it returns null.
	 */
	public AbilityInfo getInfo() { return null; }

	/**
	 * A custom check if additional checks are needed. For example, if you need to check if a player is looking up or down.
	 * @param player
	 * @return true or false
	 */
	public boolean runCheck(Player player) { return true; }

	public boolean isOnCooldown(Player player) {
		if (getInfo() != null) {
			Bukkit.broadcastMessage("is1");
			AbilityInfo info = getInfo();
			if (info.linkedSpell != null) {
				Bukkit.broadcastMessage("is2 " + (mPlugin == null) + " " + (player == null) + " " + (info == null));
				if (mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), info.linkedSpell)) {
					Bukkit.broadcastMessage("is3");
					return true;
				}
			}
		}
		return false;
	}

	public void putOnCooldown(Player player) {
		if (getInfo() != null) {
			AbilityInfo info = getInfo();
			if (info.linkedSpell != null) {
				if (!mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), info.linkedSpell))
					mPlugin.mTimers.AddCooldown(player.getUniqueId(), info.linkedSpell, info.cooldown);
			}
		}
	}

	/**
	 * A combination of both runCheck and isOnCooldown.
	 * @param player
	 * @return
	 */
	public boolean canCast(Player player) {
		Bukkit.broadcastMessage("c1");
		if (runCheck(player) && !isOnCooldown(player)) {
			Bukkit.broadcastMessage("c2");
			return true;
		}
		return false;
	}

	//Events
	//---------------------------------------------------------------------------------------------------------------

	public boolean LivingEntityDamagedByPlayerEvent(Player player, EntityDamageByEntityEvent event) { return true; }

	public boolean PlayerDamagedByLivingEntityEvent(Player player, EntityDamageByEntityEvent event) { return true; }

	public boolean EntityDeathEvent(Player player, EntityDeathEvent event, boolean shouldGenDrops) { return true; }

	public boolean PlayerDamagedByProjectileEvent(Player player, EntityDamageByEntityEvent event) { return true; }

	public boolean LivingEntityShotByPlayerEvent(Player player, Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) { return true; }

	public boolean PlayerShotArrowEvent(Player player, Arrow arrow) { return true; }

	public void PlayerItemHeldEvent(Player player, ItemStack mainHand, ItemStack offHand) { }

	public void PlayerRespawnEvent(Player player) { }

	//---------------------------------------------------------------------------------------------------------------

	//Other
	//---------------------------------------------------------------------------------------------------------------

	public void setupClassPotionEffects(Player player) { }

	//---------------------------------------------------------------------------------------------------------------
	public boolean canUse(Player player) {
		AbilityInfo aInfo = getInfo();
		if (aInfo != null) {
			if (aInfo.classId == ScoreboardUtils.getScoreboardValue(player, "Class")) {
				if (aInfo.specId < 0)
					return true;
				else if (aInfo.specId == ScoreboardUtils.getScoreboardValue(player, "Specialization"))
					return true;
			}
		}
		return false;
	}

	public int getAbilityScore(Player player) {
		if (getInfo() != null) {
			AbilityInfo info = getInfo();
			if (info.scoreboardId != null)
				return ScoreboardUtils.getScoreboardValue(player, info.scoreboardId);
		}
		return 0;
	}

	/**
	 * Utilizes reflection to grab all ability classes and put them in a list.
	 * If you wish to change this because it's too complex or not preferred, feel free to do so.
	 * This is just how I like it.
	 * @author Someone else
	 * @throws URISyntaxException
	 */
	public void putAbilities(World mWorld, Plugin mPlugin, Random mRandom) {
		
		abilities.add(new ArcaneStrike());
		abilities.add(new ElementalArrows());
		abilities.add(new FrostNova());
		abilities.add(new MagePassive());
		abilities.add(new MagmaShield());
		abilities.add(new ManaLance());
		abilities.add(new PrismaticShield());
		abilities.add(new Spellshock());
		
		abilities.add(new AdvancingShadows());
		abilities.add(new ByMyBlade());
		abilities.add(new DaggerThrow());
		abilities.add(new Dodging());
		abilities.add(new EscapeDeath());
		abilities.add(new RoguePassive());
		abilities.add(new Smokescreen());
		abilities.add(new ViciousCombos());
		
		abilities.add(new Agility());
		abilities.add(new BowMastery());
		abilities.add(new Volley());
		
		abilities.add(new BruteForce());
		abilities.add(new CounterStrike());
		abilities.add(new DefensiveLine());
		abilities.add(new Frenzy());
		abilities.add(new Riposte());
		abilities.add(new Toughness());
		abilities.add(new WarriorPassive());
		abilities.add(new WeaponryMastery());
		System.out.println("" + (mPlugin == null) + " " + (mWorld == null) + " " + (mRandom == null));
		for (Ability abil : abilities) {
			abil.initialize(mWorld, mPlugin, mRandom);
		}
		
//		File file = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
//		try {
//			@SuppressWarnings("resource")
//			JarFile jar = new JarFile(file);
//
//			for (Enumeration<JarEntry> entry = jar.entries(); entry.hasMoreElements();) {
//				JarEntry e = entry.nextElement();
//				String name = e.getName().replace("/", ".");
//
//				if (name.endsWith(".class")) {
//					name = name.split(".class")[0];
//					try {
//						Class<?> c = Class.forName(name);
//						if (c.getSuperclass() != null && c.getSuperclass() == Ability.class) {
//							try {
//								Class<? extends Ability> mc = (Class<? extends Ability>) c;
//								Ability sp = mc.newInstance();
//								initialize(mWorld, mPlugin, mRandom);
//								abilities.add(sp);
//							} catch (InstantiationException ie) {
//								ie.printStackTrace();
//							} catch (IllegalAccessException ie) {
//								ie.printStackTrace();
//							}
//						}
//					} catch (ExceptionInInitializerError ie) {
//						ie.printStackTrace();
//					}
//				}
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}

	public static List<Ability> getAbilities() { return abilities; }

	public Ability getInstance() {
		try {
			Ability newInstance = getClass().newInstance();
			newInstance.mPlugin = this.mPlugin;
			newInstance.mWorld = this.mWorld;
			newInstance.mRandom = this.mRandom;
			return newInstance;
		} catch (InstantiationException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
