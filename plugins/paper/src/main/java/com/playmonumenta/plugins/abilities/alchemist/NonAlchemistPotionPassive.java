package com.playmonumenta.plugins.abilities.alchemist;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.HashSet;
import java.util.Set;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker.KillTriggeredAbility;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.scriptedquests.utils.ScoreboardUtils;

/*
 * All players can use this ability! It is used to generate potions for nearby alchemists
 */
public class NonAlchemistPotionPassive extends Ability implements KillTriggeredAbility {

	private final KillTriggeredAbilityTracker mTracker;
	private double mPotionQuantity = 0;
	//used to store decimal amount of potions

	public NonAlchemistPotionPassive(Plugin plugin, Player player) {
		super(plugin, player, null);
		mTracker = new KillTriggeredAbilityTracker(this);
	}

	@Override
	public boolean playerThrewSplashPotionEvent(ThrownPotion potion) {
		if (ScoreboardUtils.getScoreboardValue(mPlayer, "Class") != 5 && ItemUtils.isAlchemistItem(mPlayer.getInventory().getItemInMainHand())) {
			potion.remove();
		}

		return true;
	}

	@Override
	public boolean canUse(Player player) {
		return true;
	}

	@Override
	public boolean livingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		mTracker.updateDamageDealtToBosses(event);
		return true;
	}

	@Override
	public boolean livingEntityShotByPlayerEvent(Projectile proj, LivingEntity damagee, EntityDamageByEntityEvent event) {
		mTracker.updateDamageDealtToBosses(event);
		return true;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		triggerOnKill(event.getEntity());
	}

	@Override
	public void triggerOnKill(LivingEntity mob) {
		if (mob.hasMetadata(AlchemistPotions.POTION_METADATA_PLAYER_NAME)) {
			double mobHealt = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
			if (!ServerProperties.getClassSpecializationsEnabled()) {
				//we are on R1
				mPotionQuantity += EntityUtils.isBoss(mob) || EntityUtils.isElite(mob) ? (mobHealt / AlchemistPotions.BOSS_MOB_HEALTH_PER_POTION) : (mobHealt / AlchemistPotions.NORMAL_MOB_HEALTH_PER_POTION);
			} else {
				//we are on R2
				mPotionQuantity += EntityUtils.isBoss(mob) || EntityUtils.isElite(mob) ? (mobHealt / (AlchemistPotions.BOSS_MOB_HEALTH_PER_POTION * 2)) : (mobHealt / (AlchemistPotions.NORMAL_MOB_HEALTH_PER_POTION * 2));
				//the value are the same of R1 but just multiply by 30/60 -> 60/120
			}
			int quantity = (int) mPotionQuantity;
			mPotionQuantity -= quantity;

			Set<String> names = null;
			Object obj = mob.getMetadata(AlchemistPotions.POTION_METADATA_PLAYER_NAME).get(0).value();
			if (obj instanceof HashSet) {
				names = ((HashSet<String>) obj);
			}

			if (names != null) {
				for (String playerName : names) {
					Player targetAlchemist = Bukkit.getPlayer(playerName);
					Ability ap;

					if (
						targetAlchemist != null
					&& (ap = AbilityManager.getManager().getPlayerAbility(targetAlchemist, AlchemistPotions.class)) != null
					&& ap.canUse(targetAlchemist)) {
						((AlchemistPotions) ap).incrementChargeByQuantity(quantity + 1);
					}
				}
			}
		}
	}

}
