package com.playmonumenta.plugins.abilities.warlock;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.warlock.reaper.DarkPact;
import com.playmonumenta.plugins.abilities.warlock.reaper.JudgementChain;
import com.playmonumenta.plugins.abilities.warlock.reaper.VoodooBonds;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.HauntingShades;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.UmbralWail;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.WitheringGaze;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.effects.PercentKnockbackResist;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class PhlegmaticResolve extends Ability {
	private static final String PERCENT_DAMAGE_RESIST_EFFECT_NAME = "PhlegmaticPercentDamageResistEffect";
	private static final String KNOCKBACK_RESIST_EFFECT_NAME = "PhlegmaticPercentDamageResistEffect";
	private static final double PERCENT_DAMAGE_RESIST_1 = -0.03;
	private static final double PERCENT_DAMAGE_RESIST_2 = -0.03;
	private static final double PERCENT_KNOCKBACK_RESIST = 0.1;
	private static final int RADIUS = 7;

	private final double mPercentDamageResist;


	public PhlegmaticResolve(Plugin plugin, Player player) {
		super(plugin, player, "Phlegmatic Resolve");
		mInfo.mScoreboardId = "Phlegmatic";
		mInfo.mShorthandName = "PR";
		mInfo.mDescriptions.add("For each spell on cooldown, gain +2% Damage Reduction and +1 Knockback Resistance.");
		mInfo.mDescriptions.add("Increase to +3% Damage Reduction per spell on cooldown, and players within 7 blocks are given 33% of your bonuses. (Does not stack with mulitple Warlocks.)");
		mPercentDamageResist = getAbilityScore() == 1 ? PERCENT_DAMAGE_RESIST_1 : PERCENT_DAMAGE_RESIST_2;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		//Triggers four times a second
		// *TO DO* - move into constructor
		Ability[] abilities = new Ability[12];
		abilities[0] = AbilityManager.getManager().getPlayerAbility(mPlayer, AmplifyingHex.class);
		abilities[1] = AbilityManager.getManager().getPlayerAbility(mPlayer, CholericFlames.class);
		abilities[2] = AbilityManager.getManager().getPlayerAbility(mPlayer, GraspingClaws.class);
		abilities[3] = AbilityManager.getManager().getPlayerAbility(mPlayer, SoulRend.class);
		abilities[4] = AbilityManager.getManager().getPlayerAbility(mPlayer, SanguineHarvest.class);
		abilities[5] = AbilityManager.getManager().getPlayerAbility(mPlayer, MelancholicLament.class);
		abilities[6] = AbilityManager.getManager().getPlayerAbility(mPlayer, DarkPact.class);
		abilities[7] = AbilityManager.getManager().getPlayerAbility(mPlayer, VoodooBonds.class);
		abilities[8] = AbilityManager.getManager().getPlayerAbility(mPlayer, JudgementChain.class);
		abilities[9] = AbilityManager.getManager().getPlayerAbility(mPlayer, HauntingShades.class);
		abilities[10] = AbilityManager.getManager().getPlayerAbility(mPlayer, WitheringGaze.class);
		abilities[11] = AbilityManager.getManager().getPlayerAbility(mPlayer, UmbralWail.class);

		int cooldowns = 0;
		for (Ability ability : abilities) {
			if (ability != null && mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), ability.getInfo().mLinkedSpell)) {
				cooldowns++;
			}
		}
		mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_RESIST_EFFECT_NAME, new PercentDamageReceived(6, mPercentDamageResist * cooldowns));
		mPlugin.mEffectManager.addEffect(mPlayer, KNOCKBACK_RESIST_EFFECT_NAME, new PercentKnockbackResist(6, PERCENT_KNOCKBACK_RESIST * cooldowns, KNOCKBACK_RESIST_EFFECT_NAME));

		if (getAbilityScore() > 1) {
			for (Player p : PlayerUtils.otherPlayersInRange(mPlayer, RADIUS, true)) {
				mPlugin.mEffectManager.addEffect(p, PERCENT_DAMAGE_RESIST_EFFECT_NAME, new PercentDamageReceived(6, mPercentDamageResist * cooldowns / 3.0));
				mPlugin.mEffectManager.addEffect(p, KNOCKBACK_RESIST_EFFECT_NAME, new PercentKnockbackResist(6, PERCENT_KNOCKBACK_RESIST * cooldowns / 3.0, KNOCKBACK_RESIST_EFFECT_NAME));
			}
		}
	}
}
