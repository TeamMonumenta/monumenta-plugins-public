package com.playmonumenta.plugins.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;

import java.util.logging.Logger;

public class ProtocolLibIntegration {

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

	}

}
