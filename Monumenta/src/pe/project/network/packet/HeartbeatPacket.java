package pe.project.network.packet;

import pe.project.Plugin;

public class HeartbeatPacket implements Packet {
	public static final String StaticPacketChannel = "Monumenta.Bungee.Heartbeat";

	@Override
	public String getPacketChannel() {
		return StaticPacketChannel;
	}

	@Override
	public String getPacketData() throws Exception {
		return "Hello";
	}

	public static void handlePacket(Plugin plugin, String data) throws Exception {
		throw new Exception("Got message from which should only be received by bungeecord");
	}
}
