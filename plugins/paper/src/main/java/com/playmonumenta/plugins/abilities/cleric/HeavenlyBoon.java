package com.playmonumenta.plugins.abilities.cleric;

import java.util.Collection;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker.KillTriggeredAbility;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.PotionUtils.PotionInfo;

public class HeavenlyBoon extends Ability implements KillTriggeredAbility {

	private static final double HEAVENLY_BOON_1_CHANCE = 0.06;
	private static final double HEAVENLY_BOON_2_CHANCE = 0.1;
	private static final double HEAVENLY_BOON_RADIUS = 12;
	private static final double HEAVENLY_BOON_TRIGGER_INTENSITY = 0.3;

	private final KillTriggeredAbilityTracker mTracker;
	private final double mChance;

	public HeavenlyBoon(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Heavenly Boon");
		mInfo.scoreboardId = "HeavenlyBoon";
		mInfo.mShorthandName = "HB";
		mInfo.mDescriptions.add("Whenever you are hit with a positive splash potion, the effects are also given to other players in a 12 block radius. In addition, whenever you kill an undead mob, you have a 6% chance to be splashed with an Instant Health I potion, as well as either a Speed I, Regen I, or Absorption I potion.");
		mInfo.mDescriptions.add("The chance to be splashed upon killing an Undead increases to 10%, the effect potions can now also be Strength and Resistance, and the durations of each are greater.");
		mTracker = new KillTriggeredAbilityTracker(this);
		mChance = getAbilityScore() == 1 ? HEAVENLY_BOON_1_CHANCE : HEAVENLY_BOON_2_CHANCE;
	}

	/*
	 * When a cleric is hit by a splash potion, distribute full durations of any positive effects to all
	 * nearby players. If a player gets a full-duration effect in this way, they are removed from the
	 * affectedEntities list so they don't get potion effects applied to them twice
	 */
	@Override
	public boolean playerSplashedByPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion,
	                                           PotionSplashEvent event) {
		boolean hasPositiveEffects = PotionUtils.hasPositiveEffects(PotionUtils.getEffects(potion.getItem()));

		if (PotionUtils.hasNegativeEffects(potion.getItem()) && !hasPositiveEffects) {
			return true;
		}

		if (event.isCancelled()) {
			//Another cleric already spread this potion
			return false;
		}

		if (event.getIntensity(mPlayer) >= HEAVENLY_BOON_TRIGGER_INTENSITY) {
			/* If within range, apply full strength of all potion effects to all nearby players */

			for (Player p : PlayerUtils.playersInRange(mPlayer, HEAVENLY_BOON_RADIUS, true)) {
				// Don't buff players that have their class disabled
				if (p.getScoreboardTags().contains("disable_class")) {
					continue;
				}

				/* Apply full-strength effects to players within range */
				for (PotionEffect effect : PotionUtils.getEffects(potion.getItem())) {
					PotionUtils.applyPotion(mPlugin, p, effect);
				}

				/* Remove this player from the "usual" application of potion effects */
				affectedEntities.remove(p);
			}
			return false;
		}

		return true;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		mTracker.updateDamageDealtToBosses(event);
		return true;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		mTracker.updateDamageDealtToBosses(event);
		return true;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		if (shouldGenDrops) {
			triggerOnKill(event.getEntity());
		}
	}

	@Override
	public void triggerOnKill(LivingEntity mob) {
		if (EntityUtils.isUndead(mob) && mRandom.nextDouble() < mChance) {
			ItemStack potions;

			if (getAbilityScore() == 1) {
				int rand = mRandom.nextInt(4);
				if (rand == 0 || rand == 1) {
					potions = ItemUtils.createStackedPotions(PotionEffectType.REGENERATION, 1, 20 * 20, 0,
					                                         "Splash Potion of Regeneration");
				} else if (rand == 2) {
					potions = ItemUtils.createStackedPotions(PotionEffectType.ABSORPTION, 1, 20 * 20, 0,
					                                         "Splash Potion of Absorption");
				} else {
					potions = ItemUtils.createStackedPotions(PotionEffectType.SPEED, 1, 20 * 20, 0,
					                                         "Splash Potion of Speed");
				}
			} else {
				int rand = mRandom.nextInt(5);
				if (rand == 0) {
					potions = ItemUtils.createStackedPotions(PotionEffectType.REGENERATION, 1, 50 * 20, 0,
					                                         "Splash Potion of Regeneration");
				} else if (rand == 1) {
					potions = ItemUtils.createStackedPotions(PotionEffectType.ABSORPTION, 1, 50 * 20, 0,
					                                         "Splash Potion of Absorption");
				} else if (rand == 2) {
					potions = ItemUtils.createStackedPotions(PotionEffectType.SPEED, 1, 50 * 20, 0,
					                                         "Splash Potion of Speed");
				} else if (rand == 3) {
					potions = ItemUtils.createStackedPotions(PotionEffectType.INCREASE_DAMAGE, 1, 50 * 20, 0,
					                                         "Splash Potion of Strength");
				} else {
					potions = ItemUtils.createStackedPotions(PotionEffectType.DAMAGE_RESISTANCE, 1, 50 * 20, 0,
					                                         "Splash Potion of Resistance");
				}
			}

			ItemUtils.addPotionEffect(potions, PotionInfo.HEALING);

			World world = Bukkit.getWorld(mPlayer.getWorld().getName());
			Location pos = (mPlayer.getLocation()).add(0, 2, 0);
			EntityUtils.spawnCustomSplashPotion(world, mPlayer, potions, pos);
		}
	}

}
