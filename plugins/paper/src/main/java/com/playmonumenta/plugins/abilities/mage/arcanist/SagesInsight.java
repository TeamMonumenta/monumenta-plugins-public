package com.playmonumenta.plugins.abilities.mage.arcanist;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.mage.ArcaneStrike;
import com.playmonumenta.plugins.abilities.mage.FrostNova;
import com.playmonumenta.plugins.abilities.mage.MagmaShield;
import com.playmonumenta.plugins.abilities.mage.ManaLance;
import com.playmonumenta.plugins.abilities.mage.PrismaticShield;
import com.playmonumenta.plugins.events.AbilityCastEvent;

/*
 * Sage's Insight: Whenever you cast a spell, reduce the
 * cooldown of all other spells by 5% / 10%.
 */
public class SagesInsight extends Ability {

	private static final double ARCANIST_1_COOLDOWN_REDUCTION_PERCENT = 0.05;
	private static final double ARCANIST_2_COOLDOWN_REDUCTION_PERCENT = 0.10;

	public SagesInsight(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "SagesInsight";
	}

	@Override
	public boolean AbilityCastEvent(AbilityCastEvent event) {
		Ability[] abilities = {
			AbilityManager.getManager().getPlayerAbility(mPlayer, ArcaneStrike.class),
			AbilityManager.getManager().getPlayerAbility(mPlayer, ManaLance.class),
			AbilityManager.getManager().getPlayerAbility(mPlayer, FrostNova.class),
			AbilityManager.getManager().getPlayerAbility(mPlayer, MagmaShield.class),
			AbilityManager.getManager().getPlayerAbility(mPlayer, PrismaticShield.class),
			AbilityManager.getManager().getPlayerAbility(mPlayer, FlashSword.class)
		};

		double cooldownReductionPercent = getAbilityScore() == 1 ? ARCANIST_1_COOLDOWN_REDUCTION_PERCENT : ARCANIST_2_COOLDOWN_REDUCTION_PERCENT;
		for (int i = 0; i < abilities.length; i++) {
			if (abilities[i] != null) {
				int cooldownReduction = (int)(abilities[i].getInfo().cooldown * cooldownReductionPercent);
				mPlugin.mTimers.UpdateCooldown(mPlayer, abilities[i].getInfo().linkedSpell, cooldownReduction);
			}
		}
		return true;
	}

}
