package com.playmonumenta.plugins.packets;

public interface BasePacket {
	String getPacketChannel();
	String getPacketData() throws Exception;

	// Implementers should also have:
	// static final String StaticPacketChannel
	// static void handlePacket(Plugin plugin, String data) throws Exception;
}
