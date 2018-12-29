package com.playmonumenta.plugins.abilities.alchemist;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 * All players can use this ability! It is used to generate potions for nearby
 * alchemists if they have Brutal or Gruesome Alchemy
 */
public class NonAlchemistPotionPassive extends Ability {

	public NonAlchemistPotionPassive(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
	}

	@Override
	public boolean canUse(Player player) {
		return true;
	}

	@Override
	public void EntityDeathEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		for (Player player : PlayerUtils.getNearbyPlayers(mPlayer, 12, false)) {
			if (AbilityManager.getManager().getPlayerAbility(mPlayer, GruesomeAlchemy.class) != null
			    || AbilityManager.getManager().getPlayerAbility(mPlayer, BrutalAlchemy.class) != null) {
				// Nearby player has one of the alchemist skills - chance to give them a potion
				if (mRandom.nextDouble() < 0.50) {
					AbilityUtils.addAlchemistPotions(player, 1);
				}
			}
		}
	}
}
