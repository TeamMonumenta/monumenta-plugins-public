package com.playmonumenta.plugins.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.GamePhase;
import com.comphenix.protocol.injector.netty.WirePacket;
import com.playmonumenta.plugins.Plugin;
import io.prometheus.client.Counter;

/**
 * Measures packet metrics using Prometheus
 */
public class PacketMonitor implements PacketListener {

	private static final Counter outgoingCounter = Counter.build()
		.name("monumenta_outgoing_packets_by_type")
		.help("Outgoing packets by packet type")
		.labelNames("packet_type")
		.register();

	private static final Counter outgoingBytesCounter = Counter.build()
		.name("monumenta_outgoing_packet_bytes_by_type")
		.help("Outgoing packet sizes by packet type")
		.labelNames("packet_type")
		.register();

	private final Plugin mPlugin;

	private boolean mFullReporting = false;

	public PacketMonitor(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		outgoingCounter.labels(event.getPacket().getType().name()).inc();
		if (mFullReporting) {
			byte[] contents = WirePacket.bytesFromPacket(event.getPacket());
			outgoingBytesCounter.labels(event.getPacket().getType().name()).inc(contents.length);
		}
	}

	@Override
	public void onPacketReceiving(PacketEvent event) {
	}

	@Override
	public ListeningWhitelist getSendingWhitelist() {
		return ListeningWhitelist.newBuilder()
			.gamePhase(GamePhase.PLAYING)
			.types(PacketType.Play.Server.getInstance().values().stream()
				.filter(type -> type.isSupported()
					                && !type.isDeprecated()
					                && !type.equals(PacketType.Play.Server.TAB_COMPLETE))
				.toList())
			.monitor()
			.build();
	}

	@Override
	public ListeningWhitelist getReceivingWhitelist() {
		return ListeningWhitelist.EMPTY_WHITELIST;
	}

	@Override
	public org.bukkit.plugin.Plugin getPlugin() {
		return mPlugin;
	}

	public void setFullReporting(boolean fullReporting) {
		mFullReporting = fullReporting;
	}
}
