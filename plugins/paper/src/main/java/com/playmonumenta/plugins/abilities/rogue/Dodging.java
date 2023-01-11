package com.playmonumenta.plugins.abilities.rogue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import java.util.Collection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractArrow;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

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
	private static final double ENHANCEMENT_DAMAGE = 3.0;

	public static final String CHARM_COOLDOWN = "Dodging Cooldown";
	public static final String CHARM_SPEED = "Dodging Speed Amplifier";

	public static final AbilityInfo<Dodging> INFO =
		new AbilityInfo<>(Dodging.class, "Dodging", Dodging::new)
			.linkedSpell(ClassAbility.DODGING)
			.scoreboardId("Dodging")
			.shorthandName("Dg")
			.descriptions(
				String.format("Blocks an arrow, thrown potion, blaze fireball, or snowball that would have hit you. Cooldown: %ss.",
					DODGING_COOLDOWN_1 / 20),
				String.format("The cooldown is reduced to %ss. When this ability is triggered, you gain +%s%% Speed for %ss.",
					DODGING_COOLDOWN_2 / 20,
					(int) (PERCENT_SPEED * 100),
					DODGING_SPEED_EFFECT_DURATION / 20),
				"The projectile you dodged is now reflected back to the enemy at 3x damage.")
			.cooldown(DODGING_COOLDOWN_1, DODGING_COOLDOWN_2, CHARM_COOLDOWN)
			.displayItem(new ItemStack(Material.SHIELD, 1));

	private int mTriggerTick = 0;

	public Dodging(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		// NOTE: This skill will get events even when it is on cooldown!
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
			if (isEnhanced()) {
				if (source != null) {
					deflectParticles(source);

					DamageUtils.damage(mPlayer, source, DamageType.PROJECTILE_SKILL, event.getOriginalDamage() * ENHANCEMENT_DAMAGE, mInfo.getLinkedSpell(), true);
					// mPlayer.sendMessage(source.getName() + " HP: " + source.getHealth() + " / " + source.getMaxHealth() + " (-" + event.getOriginalDamage() + ")");

					// If applicable (either arrow or trident), apply knockback
					if (damager instanceof AbstractArrow abstractArrow) {
						int knockback = abstractArrow.getKnockbackStrength();
						Location playerLocation = mPlayer.getEyeLocation();
						Location sourceLocation = source.getEyeLocation();

						Vector direction = playerLocation.toVector().subtract(sourceLocation.toVector()).normalize();

						source.setVelocity(direction.multiply(-knockback));
					}
				}
			}

			event.setDamage(0);
			event.setCancelled(true);
		}
	}

	@Override
	public boolean playerHitByProjectileEvent(ProjectileHitEvent event) {
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

		// Dodging Active.
		if (proj instanceof Arrow arrow) {
			if (isEnhanced()) {
				if (proj.getShooter() instanceof LivingEntity livingEntity && EntityUtils.isHostileMob(livingEntity)) {
					// If Source is an enemy, apply potion effects to the enemy
					PotionUtils.PotionInfo potionInfo = PotionUtils.getPotionInfo(arrow.getBasePotionData(), 8);

					if (potionInfo != null) {
						PotionUtils.apply(livingEntity, potionInfo);
					}

					if (arrow.hasCustomEffects()) {
						for (PotionEffect potionEffect : arrow.getCustomEffects()) {
							PotionUtils.applyPotion(mPlayer, livingEntity, potionEffect);
							// mPlayer.sendMessage("Applied " + potionEffect);
						}
					}
				}
			}

			arrow.setBasePotionData(new PotionData(PotionType.MUNDANE));
			arrow.clearCustomEffects();
		}
		proj.remove();
		return true;
	}

	@Override
	public boolean playerSplashedByPotionEvent(Collection<LivingEntity> affectedEntities,
											   ThrownPotion potion, PotionSplashEvent event) {
		// If potion is thrown by a player or a non-living entity (trap), let it continue.
		if (!(event.getEntity().getShooter() instanceof LivingEntity) || event.getEntity().getShooter() instanceof Player) {
			return true;
		}

		boolean dodgeCheck = dodge();

		// If Dodging is enhanced: Return the potion effects to sender (probably the witch).
		// If code is here we already know it is a living entity.
		if (isEnhanced() && dodgeCheck && EntityUtils.isHostileMob((LivingEntity) event.getEntity().getShooter())) {
			LivingEntity enemy = (LivingEntity) event.getEntity().getShooter();

			// (I am pretty sure the only potion throwers are in fact, witches)
			// (But for a more general case, I will default use the enemy's attack damage stat, like how witches do.)
			double damage = EntityUtils.getAttributeOrDefault(enemy, Attribute.GENERIC_ATTACK_DAMAGE, 1);
			DamageUtils.damage(mPlayer, enemy, DamageType.MAGIC, damage * ENHANCEMENT_DAMAGE, mInfo.getLinkedSpell(), true);

			for (PotionEffect potionEffect : potion.getEffects()) {
				PotionUtils.applyPotion(mPlayer, enemy, potionEffect);
			}

			deflectParticles(enemy);
		}

		return !dodgeCheck;
	}

	private boolean dodge() {
		if (mTriggerTick == Bukkit.getServer().getCurrentTick()) {
			// Dodging was activated this tick - allow it
			return true;
		}

		/*
		 * Must check with cooldown timers directly because isAbilityOnCooldown always returns
		 * false (because ignoreCooldown is true)
		 */
		if (isOnCooldown()) {
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
		mTriggerTick = Bukkit.getServer().getCurrentTick();
		putOnCooldown();

		Location loc = mPlayer.getLocation().add(0, 1, 0);
		World world = mPlayer.getWorld();
		if (isLevelTwo()) {
			mPlugin.mEffectManager.addEffect(mPlayer, ATTR_NAME,
				new PercentSpeed(DODGING_SPEED_EFFECT_DURATION, PERCENT_SPEED + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SPEED), ATTR_NAME));
			new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 20, 0.25, 0.45, 0.25, 0.15).spawnAsPlayerActive(mPlayer);
			world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1, 1.35f);
		}
		new PartialParticle(Particle.SMOKE_NORMAL, loc, 90, 0.25, 0.45, 0.25, 0.1).spawnAsPlayerActive(mPlayer);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1, 2f);
		return true;
	}

	/**
	 * Creates straight-line particles between player and source.
	 *
	 * @param source Hostile enemy that shot the projectile / potion
	 */
	private void deflectParticles(LivingEntity source) {
		World world = mPlayer.getWorld();

		Location playerLocation = mPlayer.getEyeLocation();
		Location sourceLocation = source.getEyeLocation();

		Location particleLocation = sourceLocation.clone();

		Vector direction = playerLocation.toVector().subtract(sourceLocation.toVector()).normalize();
		for (int i = 0; i <= playerLocation.distance(sourceLocation); i++) {
			particleLocation.add(direction);

			world.spawnParticle(Particle.VILLAGER_HAPPY, particleLocation, 3, 0.25, 0.25, 0.25, 0);
			world.spawnParticle(Particle.CLOUD, particleLocation, 6, 0.05, 0.05, 0.05, 0.05);
		}
	}

}
