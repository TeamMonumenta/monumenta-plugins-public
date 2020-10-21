package com.playmonumenta.plugins.abilities.warlock;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.warlock.reaper.DarkPact;
import com.playmonumenta.plugins.abilities.warlock.reaper.DeathsTouch;
import com.playmonumenta.plugins.abilities.warlock.reaper.GhoulishTaunt;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.FractalEnervation;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.WitheringGaze;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.CustomDamageEvent;
import com.playmonumenta.plugins.utils.PotionUtils;

public class BlasphemousAura extends Ability {
	private static final int VULNERABILITY_AMPLIFIER = 2;
	private static final int VULNERABILITY_DURATION = 20 * 5;
	private static final String PERCENT_DAMAGE_RESIST_EFFECT_NAME = "BlasphemousAuraPercentDamageResistEffect";
	private static final double PERCENT_DAMAGE_RESIST = -0.03;


	public BlasphemousAura(Plugin plugin, Player player) {
		super(plugin, player, "Blasphemous Aura");
		mInfo.mScoreboardId = "BlasphemousAura";
		mInfo.mShorthandName = "BA";
		mInfo.mDescriptions.add("Enemies you damage with an ability are afflicted with 15% vulnerability for 5 seconds.");
		mInfo.mDescriptions.add("The warlock gains +3% damage reduction for every ability they have on cooldown lasting until skills come off cooldown.");
	}

	@Override
	public void playerDealtCustomDamageEvent(CustomDamageEvent event) {
		PotionUtils.applyPotion(mPlayer, event.getDamaged(), new PotionEffect(PotionEffectType.UNLUCK, VULNERABILITY_DURATION, VULNERABILITY_AMPLIFIER, false, true));
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		//Triggers four times a second
		if (getAbilityScore() > 1) {
			Ability[] abilities = new Ability[10];
			abilities[0] = AbilityManager.getManager().getPlayerAbility(mPlayer, AmplifyingHex.class);
			abilities[1] = AbilityManager.getManager().getPlayerAbility(mPlayer, ConsumingFlames.class);
			abilities[2] = AbilityManager.getManager().getPlayerAbility(mPlayer, GraspingClaws.class);
			abilities[3] = AbilityManager.getManager().getPlayerAbility(mPlayer, SoulRend.class);
			abilities[4] = AbilityManager.getManager().getPlayerAbility(mPlayer, Exorcism.class);
			abilities[5] = AbilityManager.getManager().getPlayerAbility(mPlayer, DarkPact.class);
			abilities[6] = AbilityManager.getManager().getPlayerAbility(mPlayer, GhoulishTaunt.class);
			abilities[7] = AbilityManager.getManager().getPlayerAbility(mPlayer, DeathsTouch.class);
			abilities[8] = AbilityManager.getManager().getPlayerAbility(mPlayer, FractalEnervation.class);
			abilities[9] = AbilityManager.getManager().getPlayerAbility(mPlayer, WitheringGaze.class);

			int cooldowns = 0;
			for (Ability ability : abilities) {
				if (ability != null && mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), ability.getInfo().mLinkedSpell)) {
					cooldowns++;
				}
			}
			mPlugin.mEffectManager.addEffect(mPlayer, PERCENT_DAMAGE_RESIST_EFFECT_NAME, new PercentDamageReceived(6, PERCENT_DAMAGE_RESIST * cooldowns));
		}
	}
}
