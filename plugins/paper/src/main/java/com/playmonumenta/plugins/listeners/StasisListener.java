package com.playmonumenta.plugins.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Stasis;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.PotionEffectApplyEvent;

public class StasisListener implements Listener {

	public static boolean isInStasis(@Nullable Entity entity) {
		// Only players can stasis, at least for now
		// No need to do a bunch of iteration to check on other entities
		if (entity == null || !(entity instanceof Player)) {
			return false;
		}
		return Plugin.getInstance().mEffectManager.hasEffect(entity, Stasis.STASIS_NAME);
	}

	@EventHandler (priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		if (isInStasis(player)) {
			Location to = event.getFrom();
			to.setPitch(event.getTo().getPitch());
			to.setYaw(event.getTo().getYaw());
			event.setTo(event.getFrom());
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void damageEvent(DamageEvent event) {
		if (isInStasis(event.getDamager()) || isInStasis(event.getDamagee())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityDamageEvent(EntityDamageEvent event) {
		if (isInStasis(event.getEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityCombustByEntityEvent(EntityCombustByEntityEvent event) {
		if (isInStasis(event.getEntity()) || isInStasis(event.getCombuster())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void projectileLaunchEvent(ProjectileLaunchEvent event) {
		ProjectileSource source = event.getEntity().getShooter();
		if (source instanceof Player) {
			if (isInStasis((Player) source)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void projectileHitEvent(ProjectileHitEvent event) {
		ProjectileSource source = event.getEntity().getShooter();
		if (source instanceof Player) {
			if (isInStasis((Player) source)) {
				event.setCancelled(true);
			}
		}

		if (isInStasis(event.getHitEntity())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerItemConsumeEvent(PlayerItemConsumeEvent event) {
		if (isInStasis(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	public void potionEffectApplyEvent(PotionEffectApplyEvent event) {
		if (isInStasis(event.getApplied()) || isInStasis(event.getApplier())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockPlaceEvent(BlockPlaceEvent event) {
		if (isInStasis(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		if (isInStasis(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerInteractEntityEvent(PlayerInteractEntityEvent event) {
		if (isInStasis(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerItemDamageEvent(PlayerItemDamageEvent event) {
		if (isInStasis(event.getPlayer())) {
			event.setCancelled(true);
		}
	}
}
