package com.playmonumenta.plugins.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import org.bukkit.Bukkit;

import java.util.logging.Logger;

public class ProtocolLibIntegration {

	private final PlayerTitlePacketAdapter mPlayerTitlePacketAdapter;

	public ProtocolLibIntegration(Plugin plugin) {
		Logger logger = plugin.getLogger();
		logger.info("Enabling ProtocolLib integration");

		ProtocolManager syncManager = ProtocolLibrary.getProtocolManager();

		syncManager.addPacketListener(new PlayerItemStatsGUIOpener(plugin));

		if (ServerProperties.getReplaceSpawnerEntities()) {
			logger.info("Enabling replacement of spawner entities");
			syncManager.addPacketListener(new SpawnerEntityReplacer(plugin));
		} else {
			logger.info("Will not replace spawner entities on this shard");
		}
		syncManager.addPacketListener(new GlowingReplacer(plugin));
		syncManager.addPacketListener(new VirtualFirmamentReplacer(plugin));
		syncManager.addPacketListener(new FirmamentLagFix(plugin));
		syncManager.addPacketListener(new EntityEquipmentReplacer(plugin));

		mPlayerTitlePacketAdapter = new PlayerTitlePacketAdapter(syncManager, plugin);
		syncManager.addPacketListener(mPlayerTitlePacketAdapter);

		Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 2, 2);
	}

	// called every 2 ticks
	private void tick() {
		mPlayerTitlePacketAdapter.tick();
	}

}
