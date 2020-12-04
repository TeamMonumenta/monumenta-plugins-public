package com.playmonumenta.plugins.abilities.warlock;

import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.warlock.reaper.DarkPact;
import com.playmonumenta.plugins.abilities.warlock.reaper.DeathsTouch;
import com.playmonumenta.plugins.abilities.warlock.reaper.GhoulishTaunt;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.FractalEnervation;
import com.playmonumenta.plugins.abilities.warlock.tenebrist.WitheringGaze;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class BlasphemousAura extends Ability {
	private static final String PERCENT_DAMAGE_RESIST_EFFECT_NAME = "BlasphemousAuraPercentDamageResistEffect";
	private static final double PERCENT_DAMAGE_RESIST = -0.03;
	private static final double PERCENT_HEAL = 0.05;



	public BlasphemousAura(Plugin plugin, Player player) {
		super(plugin, player, "Blasphemous Aura");
		mInfo.mScoreboardId = "BlasphemousAura";
		mInfo.mShorthandName = "BA";
		mInfo.mDescriptions.add("The warlock gains +3% damage reduction for every ability they have on cooldown lasting until skills come off cooldown.");
		mInfo.mDescriptions.add("You also heal for 5% of your max health when an enemy dies within 8 blocks of you.");
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		//Triggers four times a second
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

	@Override
	public void entityDeathRadiusEvent(EntityDeathEvent event, boolean shouldGenDrops) {
		mPlayer.getWorld().spawnParticle(Particle.CRIMSON_SPORE, mPlayer.getLocation().add(0, 1, 0), 9, 0.35, 0.45, 0.35, 0.001);

		if (getAbilityScore() > 1) {
			PlayerUtils.healPlayer(mPlayer, PERCENT_HEAL * mPlayer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
		}
	}

	@Override
	public double entityDeathRadius() {
		return 8;
	}
}
