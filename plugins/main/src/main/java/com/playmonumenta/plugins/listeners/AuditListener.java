package com.playmonumenta.plugins.listeners;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.packets.AuditLogPacket;

public class AuditListener implements Listener {
	private final Plugin mPlugin;
	private final String mShardName;

	public AuditListener(Plugin plugin, String shardName) {
		mPlugin = plugin;
		mShardName = shardName;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void gamemode(PlayerGameModeChangeEvent event) {
		Player player = event.getPlayer();
		if (!player.isOp()) {
			return;
		}

		GameMode curMode = player.getGameMode();
		GameMode newMode = event.getNewGameMode();
		if ((curMode.equals(GameMode.SURVIVAL) && newMode.equals(GameMode.ADVENTURE))
		    || (curMode.equals(GameMode.ADVENTURE) && newMode.equals(GameMode.SURVIVAL))) {
			// Don't log normal game mode changes
			return;
		}

		log("GameMode: " + player.getName() + " " + curMode.toString() + " -> " + newMode.toString());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void death(PlayerDeathEvent event) {
		Player player = event.getEntity();
		if (!player.isOp()) {
			return;
		}

		log("Death: " + player.getName() + " " + event.getDeathMessage());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void command(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		if (!player.isOp()) {
			return;
		}

		String cmd = event.getMessage();
		if (cmd.startsWith("/questtrigger")
		    || cmd.startsWith("/msg")) {
			return;
		}

		log("Command: " + player.getName() + " " + cmd);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void creative(InventoryCreativeEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) {
			return;
		}

		Player player = (Player)event.getWhoClicked();
		if (!player.isOp()) {
			return;
		}

		if (event.getCursor() == null || event.getCursor().getType().equals(Material.AIR)) {
			/* Empty hand */

		} else {
			/* Full hand */

		}
		event.getClick().equals(ClickType.CREATIVE);
		log("CreativeInventory: " + player.getName() + " " + event.getAction().toString() + " " + event.getCursor().toString());
	}

	private void log(String message) {
		mPlugin.mSocketManager.sendPacket(new AuditLogPacket(mShardName + ": " + message));
	}
}
