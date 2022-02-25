package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import javax.annotation.Nullable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.Collection;



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
	private static final double PERCENT_SPEED = 0.2;
	private static final String ATTR_NAME = "DodgingExtraSpeed";
	private static final int DODGING_COOLDOWN_1 = 12 * 20;
	private static final int DODGING_COOLDOWN_2 = 10 * 20;

	private int mTriggerTick = 0;

	public Dodging(Plugin plugin, @Nullable Player player) {
		super(plugin, player, "Dodging");
		mInfo.mLinkedSpell = ClassAbility.DODGING;
		mInfo.mScoreboardId = "Dodging";
		mInfo.mShorthandName = "Dg";
		mInfo.mDescriptions.add("Blocks an arrow, thrown potion, blaze fireball, or snowball that would have hit you. Cooldown: 12s.");
		mInfo.mDescriptions.add("The cooldown is reduced to 10 s. When this ability is triggered, you gain +20% Speed for 15s.");
		// NOTE: getAbilityScore() can only be used after the scoreboardId is set!
		mInfo.mCooldown = getAbilityScore() == 1 ? DODGING_COOLDOWN_1 : DODGING_COOLDOWN_2;
		// NOTE: This skill will get events even when it is on cooldown!
		mInfo.mIgnoreCooldown = true;
		mDisplayItem = new ItemStack(Material.SHIELD, 1);
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
	public void onHurt(DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		// See if we should dodge. If false, allow the event to proceed normally
		if (event.getType() == DamageType.PROJECTILE && !event.isBlocked() && dodge()) {
			event.setDamage(0);
			event.setCancelled(true);
		}
	}


	@Override
	public boolean playerHitByProjectileEvent(ProjectileHitEvent event) {
		if (mPlayer == null) {
			return true;
		}
		Projectile proj = event.getEntity();
		// See if we should dodge. If false, allow the event to proceed normally
		if (proj.getShooter() instanceof Player) {
			return true;
		}
		if (mPlayer.getActiveItem() != null && mPlayer.getActiveItem().getType() == Material.SHIELD) {
			return true;
		}
		if (!dodge()) {
			return true;
		}

		if (proj instanceof Arrow arrow) {
			arrow.setBasePotionData(new PotionData(PotionType.MUNDANE));
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
		if (mPlayer == null) {
			return false;
		}
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
		putOnCooldown();

		Location loc = mPlayer.getLocation().add(0, 1, 0);
		World world = mPlayer.getWorld();
		int dodging = getAbilityScore();
		if (dodging > 1) {
			mPlugin.mEffectManager.addEffect(mPlayer, ATTR_NAME,
					new PercentSpeed(DODGING_SPEED_EFFECT_DURATION, PERCENT_SPEED, ATTR_NAME));
			world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 20, 0.25, 0.45, 0.25, 0.15);
			world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 1.35f);
		}
		world.spawnParticle(Particle.SMOKE_NORMAL, loc, 90, 0.25, 0.45, 0.25, 0.1);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1, 2f);
		return true;
	}

}
