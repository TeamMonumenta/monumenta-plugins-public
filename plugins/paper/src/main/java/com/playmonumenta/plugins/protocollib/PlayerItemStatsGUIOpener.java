package com.playmonumenta.plugins.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.playmonumenta.plugins.itemstats.PlayerItemStatsGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PlayerItemStatsGUIOpener extends PacketAdapter {

	private final Plugin mPlugin;

	public PlayerItemStatsGUIOpener(Plugin plugin) {
		super(plugin, PacketType.Play.Client.RECIPE_SETTINGS);
		mPlugin = plugin;
	}

	@Override
	public void onPacketReceiving(PacketEvent event) {
		Player player = event.getPlayer();
		Bukkit.getScheduler().runTask(mPlugin, () -> {
			new PlayerItemStatsGUI(player).openInventory(player, mPlugin);
		});
	}

}
