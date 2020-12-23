package com.playmonumenta.plugins.integrations;

import java.util.logging.Logger;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.listeners.DelvesListener;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import com.playmonumenta.redissync.event.PlayerServerTransferEvent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class MonumentaRedisSyncIntegration implements Listener {
	private static final String IDENTIFIER = "Monumenta";

	private final Plugin mPlugin;
	private final Logger mLogger;

	public MonumentaRedisSyncIntegration(Plugin plugin) {
		mPlugin = plugin;
		mLogger = plugin.getLogger();
		mLogger.info("Enabling MonumentaRedisSync integration");
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void playerServerTransferEvent(PlayerServerTransferEvent event) {
		Player player = event.getPlayer();
		mLogger.info("PlayerTransferEvent: Player: " + player + "   Target: " + event.getTarget());
		DelvesListener.onTransfer(player, event.getTarget());
		Plugin.getInstance().mEffectManager.clearEffects(player);

		player.closeInventory();

		int dropped = InventoryUtils.removeSpecialItems(player, false);
		if (dropped == 1) {
			player.sendMessage(ChatColor.RED + "The dungeon key you were carrying was dropped!");
		} else if (dropped > 1) {
			player.sendMessage(ChatColor.RED + "The dungeon keys you were carrying were dropped!");
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		JsonObject data = MonumentaRedisSyncAPI.getPlayerPluginData(player.getUniqueId(), IDENTIFIER);
		if (data != null) {
			try {
				mPlugin.mPotionManager.loadFromJsonObject(player, data.get("potions").getAsJsonObject());
				/* TODO LEVEL */
				mLogger.info("Loaded potion data for player " + player.getName());
			} catch (Exception ex) {
				mLogger.severe("Failed to load potion data for player " + player.getName() + ":" + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void playerSaveEvent(PlayerSaveEvent event) {
		JsonObject pluginData = new JsonObject();
		pluginData.add("potions", mPlugin.mPotionManager.getAsJsonObject(event.getPlayer(), false));
		event.setPluginData(IDENTIFIER, pluginData);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void playerQuitEvent(PlayerQuitEvent event) {
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			if (!event.getPlayer().isOnline()) {
				mPlugin.mPotionManager.clearAllPotions(event.getPlayer());
			}
		}, 5);
	}
}
