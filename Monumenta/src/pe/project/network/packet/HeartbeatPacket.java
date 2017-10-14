package pe.project.network.packet;

import pe.project.Main;

public class HeartbeatPacket implements Packet {

	// TODO - Ugh, this is so annoying
	// Want to just be able to call HeartbeatPacket.getPacketChannel() without an object
	// But making that static just causes other problems
	public static String getStaticPacketChannel() {
		return "Monumenta.Bungee.Heartbeat";
	}

	@Override
	public String getPacketChannel() {
		return "Monumenta.Bungee.Heartbeat";
	}

	@Override
	public String getPacketData() throws Exception {
		return "Hello";
	}

	public static void handlePacket(Main main, String data) throws Exception {
		throw new Exception("Got message from which should only be received by bungeecord");
	}
}
