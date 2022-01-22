package com.playmonumenta.plugins.protocollib;

import com.comphenix.protocol.AsynchronousManager;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;

import java.util.logging.Logger;

public class ProtocolLibIntegration {

	public ProtocolLibIntegration(Plugin plugin) {
		Logger logger = plugin.getLogger();
		logger.info("Enabling ProtocolLib integration");

		ProtocolManager asyncManager = ProtocolLibrary.getProtocolManager();
		AsynchronousManager syncManager = asyncManager.getAsynchronousManager();
		long mainThreadId = Thread.currentThread().getId();

		// Must register in synchronous fashion for anything that calls Paper methods; these should check that the threads match before doing anything or bad things will happen
		syncManager.registerAsyncHandler(new PlayerItemStatsGUIOpener(plugin, mainThreadId)).syncStart();

		// Register in asynchronous fashion for things that only use ProtocolLib
		if (ServerProperties.getReplaceSpawnerEntities()) {
			logger.info("Enabling replacement of spawner entities");
			asyncManager.addPacketListener(new SpawnerEntityReplacer(plugin));
		} else {
			logger.info("Will not replace spawner entities on this shard");
		}
		asyncManager.addPacketListener(new GlowingReplacer(plugin));

	}

}
