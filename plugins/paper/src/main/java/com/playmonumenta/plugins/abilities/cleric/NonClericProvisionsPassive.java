package com.playmonumenta.plugins.abilities.cleric;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

public class NonClericProvisionsPassive extends Ability {
	private static final int PROVISIONS_RANGE = 30;
	private static final float PROVISIONS_1_CHANCE = 0.2f;
	private static final float PROVISIONS_2_CHANCE = 0.4f;

	public NonClericProvisionsPassive(Plugin plugin, @Nullable Player player) {
		super(plugin, player, null);
	}

	@Override
	public boolean canUse(Player player) {
		return true;
	}

	public static boolean testRandomChance(Player player) {
		int level = 0;
		for (Player p : PlayerUtils.playersInRange(player.getLocation(), PROVISIONS_RANGE, true)) {
			Ability provisions = AbilityManager.getManager().getPlayerAbility(p, SacredProvisions.class);
			if (provisions != null) {
				int score = provisions.getAbilityScore();
				if (score == 2) {
					return FastUtils.RANDOM.nextDouble() < PROVISIONS_2_CHANCE;
				} else if (score > level) {
					level = score;
				}
			}
		}
		if (level == 1) {
			return FastUtils.RANDOM.nextDouble() < PROVISIONS_1_CHANCE;
		} else {
			return false;
		}
	}

	@Override
	public void playerItemConsumeEvent(PlayerItemConsumeEvent event) {
		if (mPlayer != null && testRandomChance(mPlayer)) {
			event.setReplacement(event.getItem());
			sacredProvisionsSound(mPlayer);
		}
	}

	@Override
	public void playerItemDamageEvent(PlayerItemDamageEvent event) {
		if (mPlayer != null && testRandomChance(mPlayer)) {
			event.setDamage(0);
		}
	}

	@Override
	public boolean playerShotArrowEvent(AbstractArrow arrow) {
		if (mPlayer != null && testRandomChance(mPlayer)) {
			AbilityUtils.refundArrow(mPlayer, arrow);
		}
		return true;
	}

	@Override
	public boolean playerThrewSplashPotionEvent(ThrownPotion potion) {
		if (mPlayer != null && testRandomChance(mPlayer)) {
			if (AbilityUtils.refundPotion(mPlayer, potion)) {
				sacredProvisionsSound(mPlayer);
			}
		}
		return true;
	}

	@Override
	public boolean playerThrewLingeringPotionEvent(ThrownPotion potion) {
		if (mPlayer != null && testRandomChance(mPlayer)) {
			if (AbilityUtils.refundPotion(mPlayer, potion)) {
				sacredProvisionsSound(mPlayer);
			}
		}
		return true;
	}

	public static void sacredProvisionsSound(Player player) {
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_COW_BELL, 0.65f, 2f);
	}

}
