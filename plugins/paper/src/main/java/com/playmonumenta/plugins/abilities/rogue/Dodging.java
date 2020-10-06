package com.playmonumenta.plugins.abilities.rogue;

import java.util.Collection;
import java.util.EnumSet;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.enchantments.BaseAbilityEnchantment;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.EntityUtils;

public class Dodging extends Ability {

	public static class DodgingCooldownEnchantment extends BaseAbilityEnchantment {
		public DodgingCooldownEnchantment() {
			super("Dodging Cooldown", EnumSet.of(ItemSlot.OFFHAND));
		}
	}

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

	public Dodging(Plugin plugin, World world, Player player) {
		super(plugin, world, player, "Dodging");
		mInfo.mLinkedSpell = Spells.DODGING;
		mInfo.mScoreboardId = "Dodging";
		mInfo.mShorthandName = "Dg";
		mInfo.mDescriptions.add("Blocks an arrow or blaze fireball that would have hit you. Cooldown: 12s.");
		mInfo.mDescriptions.add("The cooldown is reduced to 10 s. When this ability is triggered, you gain 15 s of Speed I.");
		// NOTE: getAbilityScore() can only be used after the scoreboardId is set!
		mInfo.mCooldown = getAbilityScore() == 1 ? DODGING_COOLDOWN_1 : DODGING_COOLDOWN_2;
		// NOTE: This skill will get events even when it is on cooldown!
		mInfo.mIgnoreCooldown = true;
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
		if ((proj.getShooter() != null && proj.getShooter() instanceof Player) ||
				mPlayer.isBlocking() || EntityUtils.getRealFinalDamage(event) <= 0) {
			return true;
		}

		return !dodge();
	}


	@Override
	public boolean playerHitByProjectileEvent(ProjectileHitEvent event) {
		Projectile proj = event.getEntity();
		// See if we should dodge. If false, allow the event to proceed normally
		if ((proj.getShooter() instanceof Player) || mPlayer.isBlocking()) {
			return true;
		}
		if (!dodge()) {
			return true;
		}

		if (proj instanceof Arrow) {
			Arrow arrow = (Arrow) proj;
			arrow.setBasePotionData(null);
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
		return !dodge();
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
		if (mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), mInfo.mLinkedSpell)) {
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
		int cd = getAbilityScore() == 1 ? DODGING_COOLDOWN_1 : DODGING_COOLDOWN_2;
		mInfo.mCooldown = (int) DodgingCooldownEnchantment.getCooldown(mPlayer, cd, DodgingCooldownEnchantment.class);
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
