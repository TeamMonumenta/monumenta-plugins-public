package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager.PlayerItemStats;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
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

	private static final WeakHashMap<Projectile, PlayerItemStats> mPlayerItemStatsMap = new WeakHashMap<>();

	public DamageListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void entityDamageEvent(EntityDamageEvent event) {

		if (event instanceof EntityDamageByEntityEvent entityDamageByEntityEvent) {
			if (event.getCause().equals(DamageCause.ENTITY_EXPLOSION)
				    && event.getEntity() instanceof LivingEntity le) {
				Entity damager = entityDamageByEntityEvent.getDamager();
				if (damager instanceof Creeper creeper) {
					event.setDamage(EntityDamageEvent.DamageModifier.BASE, EntityUtils.calculateCreeperExplosionDamage(creeper, le, event.getDamage(EntityDamageEvent.DamageModifier.BASE)));
				}
			}

			if (event.getEntity() instanceof LivingEntity mob) {
				event.setDamage(event.getDamage() * EntityUtils.vulnerabilityMult(mob));
			}

			if (entityDamageByEntityEvent.getDamager() instanceof Player player
				    && event.getCause() == DamageCause.ENTITY_SWEEP_ATTACK) {
				PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStats(player);
				double sweepingEdgeLevel = ItemStatUtils.getEnchantmentLevel(player.getInventory().getItemInMainHand(), ItemStatUtils.EnchantmentType.SWEEPING_EDGE);
				if (playerItemStats != null && sweepingEdgeLevel > 0) {
					double damage = (1 + playerItemStats.getItemStats().get(ItemStatUtils.AttributeType.ATTACK_DAMAGE_ADD.getItemStat()))
						                * playerItemStats.getItemStats().get(ItemStatUtils.AttributeType.ATTACK_DAMAGE_MULTIPLY.getItemStat(), 1);
					event.setDamage(1 + damage * (sweepingEdgeLevel / (sweepingEdgeLevel + 1)));
				}
			}
		}

		/*
		 * Puts the wrapper DamageEvent on EntityDamageEvents not caused by the
		 * plugin (DamageCause.CUSTOM), which should wrap events manually to
		 * set the correct DamageType.
		 */
		double originalDamage = event.getDamage();
		if (event.getEntity() instanceof LivingEntity le) {
			if (event.getCause() != DamageCause.CUSTOM) {
				Bukkit.getPluginManager().callEvent(new DamageEvent(event, le));
			} else if (DamageUtils.nextEventMetadata != null) {
				Bukkit.getPluginManager().callEvent(new DamageEvent(event, le, DamageUtils.nextEventMetadata));
			}
		}
		// If the damage is blocked, revert to the initial damage to make sure the shield gets proper durability damage.
		// This also prevents knockback going through shields sometimes for some reason.
		if (event.getOriginalDamage(EntityDamageEvent.DamageModifier.BLOCKING) < 0) {
			event.setDamage(originalDamage);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void projectileLaunchEvent(ProjectileLaunchEvent event) {
		Projectile projectile = event.getEntity();
		ProjectileSource source = projectile.getShooter();
		if (source instanceof Player player) {
			addProjectileItemStats(projectile, player);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void damageEvent(DamageEvent event) {
		LivingEntity damagee = event.getDamagee();
		Entity damager = event.getDamager();
		LivingEntity source = event.getSource();

		// Player getting damaged
		if (damagee instanceof Player player) {
			mPlugin.mItemStatManager.onHurt(mPlugin, player, event, damager, source);
			mPlugin.mAbilityManager.onHurt(player, event, damager, source);

			if (event.getFinalDamage(true) >= player.getHealth()
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
					PlayerItemStats eventPlayerItemStats = event.getPlayerItemStats();
					if (eventPlayerItemStats != null) {
						mPlugin.mItemStatManager.onDamage(mPlugin, player, eventPlayerItemStats, event, damagee);
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
