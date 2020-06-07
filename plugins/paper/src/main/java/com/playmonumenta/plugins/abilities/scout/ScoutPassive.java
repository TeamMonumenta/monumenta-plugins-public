package com.playmonumenta.plugins.abilities.scout;

import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * Scout Passive: 20% chance to not consume an arrow when shooting a non-infinity bow
 */

public class ScoutPassive extends Ability {

	private static final float PASSIVE_ARROW_SAVE = 0.20f;

	public ScoutPassive(Plugin plugin, World world, Player player) {
		super(plugin, world, player, null);
	}

	@Override
	public boolean canUse(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "Class") == 6;
	}

	@Override
	public boolean playerShotArrowEvent(Arrow arrow) {
		if (FastUtils.RANDOM.nextDouble() < PASSIVE_ARROW_SAVE) {
			mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.3f, 1.0f);
			AbilityUtils.refundArrow(mPlayer, arrow);
		}
		return true;
	}

}
