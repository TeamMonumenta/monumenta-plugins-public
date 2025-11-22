package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.itemstats.enchantments.Grappling;
import java.util.HashMap;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class GrapplingListener implements Listener {
	private final HashMap<Projectile, Double> mLevelMap = new HashMap<>();

	// We need to handle cases where the user fires an arrow and switches mainhand before the arrow lands
	@EventHandler(ignoreCancelled = false)
	public void onProjectileHit(ProjectileHitEvent event) {
		if (!mLevelMap.containsKey(event.getEntity())
			|| !(event.getEntity().getShooter() instanceof Player)) {
			return;
		}
		Projectile proj = event.getEntity();
		Player player = (Player) proj.getShooter();
		double level = mLevelMap.remove(proj);
		Grappling.handleProjectileHit(player, level, event, proj);
	}

	public void registerArrow(Projectile proj, double level) {
		mLevelMap.put(proj, level);
	}

	@EventHandler(ignoreCancelled = false)
	public void onPlayerQuit(PlayerQuitEvent event) {
		Grappling.untrackPlayer(event.getPlayer());
	}

	@EventHandler(ignoreCancelled = false)
	public void onPlayerDeath(PlayerDeathEvent event) {
		Grappling.untrackPlayer(event.getPlayer());
	}
}
