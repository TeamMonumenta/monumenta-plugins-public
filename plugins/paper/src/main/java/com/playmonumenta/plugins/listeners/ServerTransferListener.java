
package com.playmonumenta.plugins.listeners;

import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.redissync.event.PlayerServerTransferEvent;

public class ServerTransferListener implements Listener {
	private final Logger mLogger;

	public ServerTransferListener(Logger logger) {
		mLogger = logger;
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void playerServerTransferEvent(PlayerServerTransferEvent event) {
		Player player = event.getPlayer();
		mLogger.info("PlayerTransferEvent: Player: " + player + "   Target: " + event.getTarget());

		int dropped = InventoryUtils.removeSpecialItems(player, false);
		if (dropped == 1) {
			player.sendMessage(ChatColor.RED + "The dungeon key you were carrying was dropped!");
		} else if (dropped > 1) {
			player.sendMessage(ChatColor.RED + "The dungeon keys you were carrying were dropped!");
		}

		/* Remove potion effects that will be reapplied on the destination shard */
		Plugin plugin = Plugin.getInstance();
		plugin.mPotionManager.clearPotionIDType(player, PotionID.ABILITY_SELF);
		plugin.mPotionManager.clearPotionIDType(player, PotionID.ABILITY_OTHER);
		plugin.mPotionManager.clearPotionIDType(player, PotionID.SAFE_ZONE);
		plugin.mPotionManager.clearPotionIDType(player, PotionID.ITEM);
	}
}
