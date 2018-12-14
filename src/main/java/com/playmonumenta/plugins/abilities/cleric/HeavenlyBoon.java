package com.playmonumenta.plugins.abilities.cleric;

import java.util.Collection;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.PotionUtils.PotionInfo;

public class HeavenlyBoon extends Ability {

	private static final double HEAVENLY_BOON_1_CHANCE = 0.06;
	private static final double HEAVENLY_BOON_2_CHANCE = 0.1;
	private static final double HEAVENLY_BOON_RADIUS = 12;
	private static final double HEAVENLY_BOON_TRIGGER_INTENSITY = 0.3;

	public HeavenlyBoon(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 3;
		mInfo.specId = -1;
		mInfo.scoreboardId = "HeavenlyBoon";
	}

	@Override
	public void EntityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		LivingEntity killedEntity = event.getEntity();
		int heavenlyBoon = getAbilityScore();
		if (shouldGenDrops) {
			if (EntityUtils.isUndead(killedEntity)) {
				double chance = heavenlyBoon == 1 ? HEAVENLY_BOON_1_CHANCE : HEAVENLY_BOON_2_CHANCE;

				if (mRandom.nextDouble() < chance) {
					ItemStack potions;

					if (heavenlyBoon == 1) {
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
	}

	/*
	 * When a cleric is hit by a splash potion, distribute full durations of any positive effects to all
	 * nearby players. If a player gets a full-duration effect in this way, they are removed from the
	 * affectedEntities list so they don't get potion effects applied to them twice
	 */
	@Override
	public boolean PlayerSplashedByPotionEvent(Collection<LivingEntity> affectedEntities, ThrownPotion potion,
	                                           PotionSplashEvent event) {
		if (PotionUtils.hasNegativeEffects(potion.getEffects())) {
			// This potion is bad - don't do anything with it
			return true;
		}

		if (event.getIntensity(mPlayer) >= HEAVENLY_BOON_TRIGGER_INTENSITY) {
			/* If within range, apply full strength of all potion effects to all nearby players */

			for (Player p : PlayerUtils.getNearbyPlayers(mPlayer, HEAVENLY_BOON_RADIUS, true)) {
				// Don't buff players that have their class disabled
				if (p.getScoreboardTags().contains("disable_class")) {
					continue;
				}

				/* Apply full-strength effects to players within range */
				for (PotionEffect effect : potion.getEffects()) {
					PotionUtils.applyPotion(mPlugin, p, effect);
				}

				/* Remove this player from the "usual" application of potion effects */
				affectedEntities.remove(p);
			}
		}

		return true;
	}

}
