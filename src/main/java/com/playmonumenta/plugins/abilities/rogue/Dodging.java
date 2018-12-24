package com.playmonumenta.plugins.abilities.rogue;

import java.util.Iterator;
import java.util.Random;

import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TippedArrow;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.managers.potion.PotionManager.PotionID;

public class Dodging extends Ability {
	/*
	 * This skill is a freaking nightmare because it spans two different events.
	 *
	 * Because of the way the ability system works, one event triggers (which puts
	 * it on cooldown), then the other event is missed because the skill is on
	 * cooldown...
	 *
	 * So this skill has mInfo.ignoreCooldown = true, meaning the events will always
	 * be triggered here, even when the skill is on cooldown. It must check itself
	 * that cooldown is active and behave accordingly.
	 */

	private static final int DODGING_SPEED_EFFECT_DURATION = 15 * 20;
	private static final int DODGING_SPEED_EFFECT_LEVEL = 0;
	private static final int DODGING_COOLDOWN_1 = 12 * 20;
	private static final int DODGING_COOLDOWN_2 = 10 * 20;

	private int mTriggerTick = 0;

	public Dodging(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.classId = 4;
		mInfo.specId = -1;
		mInfo.linkedSpell = Spells.DODGING;
		mInfo.scoreboardId = "Dodging";
		// NOTE: getAbilityScore() can only be used after the scoreboardId is set!
		mInfo.cooldown = getAbilityScore() == 1 ? DODGING_COOLDOWN_1 : DODGING_COOLDOWN_2;
		// NOTE: This skill will get events even when it is on cooldown!
		mInfo.ignoreCooldown = true;
	}

	@Override
	public boolean PlayerCombustByEntityEvent(EntityCombustByEntityEvent event) {
		// See if we should dodge. If false, allow the event to proceed normally
		if (!_dodge()) {
			return true;
		}

		return false;
	}

	@Override
	public boolean PlayerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		// See if we should dodge. If false, allow the event to proceed normally
		if (!_dodge()) {
			return true;
		}

		EntityType type = event.getDamager().getType();
		Projectile damager = (Projectile) event.getDamager();

		// Remove effects from tipped arrows
		// TODO: This is the same code as for removing from shields, should probably be
		// a utility function
		if (type == EntityType.TIPPED_ARROW) {
			TippedArrow arrow = (TippedArrow)damager;
			PotionData data = new PotionData(PotionType.AWKWARD);
			arrow.setBasePotionData(data);

			if (arrow.hasCustomEffects()) {
				Iterator<PotionEffect> effectIter = arrow.getCustomEffects().iterator();
				while (effectIter.hasNext()) {
					PotionEffect effect = effectIter.next();
					arrow.removeCustomEffect(effect.getType());
				}
			}
		}

		return false;
	}

	private boolean _dodge() {
		if (mTriggerTick == mPlayer.getTicksLived()) {
			// Dodging was activated this tick - allow it
			return true;
		}

		/*
		 * Must check with cooldown timers directly because isAbilityOnCooldown always returns
		 * false (because ignoreCooldown is true)
		 */
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.linkedSpell)) {
			/*
			 * This ability is actually on cooldown (and was not triggered this tick)
			 * Don't process dodging
			 */
			return false;

		}

		/*
		 * Make note of which tick this triggered on so that any other event that triggers this
		 * tick will also be dodged
		 */
		mTriggerTick = mPlayer.getTicksLived();
		putOnCooldown();

		int dodging = getAbilityScore();
		if (dodging > 1) {
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.SPEED,
			                                                  DODGING_SPEED_EFFECT_DURATION,
			                                                  DODGING_SPEED_EFFECT_LEVEL,
			                                                  true, false));
			mWorld.playSound(mPlayer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 2.0f, 0.5f);
		}
		mWorld.playSound(mPlayer.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 1.5f);

		return true;
	}
}
