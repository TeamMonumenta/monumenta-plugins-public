package com.playmonumenta.plugins.bosses.spells;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.events.SpellCastEvent;

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
		Iterator<BukkitRunnable> iterator = mActiveRunnables.iterator();
		while (iterator.hasNext()) {
			BukkitRunnable runnable = iterator.next();
			if (!runnable.isCancelled()) {
				runnable.cancel();
			}
			iterator.remove();
		}
	}

	/* How long this spell takes to cast (in ticks) */
	public abstract int duration();

	/* How long the cast time is. The duration() method is more of a "cooldown before the next ability" */
	public int castTime() {
		return 0;
	}

	/*
	 * Boss damaged another entity
	 */
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {};

	/*
	 * Boss was damaged
	 */
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {};

	/*
	 * Boss shot a projectile
	 */
	public void bossLaunchedProjectile(ProjectileLaunchEvent event) {};

	/*
	 * Boss-shot projectile hit something
	 */
	public void bossProjectileHit(ProjectileHitEvent event) {};

	/*
	 * Boss gets hit by a projectile
	 */
	public void bossHitByProjectile(ProjectileHitEvent event) {};

	/*
	 * Boss hit by area effect cloud
	 */
	public void areaEffectAppliedToBoss(AreaEffectCloudApplyEvent event) {};

	/*
	 * Boss-shot projectile hit something
	 */
	public void splashPotionAppliedToBoss(PotionSplashEvent event) {};

	public void bossCastAbility(SpellCastEvent event) {};

}
