package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager.PlayerItemStats;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.projectiles.ProjectileSource;

import java.util.logging.Level;

public class DamageListener implements Listener {

	public static final String PROJECTILE_ITEM_STATS_METAKEY = "ProjectileItemStats";

	private final Plugin mPlugin;

	public DamageListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void entityDamageEvent(EntityDamageEvent event) {
		/*
		 * Puts the wrapper DamageEvent on EntityDamageEvents not caused by the
		 * plugin (DamageCause.CUSTOM), which should wrap events manually to
		 * set the correct DamageType.
		 */
		if (event.getCause() != DamageCause.CUSTOM) {
			if (event.getEntity() instanceof LivingEntity le) {
				Bukkit.getPluginManager().callEvent(new DamageEvent(event, le));
			}
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
			PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStats(player);
			if (playerItemStats != null) {
				projectile.setMetadata(PROJECTILE_ITEM_STATS_METAKEY, new FixedMetadataValue(mPlugin, new PlayerItemStats(playerItemStats)));
			} else {
				mPlugin.getLogger().log(Level.WARNING, "Null PlayerItemStats attempted to be added to a projectile. Player: " + player.getName());
			}
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
			mPlugin.mItemStatManager.onHurt(mPlugin, player, event);
			mPlugin.mAbilityManager.onHurt(player, event);

			if (damager != null) {
				mPlugin.mItemStatManager.onHurtByEntity(mPlugin, player, event, damager);
				mPlugin.mAbilityManager.onHurtByEntity(player, event, damager);

				if (source != null) {
					mPlugin.mItemStatManager.onHurtByEntityWithSource(mPlugin, player, event, damager, source);
					mPlugin.mAbilityManager.onHurtByEntityWithSource(player, event, damager, source);
				}
			}

			if (event.getDamage() > player.getHealth()) {
				mPlugin.mItemStatManager.onHurtFatal(mPlugin, player, event);
			}
		} else {
			if (source instanceof Player player) {
				// Check if projectile
				if (damager instanceof Projectile proj) {
					if (proj.hasMetadata(PROJECTILE_ITEM_STATS_METAKEY)) {
						Object value = proj.getMetadata(PROJECTILE_ITEM_STATS_METAKEY).get(0).value();
						if (value instanceof PlayerItemStats) {
							mPlugin.mItemStatManager.onDamage(mPlugin, player, (PlayerItemStats) value, event, damagee);
							mPlugin.mAbilityManager.onDamage(player, event, damagee);
						} else {
							mPlugin.getLogger().log(Level.WARNING, "Malformed ProjectileItemStats metadata detected");
						}
					}
				} else {
					if (event.isDelayed()) {
						Object value = event.getPlayerItemStat().value();
						if (value instanceof PlayerItemStats) {
							mPlugin.mItemStatManager.onDamage(mPlugin, player, (PlayerItemStats) value, event, damagee);
							mPlugin.mAbilityManager.onDamage(player, event, damagee);
						} else {
							mPlugin.getLogger().log(Level.WARNING, "Malformed PlayerItemStats metadata detected");
						}
					} else {
						mPlugin.mItemStatManager.onDamage(mPlugin, player, event, damagee);
						mPlugin.mAbilityManager.onDamage(player, event, damagee);
					}
				}
			}
		}
		mPlugin.mEffectManager.damageEvent(event);
	}

}
