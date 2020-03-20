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
 * cooldown of all other spells by 5% / 10%, capped at
 * 0.5 / 1 second.
 */
public class SagesInsight extends Ability {

	private static final double ARCANIST_1_COOLDOWN_REDUCTION_PERCENT = 0.05;
	private static final double ARCANIST_2_COOLDOWN_REDUCTION_PERCENT = 0.10;
	private static final int ARCANIST_1_COOLDOWN_REDUCTION_CAP = 10;
	private static final int ARCANIST_2_COOLDOWN_REDUCTION_CAP = 20;

	private final double mCooldownReductionPercent;
	private final int mCooldownReductionCap;

	public SagesInsight(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player, "Sage's Insight");
		mInfo.scoreboardId = "SagesInsight";
		mInfo.mShorthandName = "SI";
		mInfo.mDescriptions.add("Whenever the user casts a spell the cooldowns of other equipped spells are reduced by 5%, capped at 0.5 seconds.");
		mInfo.mDescriptions.add("Cooldown reduction upon casting a spell is increased to 10%, and the cap is increased to 1 second.");

		mCooldownReductionPercent = getAbilityScore() == 1 ? ARCANIST_1_COOLDOWN_REDUCTION_PERCENT : ARCANIST_2_COOLDOWN_REDUCTION_PERCENT;
		mCooldownReductionCap = getAbilityScore() == 1 ? ARCANIST_1_COOLDOWN_REDUCTION_CAP : ARCANIST_2_COOLDOWN_REDUCTION_CAP;
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		Ability[] abilities = {
			AbilityManager.getManager().getPlayerAbility(mPlayer, ArcaneStrike.class),
			AbilityManager.getManager().getPlayerAbility(mPlayer, ManaLance.class),
			AbilityManager.getManager().getPlayerAbility(mPlayer, FrostNova.class),
			AbilityManager.getManager().getPlayerAbility(mPlayer, MagmaShield.class),
			AbilityManager.getManager().getPlayerAbility(mPlayer, PrismaticShield.class),
			AbilityManager.getManager().getPlayerAbility(mPlayer, FlashSword.class)
		};

		for (int i = 0; i < abilities.length; i++) {
			if (abilities[i] != null) {
				mPlugin.mTimers.updateCooldown(mPlayer, abilities[i].getInfo().linkedSpell,
						(int)(Math.min(mCooldownReductionCap, abilities[i].getInfo().cooldown * mCooldownReductionPercent)));
			}
		}

		return true;
	}

}
