package com.playmonumenta.plugins.abilities.mage.arcanist;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.magic.AbilityCastEvent;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/*
 * Sage’s Insight: Whenever you cast a spell, reduce the
 * cooldown of all other spells by 1. At level 2, taking
 * damage reduces the cooldown of your spells by 1 second
 * as well.
 */
public class SagesInsight extends Ability {

	private static final int ARCANIST_INSIGHT = (int)(1 * 20);

	public SagesInsight(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "SagesInsight";
	}

	@Override
	public boolean AbilityCastEvent(AbilityCastEvent event) {
		mPlugin.mTimers.UpdateCooldowns(mPlayer, ARCANIST_INSIGHT);
		return true;
	}

	@Override
	public boolean PlayerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		if (getAbilityScore() > 1) {
			mPlugin.mTimers.UpdateCooldowns(mPlayer, ARCANIST_INSIGHT);
		}
		return true;
	}
}
