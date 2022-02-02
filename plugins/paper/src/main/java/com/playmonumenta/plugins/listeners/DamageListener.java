package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager.PlayerItemStats;
import com.playmonumenta.plugins.utils.AbsorptionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.WeakHashMap;

public class DamageListener implements Listener {

	private final Plugin mPlugin;

	private static WeakHashMap<Projectile, PlayerItemStats> mPlayerItemStatsMap = new WeakHashMap<>();

	public DamageListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void entityDamageEvent(EntityDamageEvent event) {

		if (event instanceof EntityDamageByEntityEvent entityDamageByEntityEvent
				&& event.getCause().equals(DamageCause.ENTITY_EXPLOSION)
				&& event.getEntity() instanceof LivingEntity le) {
			Entity damager = entityDamageByEntityEvent.getDamager();
			if (damager instanceof Creeper creeper) {
				event.setDamage(EntityUtils.calculateCreeperExplosionDamage(creeper, le, event.getDamage()));
			} else if (damager instanceof LargeFireball largeFireball && largeFireball.getShooter() instanceof Ghast ghast) {
				event.setDamage(EntityUtils.calculateGhastExplosionDamage(ghast, largeFireball, le, event.getDamage()));
			}

		}

		/*
		 * Puts the wrapper DamageEvent on EntityDamageEvents not caused by the
		 * plugin (DamageCause.CUSTOM), which should wrap events manually to
		 * set the correct DamageType.
		 * If the damage is <= 0 (blocked with a shield), don't do anything to make
		 * sure the shield gets proper durability damage (and this also prevents knockback going through shields sometimes).
		 */
		if (event.getCause() != DamageCause.CUSTOM
			    && event.getFinalDamage() > 0
			    && event.getEntity() instanceof LivingEntity le) {
			Bukkit.getPluginManager().callEvent(new DamageEvent(event, le));
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void projectileLaunchEvent(ProjectileLaunchEvent event) {
		if (event.isCancelled()) {
			return;
		}

		Projectile projectile = event.getEntity();
		ProjectileSource source = projectile.getShooter();
		if (source instanceof Player player) {
			addProjectileItemStats(projectile, player);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void damageEvent(DamageEvent event) {
		if (event.isCancelled()) {
			return;
		}

		LivingEntity damagee = event.getDamagee();
		Entity damager = event.getDamager();
		LivingEntity source = event.getSource();

		// Player getting damaged
		if (damagee instanceof Player player) {
			mPlugin.mItemStatManager.onHurt(mPlugin, player, event, damager, source);
			mPlugin.mAbilityManager.onHurt(player, event, damager, source);

			if (event.getDamage() >= player.getHealth() + AbsorptionUtils.getAbsorption(player)
				    && !event.isCancelled()) {
				mPlugin.mAbilityManager.onHurtFatal(player, event);
				mPlugin.mItemStatManager.onHurtFatal(mPlugin, player, event);
			}
		} else {
			if (source instanceof Player player) {
				// Check if projectile
				if (damager instanceof Projectile proj) {
					PlayerItemStats playerItemStats = mPlayerItemStatsMap.get(proj);
					if (playerItemStats != null) {
						mPlugin.mItemStatManager.onDamage(mPlugin, player, playerItemStats, event, damagee);
						mPlugin.mAbilityManager.onDamage(player, event, damagee);
					}
				} else {
					if (event.isDelayed()) {
						mPlugin.mItemStatManager.onDamage(mPlugin, player, event.getPlayerItemStats(), event, damagee);
						mPlugin.mAbilityManager.onDamage(player, event, damagee);
					} else {
						mPlugin.mItemStatManager.onDamage(mPlugin, player, event, damagee);
						mPlugin.mAbilityManager.onDamage(player, event, damagee);
					}
				}
			}
		}
		mPlugin.mEffectManager.damageEvent(event);
	}

	public static @Nullable PlayerItemStats getProjectileItemStats(Projectile proj) {
		return mPlayerItemStatsMap.get(proj);
	}

	public static void addProjectileItemStats(Projectile proj, Player player) {
		mPlayerItemStatsMap.put(proj, Plugin.getInstance().mItemStatManager.getPlayerItemStatsCopy(player));
	}

	public static void addProjectileItemStats(Projectile proj, PlayerItemStats playerItemStats) {
		mPlayerItemStatsMap.put(proj, playerItemStats);
	}

	public static PlayerItemStats removeProjectileItemStats(Projectile proj) {
		return mPlayerItemStatsMap.remove(proj);
	}
}
