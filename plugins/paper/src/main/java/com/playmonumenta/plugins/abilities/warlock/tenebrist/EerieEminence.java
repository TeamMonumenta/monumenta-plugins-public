package com.playmonumenta.plugins.abilities.warlock.tenebrist;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

	private static class DebuffElement {
		protected final PotionEffectType mDebuff;
		protected final PotionEffectType mBuff;
		protected int mDuration;

		protected DebuffElement(PotionEffectType debuff, PotionEffectType buff, int duration) {
			mDebuff = debuff;
			mBuff = buff;
			mDuration = duration;
		}

		protected PotionEffectType getDebuff() {
			return mDebuff;
		}

		protected PotionEffectType getBuff() {
			return mBuff;
		}

		protected int getDuration() {
			return mDuration;
		}

		protected void setDuration(int duration) {
			mDuration = duration;
		}
	}

	private List<DebuffElement> debuffs = new ArrayList<DebuffElement>();

	public EerieEminence(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "EerieEminence";
	}

	@Override
	public boolean AbilityCastEvent(AbilityCastEvent event) {
		if (event.getAbility() == Spells.GRASPING_CLAWS) {
			debuffs.add(new DebuffElement(PotionEffectType.SLOW, PotionEffectType.SPEED, EERIE_EFFECT_TIMER));
		} else if (event.getAbility() == Spells.CONSUMING_FLAMES) {
			debuffs.add(new DebuffElement(PotionEffectType.WEAKNESS, PotionEffectType.INCREASE_DAMAGE, EERIE_EFFECT_TIMER));
		} else if (event.getAbility() == Spells.FRACTAL_ENERVATION) {
			debuffs.add(new DebuffElement(PotionEffectType.SLOW_DIGGING, PotionEffectType.FAST_DIGGING, EERIE_EFFECT_TIMER));
		} else if (event.getAbility() == Spells.WITHERING_GAZE) {
			debuffs.add(new DebuffElement(PotionEffectType.WITHER, PotionEffectType.REGENERATION, EERIE_EFFECT_TIMER));
		}

		return true;
	}

	@Override
	public void PeriodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (fourHertz) {
			double radius = getAbilityScore() == 1 ? EERIE_1_RADIUS : EERIE_2_RADIUS;

			Iterator<DebuffElement> iter = debuffs.iterator();
			while (iter.hasNext()) {
				DebuffElement entry = iter.next();
				for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), radius)) {
					PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(entry.getDebuff(), EERIE_EFFECT_LINGER_DURATION, 0));
				}
				if (getAbilityScore() > 1) {
					for (Player player : PlayerUtils.playersInRange(mPlayer, radius, true)) {
						mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_OTHER,
						                                 new PotionEffect(entry.getBuff(), EERIE_EFFECT_LINGER_DURATION, 0, true, true));
					}
				}
				// 5 ticks because it triggers on four hertz.
				int timer = entry.getDuration() - 5;
				if (timer <= 0) {
					iter.remove();
				} else {
					entry.setDuration(timer);
				}
			}

		}
	}

}
