package com.playmonumenta.plugins.abilities.warlock.tenebrist;

import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.classes.magic.AbilityCastEvent;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 * Eerie Eminence: Provides a debuff aura around the player with a radius of 6 / 8 depending
 * on the last spell you cast (the effect lingers for 4 seconds after leaving the aura):
 * Grasping Claws -> Slowness I
 * Consuming Flames -> Weakness I
 * Fractal Enervation -> Mining Fatigue I
 * Withering Gaze -> Wither I
 * At level 2, the aura also gives the opposite buff to other players.
 */

public class EerieEminence extends Ability {

	private static final double EERIE_1_RADIUS = 6;
	private static final double EERIE_2_RADIUS = 8;
	private static final int EERIE_EFFECT_LINGER_DURATION = 20 * 4;

	private PotionEffectType debuff = null;
	private PotionEffectType buff = null;

	public EerieEminence(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "EerieEminence";
	}

	@Override
	public boolean AbilityCastEvent(AbilityCastEvent event) {
		if (event.getAbility() == Spells.GRASPING_CLAWS) {
			debuff = PotionEffectType.SLOW;
			buff = PotionEffectType.SPEED;
		} else if (event.getAbility() == Spells.CONSUMING_FLAMES) {
			debuff = PotionEffectType.WEAKNESS;
			buff = PotionEffectType.INCREASE_DAMAGE;
		} else if (event.getAbility() == Spells.FRACTAL_ENERVATION) {
			debuff = PotionEffectType.SLOW_DIGGING;
			buff = PotionEffectType.FAST_DIGGING;
		} else if (event.getAbility() == Spells.WITHERING_GAZE) {
			debuff = PotionEffectType.WITHER;
			buff = PotionEffectType.REGENERATION;
		}

		return true;
	}

	@Override
	public void PeriodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		double radius = getAbilityScore() == 1 ? EERIE_1_RADIUS : EERIE_2_RADIUS;
		for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), radius)) {
			PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(debuff, EERIE_EFFECT_LINGER_DURATION, 0));
		}
		if (getAbilityScore() > 1) {
			for (Player player : PlayerUtils.getNearbyPlayers(mPlayer, radius, false)) {
				mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_OTHER,
						new PotionEffect(buff, EERIE_EFFECT_LINGER_DURATION, 0, true, false));
			}
		}
	}

}
