package com.playmonumenta.plugins.network.packet;

public interface Packet {
	String getPacketChannel();
	String getPacketData() throws Exception;

	// Implementers should also have:
	// static final String StaticPacketChannel
	// static void handlePacket(Plugin plugin, String data) throws Exception;
}
