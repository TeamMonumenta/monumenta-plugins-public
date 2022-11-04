package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.events.SpellCastEvent;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.scheduler.BukkitRunnable;

public abstract class Spell implements Cloneable {
	protected final Set<BukkitRunnable> mActiveRunnables = new LinkedHashSet<BukkitRunnable>();

	/*
	 * Used by some spells to indicate if they can be run
	 * now (true) or not (false)
	 */
	public boolean canRun() {
		return true;
	}

	public abstract void run();

	/*
	 * Cancels all currently running tasks (tasks in mActiveRunnables)
	 *
	 * To use this functionality, user needs to add every BukkitRunnable created to mActiveRunnables,
	 * and then remove them from mActiveRunnables when they are finished
	 */
	public void cancel() {
		/*
		 * Iterate over a copy of mActiveRunnables and cancel each task that isn't already cancelled.
		 * Need to iterate over a copy because some runnables remove themselves from mActiveRunnables when cancelled
		 */
		new ArrayList<>(mActiveRunnables).forEach((runnable) -> {
			if (!runnable.isCancelled()) {
				runnable.cancel();
			}
		});
		mActiveRunnables.clear();
	}

	/**
	 * How long boss must wait/cooldown in ticks between start (not end!) of this spell and start of next spell
	 */
	public abstract int cooldownTicks();

	/**
	 * How long boss takes in ticks to complete casting process of this spell
	 */
	public int castTicks() {
		return 0;
	}

	public boolean onlyForceCasted() {
		return false;
	}

	/**
	 * Whether this spell is currently active.
	 *
	 * By default, this checks if there are active runnables, but may be overridden by spells to be more specific.
	 */
	public boolean isRunning() {
		return mActiveRunnables.stream().anyMatch(r -> !r.isCancelled());
	}

	public void onDamage(DamageEvent event, LivingEntity damagee) {

	}

	/*
	 * Boss was hurt, with or without an entity
	 */
	public void onHurt(DamageEvent event) {

	}

	/*
	 * Boss was hurt by an entity
	 */
	public void onHurtByEntity(DamageEvent event, Entity damager) {

	}

	/*
	 * Boss was hurt by an entity with a source (source is usually the same as the entity)
	 */
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {

	}

	public void onDeath(EntityDeathEvent event) {

	}

	/**
	 * Whether this ability ignores being silenced.
	 *
	 * Currently only works for passive spells.
	 */
	public boolean bypassSilence() {
		return false;
	}

	/*
	 * Boss shot a projectile
	 */
	public void bossLaunchedProjectile(ProjectileLaunchEvent event) {

	}

	/*
	 * Boss-shot projectile hit something
	 */
	public void bossProjectileHit(ProjectileHitEvent event) {

	}

	/*
	 * Boss gets hit by a projectile
	 */
	public void bossHitByProjectile(ProjectileHitEvent event) {

	}

	/*
	 * Boss hit by area effect cloud
	 */
	public void areaEffectAppliedToBoss(AreaEffectCloudApplyEvent event) {

	}

	/*
	 * Boss-shot projectile hit something
	 */
	public void splashPotionAppliedToBoss(PotionSplashEvent event) {

	}

	/*
	 * Boss throws a splash potion
	 */
	public void bossSplashPotion(PotionSplashEvent event) {

	}

	public void bossCastAbility(SpellCastEvent event) {

	}

	public void nearbyPlayerDeath(PlayerDeathEvent event) {

	}

	public boolean hasNearbyPlayerDeathTrigger() {
		return false;
	}

	@FunctionalInterface
	public interface GetSpellTargets<V extends LivingEntity> {
		List<? extends V> getTargets();
	}

}
