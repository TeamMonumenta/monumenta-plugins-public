package com.playmonumenta.plugins.abilities;

import com.playmonumenta.plugins.player.data.PlayerInfo;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import java.io.File;

import java.net.URISyntaxException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.List;
import java.util.Random;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.World;

public class Ability {

	private static List<Ability> abilities = new ArrayList<Ability>();

	protected World mWorld;
	protected Plugin mPlugin;
	protected Random mRandom;

	public Player player = null;

	public Ability() { }


	private void initialize(World mWorld, Plugin mPlugin, Random mRandom) {
		this.mWorld = mWorld;
		this.mPlugin = mPlugin;
		this.mRandom = mRandom;
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
			AbilityInfo info = getInfo();
			if (info.linkedSpell != null) {
				if (mPlugin.mTimers.isAbilityOnCooldown(player.getUniqueId(), info.linkedSpell))
					return true;
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
		if (runCheck(player) && !isOnCooldown(player)) {
			putOnCooldown(player);
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

	public boolean canUse(Player player, PlayerInfo info) {
		Ability ability = this;
		if (info != null) {
			AbilityInfo aInfo = ability.getInfo();
			if (aInfo != null) {
				if (info.classId == aInfo.classId) {
					if (aInfo.specId < 0)
						return true;
					else if (aInfo.specId == info.classId)
						return true;
				}
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
	@SuppressWarnings({ "unchecked" })
	public void putAbilities(World mWorld, Plugin mPlugin, Random mRandom) throws URISyntaxException {
		File file = new File(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
		try {
			@SuppressWarnings("resource")
			JarFile jar = new JarFile(file);

			for (Enumeration<JarEntry> entry = jar.entries(); entry.hasMoreElements();) {
				JarEntry e = entry.nextElement();
				String name = e.getName().replace("/", ".");

				if (name.endsWith(".class")) {
					name = name.split(".class")[0];
					try {
						Class<?> c = Class.forName(name);
						if (c.getSuperclass() != null && c.getSuperclass() == Ability.class) {
							try {
								Class<? extends Ability> mc = (Class<? extends Ability>) c;
								Ability sp = mc.newInstance();
								initialize(mWorld, mPlugin, mRandom);
								abilities.add(sp);
							} catch (InstantiationException ie) {
								ie.printStackTrace();
							} catch (IllegalAccessException ie) {
								ie.printStackTrace();
							}
						}
					} catch (ExceptionInInitializerError ie) {
						ie.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static List<Ability> getAbilities() { return abilities; }

	public Ability getInstance() {
		try {
			return getClass().newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
