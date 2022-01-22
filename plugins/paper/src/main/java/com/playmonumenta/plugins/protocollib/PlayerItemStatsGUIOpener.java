package com.playmonumenta.plugins.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.playmonumenta.plugins.itemstats.PlayerItemStatsGUI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

public class PlayerItemStatsGUIOpener extends PacketAdapter {

	private final Plugin mPlugin;
	private final long mMainThreadId;

	public PlayerItemStatsGUIOpener(Plugin plugin, long mainThreadId) {
		super(plugin, PacketType.Play.Client.RECIPE_SETTINGS);
		mPlugin = plugin;
		mMainThreadId = mainThreadId;
	}

	@Override
	public void onPacketReceiving(PacketEvent event) {
		if (mMainThreadId != Thread.currentThread().getId()) {
			mPlugin.getLogger().log(Level.WARNING, "Thread mismatch on synchronous PacketListener PlayerItemStatsGUIOpener");
		} else {
			Player player = event.getPlayer();
			new PlayerItemStatsGUI(player).openInventory(player, mPlugin);

			// TODO: this doesn't work, need to cancel the opening somehow; canceling event doesn't work, and neither does putting stuff in onPacketSending()
			event.getPacket().getBooleans().write(0, false).write(1, false);
		}
	}

}
