package com.playmonumenta.plugins.abilities.cleric;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LingeringPotion;
import org.bukkit.entity.SplashPotion;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * All players can use this ability! It is used to generate potions for nearby
 * alchemists if they have Brutal or Gruesome Alchemy
 */
public class NonClericProvisionsPassive extends Ability {
	private static final int PROVISIONS_RANGE = 30;
	private static final float PROVISIONS_1_CHANCE = 0.2f;
	private static final float PROVISIONS_2_CHANCE = 0.4f;

	public NonClericProvisionsPassive(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
	}

	@Override
	public boolean canUse(Player player) {
		return true;
	}

	private boolean testRandomChance() {
		int level = 0;
		for (Player player : PlayerUtils.getNearbyPlayers(mPlayer, PROVISIONS_RANGE, true)) {
			Ability provisions = AbilityManager.getManager().getPlayerAbility(player, SacredProvisions.class);
			if (provisions != null) {
				Integer score = provisions.getAbilityScore();
				if (score == 2) {
					return mRandom.nextFloat() < PROVISIONS_2_CHANCE;
				} else if (score > level) {
					level = score;
				}
			}
		}
		if (level == 1) {
			return mRandom.nextFloat() < PROVISIONS_1_CHANCE;
		} else {
			return false;
		}
	}

	@Override
	public void PlayerItemConsumeEvent(PlayerItemConsumeEvent event) {
		if (testRandomChance()) {
			event.setReplacement(event.getItem());
		}
	}

	@Override
	public void PlayerItemDamageEvent(PlayerItemDamageEvent event) {
		if (testRandomChance()) {
			event.setDamage(0);
		}
	}

	@Override
	public boolean PlayerShotArrowEvent(Arrow arrow) {
		if (testRandomChance()) {
			AbilityUtils.refundArrow(mPlayer, arrow);
		}
		return true;
	}

	@Override
	public boolean PlayerThrewSplashPotionEvent(SplashPotion potion) {
		if (testRandomChance()) {
			AbilityUtils.refundPotion(mPlayer, potion);
		}
		return true;
	}

	@Override
	public boolean PlayerThrewLingeringPotionEvent(LingeringPotion potion) {
		if (testRandomChance()) {
			AbilityUtils.refundPotion(mPlayer, potion);
		}
		return true;
	}

}
