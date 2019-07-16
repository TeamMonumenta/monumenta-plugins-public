package com.playmonumenta.plugins.abilities.warlock.tenebrist;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

/*
 * Eerie Eminence: Provides a debuff aura around the player with a radius of 6 / 8 depending
 * on the spells you cast within the last 10 seconds(the effect lingers for 4 seconds after leaving the aura):
 * Grasping Claws -> Slowness I
 * Consuming Flames -> Weakness I
 * Fractal Enervation -> Mining Fatigue I
 * Withering Gaze -> Wither I
 * At level 2, the aura also gives the opposite buff to nearby players (including self).
 */

public class EerieEminence extends Ability {

	private static final double EERIE_1_RADIUS = 6;
	private static final double EERIE_2_RADIUS = 8;
	private static final int EERIE_EFFECT_LINGER_DURATION = 20 * 4;
	private static final int EERIE_EFFECT_TIMER = 20 * 10;

	private Map<List<PotionEffectType>, Integer> debuffs = new HashMap<List<PotionEffectType>, Integer>() ;

	public EerieEminence(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "EerieEminence";
	}

	@Override
	public boolean AbilityCastEvent(AbilityCastEvent event) {
		if (event.getAbility() == Spells.GRASPING_CLAWS) {
			debuffs.put(Arrays.asList(PotionEffectType.SLOW,PotionEffectType.SPEED), EERIE_EFFECT_TIMER);
		} else if (event.getAbility() == Spells.CONSUMING_FLAMES) {
			debuffs.put(Arrays.asList(PotionEffectType.WEAKNESS,PotionEffectType.INCREASE_DAMAGE), EERIE_EFFECT_TIMER);
		} else if (event.getAbility() == Spells.FRACTAL_ENERVATION) {
			debuffs.put(Arrays.asList(PotionEffectType.SLOW_DIGGING,PotionEffectType.FAST_DIGGING), EERIE_EFFECT_TIMER);
		} else if (event.getAbility() == Spells.WITHERING_GAZE) {
			debuffs.put(Arrays.asList(PotionEffectType.WITHER,PotionEffectType.REGENERATION), EERIE_EFFECT_TIMER);
		}

		return true;
	}

	@Override
	public void PeriodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (fourHertz) {
			double radius = getAbilityScore() == 1 ? EERIE_1_RADIUS : EERIE_2_RADIUS;

			Iterator<Map.Entry<List<PotionEffectType>, Integer>> iter = debuffs.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<List<PotionEffectType>, Integer> entry = iter.next();
				for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), radius)) {
					PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(entry.getKey().get(0), EERIE_EFFECT_LINGER_DURATION, 0));
				}
				if (getAbilityScore() > 1) {
					for (Player player : PlayerUtils.getNearbyPlayers(mPlayer, radius, true)) {
						mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_OTHER,
						                                 new PotionEffect(entry.getKey().get(1), EERIE_EFFECT_LINGER_DURATION, 0, true, false));
					}
				}
				// 5 ticks because it triggers on four hertz.
				int timer = entry.getValue() - 5;
				if (timer <= 0) {
					iter.remove();
				} else {
					entry.setValue(timer);
				}
			}

		}
	}

}
