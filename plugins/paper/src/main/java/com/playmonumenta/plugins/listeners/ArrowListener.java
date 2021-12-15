package com.playmonumenta.plugins.listeners;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class ArrowListener implements Listener {

	//If the projectile hits an entity, put it in here to see if it has hit the entity before
	private final Map<AbstractArrow, Entity> mHitBefore = new HashMap<>();

	public ArrowListener(Plugin plugin) {

		//Clear the mHitBefore map every 5 minutes
		new BukkitRunnable() {
			@Override
			public void run() {
				mHitBefore.clear();
			}
		}.runTaskTimer(plugin, 20 * 60 * 5, 20 * 60 * 5);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void projectileHitEvent(ProjectileHitEvent event) {
		//Has to be an arrow
		Entity hitEntity = event.getHitEntity();
		if (hitEntity == null
				|| !(event.getEntity() instanceof AbstractArrow)
				|| event.getEntity() instanceof Trident
				|| !(event.getEntity().getShooter() instanceof LivingEntity)
				|| !(event.getEntity().getShooter() instanceof Player)) {
			return;
		}

		AbstractArrow arrow = (AbstractArrow) event.getEntity();
		Entity hitBeforeEntity = mHitBefore.get(arrow);

		if (hitBeforeEntity == null) {
			mHitBefore.put(arrow, hitEntity);
		} else if (hitEntity.equals(hitBeforeEntity)) {
			mHitBefore.remove(arrow);
			arrow.remove();
		}
	}
}
