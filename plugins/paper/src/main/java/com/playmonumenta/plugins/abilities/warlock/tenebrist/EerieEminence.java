package com.playmonumenta.plugins.abilities.warlock.tenebrist;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.enchantments.Inferno;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.tracking.PlayerTracking;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;

public class EerieEminence extends Ability {

	private static final double EERIE_1_RADIUS = 6;
	private static final double EERIE_2_RADIUS = 8;
	private static final int EERIE_EFFECT_TIMER = 20 * 9;	// The linger still inherently exists for 1 second or so, bringing this up to essentially 10 seconds

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

	private final double mRadius;

	private final List<DebuffElement> mDebuffs = new ArrayList<>();

	public EerieEminence(Plugin plugin, Player player) {
		super(plugin, player, "Eerie Eminence");
		mInfo.mScoreboardId = "EerieEminence";
		mInfo.mShorthandName = "EE";
		mInfo.mDescriptions.add("You gain an AoE debuff aura around you that applies a level 1 debuff for every of the following four skills that you used in the last 10s. Grasping Claws > Slowness. Consuming Flames > Set mobs on Fire. Fractal Enervation > Mining Fatigue. Withering Gaze > Wither. The AoE affects all enemies in a 6 block radius.");
		mInfo.mDescriptions.add("The range is increased to 8. In addition it provides the opposite effect to players in range. Slowness > Speed. Set Fire > Extinguish Fire. Mining Fatigue > Haste. Wither > Regeneration.");
		mRadius = getAbilityScore() == 1 ? EERIE_1_RADIUS : EERIE_2_RADIUS;
	}

	@Override
	public boolean abilityCastEvent(AbilityCastEvent event) {
		if (event.getAbility() == Spells.GRASPING_CLAWS) {
			mDebuffs.add(new DebuffElement(PotionEffectType.SLOW, PotionEffectType.SPEED, EERIE_EFFECT_TIMER));
		} else if (event.getAbility() == Spells.CONSUMING_FLAMES) {
			mDebuffs.add(new DebuffElement(null, null, EERIE_EFFECT_TIMER));
		} else if (event.getAbility() == Spells.FRACTAL_ENERVATION) {
			mDebuffs.add(new DebuffElement(PotionEffectType.SLOW_DIGGING, PotionEffectType.FAST_DIGGING, EERIE_EFFECT_TIMER));
		} else if (event.getAbility() == Spells.WITHERING_GAZE) {
			mDebuffs.add(new DebuffElement(PotionEffectType.WITHER, PotionEffectType.REGENERATION, EERIE_EFFECT_TIMER));
		}

		return true;
	}

	@Override
	public void periodicTrigger(boolean fourHertz, boolean twoHertz, boolean oneSecond, int ticks) {
		if (fourHertz) {
			Iterator<DebuffElement> iter = mDebuffs.iterator();
			while (iter.hasNext()) {
				DebuffElement entry = iter.next();

				// Consuming Flames case
				if (entry.getDebuff() == null) {
					for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), mRadius)) {
						// Check that the mob is not on fire, that the mob can be set on fire, and that the mob won't be extinguished
						if (mob.getFireTicks() <= 0 && !Inferno.mobHasInferno(mPlugin, mob)
								&& (!EntityUtils.isFireResistant(mob) || PlayerTracking.getInstance().getPlayerCustomEnchantLevel(mPlayer, Inferno.class) > 0)
								&& mob.getLocation().getBlock().getType() != Material.WATER) {
							EntityUtils.applyFire(mPlugin, 80, mob, mPlayer);
						}
					}

					if (getAbilityScore() > 1) {
						for (Player player : PlayerUtils.playersInRange(mPlayer, mRadius, true)) {
							player.setFireTicks(0);
						}
					}
				} else {
					for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), mRadius)) {
						PotionUtils.applyPotion(mPlayer, mob, new PotionEffect(entry.getDebuff(), 30, 0));
					}

					if (getAbilityScore() > 1) {
						for (Player player : PlayerUtils.playersInRange(mPlayer, mRadius, true)) {
							if (entry.getBuff().equals(PotionEffectType.REGENERATION)) {
								/* Can't heal with regen, because regen only heals 1 health every 50 ticks.
								 * Instead, heal the same amount over the period (4 health over 10s)
								 * 0.1 * 10s * 4 ticks/s = 4 health
								 */
								PlayerUtils.healPlayer(player, 0.1d);
							} else {
								mPlugin.mPotionManager.addPotion(player, PotionID.ABILITY_OTHER,
																 new PotionEffect(entry.getBuff(), 30, 0, true, true));
							}
						}
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
