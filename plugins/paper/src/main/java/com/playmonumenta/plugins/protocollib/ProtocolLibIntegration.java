package com.playmonumenta.plugins.protocollib;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

public class ProtocolLibIntegration {

	private final PlayerTitlePacketAdapter mPlayerTitlePacketAdapter;

	private @Nullable PacketMonitor mPacketMonitor;

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
		syncManager.addPacketListener(new WorldNameReplacer(plugin));

		mPlayerTitlePacketAdapter = new PlayerTitlePacketAdapter(syncManager, plugin);
		syncManager.addPacketListener(mPlayerTitlePacketAdapter);

		Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, 2, 2);

		if (Bukkit.getPluginManager().isPluginEnabled("PrometheusExporter")) {
			mPacketMonitor = new PacketMonitor(plugin);
			syncManager.addPacketListener(mPacketMonitor);
		}
	}

	// called every 2 ticks
	private void tick() {
		mPlayerTitlePacketAdapter.tick();
	}

	public void enablePacketMonitor(CommandSender sender, boolean enable, boolean fullReporting) {
		if (mPacketMonitor == null) {
			sender.sendMessage(Component.text("PrometheusExporter not enabled!", NamedTextColor.RED));
			return;
		}
		ProtocolLibrary.getProtocolManager().removePacketListener(mPacketMonitor);
		mPacketMonitor.setFullReporting(fullReporting);
		if (enable) {
			ProtocolLibrary.getProtocolManager().addPacketListener(mPacketMonitor);
		}
		sender.sendMessage("Packet monitoring is now " + (enable ? "enabled with " + (fullReporting ? "full" : "simple") + " measurements" : "disabled") + ".");
	}

}
