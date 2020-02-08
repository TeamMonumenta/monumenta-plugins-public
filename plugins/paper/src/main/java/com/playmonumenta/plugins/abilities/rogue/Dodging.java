package com.playmonumenta.plugins.abilities.rogue;

import java.util.Collection;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.TippedArrow;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;

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

	/*
	 * Debug findings:
	 * ProjectileHitEvent occurs before EntityDamageByEntityEvent
	 * Tipped Arrows apply after the ProjectileHitEvent is called, meaning we can remove their effects there
	 */

	private static final int DODGING_SPEED_EFFECT_DURATION = 15 * 20;
	private static final int DODGING_SPEED_EFFECT_LEVEL = 0;
	private static final int DODGING_COOLDOWN_1 = 12 * 20;
	private static final int DODGING_COOLDOWN_2 = 10 * 20;

	private int mTriggerTick = 0;

	public Dodging(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.linkedSpell = Spells.DODGING;
		mInfo.scoreboardId = "Dodging";
		// NOTE: getAbilityScore() can only be used after the scoreboardId is set!
		mInfo.cooldown = getAbilityScore() == 1 ? DODGING_COOLDOWN_1 : DODGING_COOLDOWN_2;
		// NOTE: This skill will get events even when it is on cooldown!
		mInfo.ignoreCooldown = true;
	}

	@Override
	public boolean playerCombustByEntityEvent(EntityCombustByEntityEvent event) {
		// Don't proc on Fire Aspect
		if (!(event.getCombuster() instanceof Projectile)) {
			return true;
		}

		// See if we should dodge. If false, allow the event to proceed normally
		if (!dodge()) {
			return true;
		}
		event.setDuration(0);
		return false;
	}


	@Override
	public boolean playerDamagedByProjectileEvent(EntityDamageByEntityEvent event) {
		// See if we should dodge. If false, allow the event to proceed normally
		Projectile proj = (Projectile) event.getDamager();
		if ((proj.getShooter() != null && proj.getShooter() instanceof Player) || EntityUtils.getRealFinalDamage(event) <= 0) {
			return true;
		}

		if (!dodge()) {
			return true;
		}
		return false;
	}


	@Override
	public boolean playerHitByProjectileEvent(ProjectileHitEvent event) {
		// See if we should dodge. If false, allow the event to proceed normally
		if ((event.getEntity().getShooter() instanceof Player) || mPlayer.isBlocking()) {
			return true;
		}
		if (!dodge()) {
			return true;
		}

		if (event.getEntity() instanceof TippedArrow) {
			TippedArrow arrow = (TippedArrow) event.getEntity();
			PotionData data = new PotionData(PotionType.MUNDANE);
			arrow.setBasePotionData(data);
			arrow.clearCustomEffects();
		}
		return true;
	}

	@Override
	public boolean playerSplashedByPotionEvent(Collection<LivingEntity> affectedEntities,
	                                           ThrownPotion potion, PotionSplashEvent event) {
		if (!(event.getEntity().getShooter() instanceof LivingEntity) || event.getEntity().getShooter() instanceof Player) {
			return true;
		}
		if (!dodge()) {
			return true;
		}
		return false;
	}

	private boolean dodge() {
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

		Location loc = mPlayer.getLocation().add(0, 1, 0);
		int dodging = getAbilityScore();
		if (dodging > 1) {
			mPlugin.mPotionManager.addPotion(mPlayer, PotionID.ABILITY_SELF,
			                                 new PotionEffect(PotionEffectType.SPEED,
			                                                  DODGING_SPEED_EFFECT_DURATION,
			                                                  DODGING_SPEED_EFFECT_LEVEL,
			                                                  true, true));
			mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 20, 0.25, 0.45, 0.25, 0.15);
			mWorld.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 1.35f);
		}
		mWorld.spawnParticle(Particle.SMOKE_NORMAL, loc, 90, 0.25, 0.45, 0.25, 0.1);
		mWorld.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 2f);
		return true;
	}
}
