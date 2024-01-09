package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;
import com.playmonumenta.scriptedquests.zones.ZonePropertyChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class ZoneListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void zonePropertyChangeEvent(ZonePropertyChangeEvent event) {
		Player player = event.getPlayer();
		String namespace = event.getNamespaceName();
		String property = event.getProperty();

		GameMode mode = player.getGameMode();

		if (namespace.equals("default")) {
			switch (property) {
			case "Adventure Mode":
				if (mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR) {
					player.setGameMode(GameMode.ADVENTURE);
				}
				break;
			case "!Adventure Mode":
				if (mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR) {
					player.setGameMode(GameMode.SURVIVAL);
				}
				break;
			default:
				// Do nothing
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void projectileLaunchEvent(ProjectileLaunchEvent event) {
		if (event.getEntity().getShooter() instanceof Player player) {
			if (event.getEntityType() == EntityType.SNOWBALL && event.getEntity() instanceof Snowball snowball) {
				// Only allow winter arena snowballs in the winter arena
				if (ZoneUtils.hasZoneProperty(player, ZoneProperty.WINTER_SNOWBALLS_ONLY)
					    && !InventoryUtils.testForItemWithName(snowball.getItem(), "Arena Snowball", true)) {
					event.setCancelled(true);
					return;
				}
			} else if (event.getEntityType() == EntityType.SPLASH_POTION) {
				if (ZoneUtils.hasZoneProperty(player, ZoneProperty.NO_POTIONS)) {
					event.setCancelled(true);
					return;
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerItemConsumeEvent(PlayerItemConsumeEvent event) {
		if ((ItemUtils.isSomePotion(event.getItem()) || ItemStatUtils.isConsumable(event.getItem())) && ZoneUtils.hasZoneProperty(event.getPlayer(), ZoneProperty.NO_POTIONS)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockFadeEvent(BlockFadeEvent event) {
		if (ZoneUtils.hasZoneProperty(event.getBlock().getLocation(), ZoneUtils.ZoneProperty.ADVENTURE_MODE)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockFormEvent(BlockFormEvent event) {
		if (ZoneUtils.hasZoneProperty(event.getBlock().getLocation(), ZoneUtils.ZoneProperty.ADVENTURE_MODE)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockFromToEvent(BlockFromToEvent event) {
		if (event.getBlock().getType() == Material.DRAGON_EGG
			&& ZoneUtils.hasZoneProperty(event.getToBlock().getLocation(), ZoneUtils.ZoneProperty.ADVENTURE_MODE)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockGrowEvent(BlockGrowEvent event) {
		if (ZoneUtils.hasZoneProperty(event.getBlock().getLocation(), ZoneUtils.ZoneProperty.ADVENTURE_MODE)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockSpreadEvent(BlockSpreadEvent event) {
		if (ZoneUtils.hasZoneProperty(event.getBlock().getLocation(), ZoneUtils.ZoneProperty.ADVENTURE_MODE)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void cauldronLevelChangeEvent(CauldronLevelChangeEvent event) {
		if (event.getReason().equals(CauldronLevelChangeEvent.ChangeReason.EXTINGUISH) &&
			ZoneUtils.hasZoneProperty(event.getBlock().getLocation(), ZoneUtils.ZoneProperty.ADVENTURE_MODE)) {
			final Block blockToRestore = event.getBlock();
			final BlockState restoredState = event.getBlock().getState();
			Bukkit.getScheduler().runTask(Plugin.getInstance(),
				() -> blockToRestore.setBlockData(restoredState.getBlockData()));
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityBlockFormEvent(EntityBlockFormEvent event) {
		if (ZoneUtils.hasZoneProperty(event.getBlock().getLocation(), ZoneUtils.ZoneProperty.ADVENTURE_MODE)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityDamageEvent(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player player &&
			    ZoneUtils.hasZoneProperty(player, ZoneProperty.NO_FALL_DAMAGE) &&
			    event.getCause() == EntityDamageEvent.DamageCause.FALL) {
			event.setCancelled(true);
		}
	}

}
