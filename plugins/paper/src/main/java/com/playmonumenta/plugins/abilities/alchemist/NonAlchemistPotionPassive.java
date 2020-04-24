package com.playmonumenta.plugins.abilities.alchemist;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker;
import com.playmonumenta.plugins.abilities.KillTriggeredAbilityTracker.KillTriggeredAbility;
import com.playmonumenta.plugins.abilities.alchemist.apothecary.InvigoratingOdor;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 * All players can use this ability! It is used to generate potions for nearby alchemists
 */
public class NonAlchemistPotionPassive extends Ability implements KillTriggeredAbility {

	private static final double POTION_CHANCE = 0.5;

	private final KillTriggeredAbilityTracker mTracker;

	public NonAlchemistPotionPassive(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, null);
		mTracker = new KillTriggeredAbilityTracker(this);
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
	public boolean livingEntityShotByPlayerEvent(Arrow arrow, LivingEntity damagee, EntityDamageByEntityEvent event) {
		mTracker.updateDamageDealtToBosses(event);
		return true;
	}

	@Override
	public void entityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		triggerOnKill(event.getEntity());
	}

	@Override
	public void triggerOnKill(LivingEntity mob) {
		Ability ap;
		for (Player player : PlayerUtils.playersInRange(mPlayer, 12, false)) {
			if ((ap = AbilityManager.getManager().getPlayerAbility(player, AlchemistPotions.class)) != null
					&& ap.canUse(player)) {
				// Nearby player has one of the alchemist skills - chance to give them a potion
				double chance = POTION_CHANCE;
				InvigoratingOdor io = (InvigoratingOdor) AbilityManager.getManager().getPlayerAbility(player, InvigoratingOdor.class);
				if (io != null) {
					chance += io.getPotionChanceBonus();
				}

				if (mRandom.nextDouble() < chance) {
					AbilityUtils.addAlchemistPotions(player, 1);
				}
			}
		}
	}

}
